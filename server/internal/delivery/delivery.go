// Package delivery fans an event out to every pushable device, records the
// outcome in the deliveries table, and logs push.sent / push.failed.
package delivery

import (
	"context"
	"database/sql"
	"errors"
	"log/slog"
	"sync"
	"time"

	"github.com/chrisgreg/boop/server/internal/apns"
	"github.com/chrisgreg/boop/server/internal/devices"
	"github.com/chrisgreg/boop/server/internal/events"
	"github.com/chrisgreg/boop/server/internal/events/levels"
	"github.com/chrisgreg/boop/server/internal/fcm"
	"github.com/chrisgreg/boop/server/internal/ids"
	"github.com/chrisgreg/boop/server/internal/projects"
)

// Sender is the subset of *apns.Client the dispatcher needs.
type Sender interface {
	Send(ctx context.Context, deviceToken string, n apns.Notification) (string, error)
}

// AndroidSender is the subset of *fcm.Client the dispatcher needs.
type AndroidSender interface {
	Send(ctx context.Context, deviceToken string, n fcm.Notification) (string, error)
}

// Statuses recorded in deliveries.status.
const (
	StatusSent    = "sent"
	StatusFailed  = "failed"
	StatusSkipped = "skipped"
)

// Delivery is one attempt to push an event to a device.
type Delivery struct {
	ID          string `json:"id"`
	EventID     string `json:"event_id"`
	DeviceID    string `json:"device_id"`
	DeviceName  string `json:"device_name"`
	Status      string `json:"status"`
	APNSID      string `json:"apns_id,omitempty"`
	Error       string `json:"error,omitempty"`
	AttemptedAt string `json:"attempted_at"`
}

// Dispatcher sends notifications for events.
type Dispatcher struct {
	db      *sql.DB
	devices *devices.Store
	sender  Sender        // APNs sender; nil when APNs is not configured
	android AndroidSender // FCM sender; nil when FCM is not configured
	log     *slog.Logger
	timeout time.Duration

	queue chan job
	wg    sync.WaitGroup
}

type job struct {
	event   events.Event
	project projects.Project
}

// New returns a Dispatcher. sender may be nil, in which case deliveries are
// recorded as skipped.
func New(db *sql.DB, d *devices.Store, sender Sender, log *slog.Logger) *Dispatcher {
	return &Dispatcher{db: db, devices: d, sender: sender, log: log, timeout: 10 * time.Second, queue: make(chan job, 1024)}
}

// SetAndroidSender wires the FCM sender used for devices whose platform is
// "android". It may be nil, in which case those deliveries are recorded as
// skipped. It is safe to call before Start.
func (d *Dispatcher) SetAndroidSender(s AndroidSender) { d.android = s }

// Start runs the background worker until ctx is cancelled.
func (d *Dispatcher) Start(ctx context.Context) {
	d.wg.Add(1)
	go func() {
		defer d.wg.Done()
		for {
			select {
			case <-ctx.Done():
				return
			case j := <-d.queue:
				d.Deliver(context.Background(), j.event, j.project)
			}
		}
	}()
}

// Wait blocks until the worker has stopped.
func (d *Dispatcher) Wait() { d.wg.Wait() }

// Enqueue schedules delivery without blocking the caller. If the queue is
// full the event is delivered synchronously instead.
func (d *Dispatcher) Enqueue(e events.Event, p projects.Project) {
	select {
	case d.queue <- job{event: e, project: p}:
	default:
		d.Deliver(context.Background(), e, p)
	}
}

// Deliver pushes e to every registered device synchronously and returns the
// recorded deliveries.
func (d *Dispatcher) Deliver(ctx context.Context, e events.Event, p projects.Project) []Delivery {
	if !p.Notify || !levels.AtLeast(e.Level, p.MinLevel) {
		return nil
	}
	devs, err := d.devices.Pushable(ctx)
	if err != nil {
		d.log.Error("push.failed", "event_id", e.ID, "error", err.Error())
		return nil
	}
	n := apns.Notification{
		Title:     p.Name + " · " + e.Title,
		Body:      e.Body,
		EventID:   e.ID,
		ProjectID: p.ID,
		Prominent: e.Level == levels.Critical,
	}
	for _, a := range e.Actions {
		n.Actions = append(n.Actions, apns.Action{Label: a.Label, URL: a.URL})
	}
	if n.Body == "" {
		n.Body = e.Title
		n.Title = p.Name
	}
	var out []Delivery
	for _, dev := range devs {
		rec := Delivery{ID: ids.New("dlv"), EventID: e.ID, DeviceID: dev.ID, DeviceName: dev.Name, AttemptedAt: ids.Now()}
		if dev.Platform == "android" {
			d.deliverAndroid(ctx, dev, n, &rec)
		} else {
			d.deliverAPNS(ctx, dev, n, &rec)
		}
		if _, err := d.db.ExecContext(ctx, `INSERT INTO deliveries (id, event_id, device_id, status, apns_id, error, attempted_at, created_at) VALUES (?,?,?,?,?,?,?,?)`,
			rec.ID, rec.EventID, rec.DeviceID, rec.Status, rec.APNSID, rec.Error, rec.AttemptedAt, rec.AttemptedAt); err != nil {
			d.log.Error("delivery.record_failed", "error", err.Error())
		}
		out = append(out, rec)
	}
	return out
}

// deliverAPNS sends n to an iOS device through APNs.
func (d *Dispatcher) deliverAPNS(ctx context.Context, dev devices.Device, n apns.Notification, rec *Delivery) {
	if d.sender == nil {
		rec.Status = StatusSkipped
		rec.Error = "APNs is not configured"
		return
	}
	sctx, cancel := context.WithTimeout(ctx, d.timeout)
	defer cancel()
	apnsID, err := d.sender.Send(sctx, *dev.DeviceToken, n)
	if err != nil {
		rec.Status = StatusFailed
		rec.Error = err.Error()
		var ae *apns.Error
		if errors.As(err, &ae) && ae.Unregistered() {
			_ = d.devices.ClearToken(ctx, dev.ID)
		}
		d.log.Warn("push.failed", "event_id", rec.EventID, "device_id", dev.ID, "error", rec.Error)
		return
	}
	rec.Status = StatusSent
	rec.APNSID = apnsID
	d.log.Info("push.sent", "event_id", rec.EventID, "device_id", dev.ID, "apns_id", apnsID)
}

// deliverAndroid sends n to an Android device through FCM.
func (d *Dispatcher) deliverAndroid(ctx context.Context, dev devices.Device, n apns.Notification, rec *Delivery) {
	if d.android == nil {
		rec.Status = StatusSkipped
		rec.Error = "FCM is not configured"
		return
	}
	an := fcm.Notification{Title: n.Title, Body: n.Body, EventID: n.EventID, ProjectID: n.ProjectID}
	for _, a := range n.Actions {
		an.Actions = append(an.Actions, fcm.Action{Label: a.Label, URL: a.URL})
	}
	sctx, cancel := context.WithTimeout(ctx, d.timeout)
	defer cancel()
	msgID, err := d.android.Send(sctx, *dev.DeviceToken, an)
	if err != nil {
		rec.Status = StatusFailed
		rec.Error = err.Error()
		if errors.Is(err, fcm.ErrUnregistered) {
			_ = d.devices.ClearToken(ctx, dev.ID)
		}
		d.log.Warn("push.failed", "event_id", rec.EventID, "device_id", dev.ID, "error", rec.Error)
		return
	}
	rec.Status = StatusSent
	rec.APNSID = msgID
	d.log.Info("push.sent", "event_id", rec.EventID, "device_id", dev.ID, "fcm_id", msgID)
}

// ForEvent lists deliveries recorded for an event.
func (d *Dispatcher) ForEvent(ctx context.Context, eventID string) ([]Delivery, error) {
	rows, err := d.db.QueryContext(ctx, `SELECT dl.id, dl.event_id, dl.device_id, COALESCE(dv.name, ''), dl.status, dl.apns_id, dl.error, dl.attempted_at
		FROM deliveries dl LEFT JOIN devices dv ON dv.id = dl.device_id WHERE dl.event_id = ? ORDER BY dl.attempted_at DESC`, eventID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Delivery{}
	for rows.Next() {
		var r Delivery
		if err := rows.Scan(&r.ID, &r.EventID, &r.DeviceID, &r.DeviceName, &r.Status, &r.APNSID, &r.Error, &r.AttemptedAt); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

// Last returns the most recent delivery attempt, if any.
func (d *Dispatcher) Last(ctx context.Context) (*Delivery, error) {
	var r Delivery
	err := d.db.QueryRowContext(ctx, `SELECT dl.id, dl.event_id, dl.device_id, COALESCE(dv.name, ''), dl.status, dl.apns_id, dl.error, dl.attempted_at
		FROM deliveries dl LEFT JOIN devices dv ON dv.id = dl.device_id ORDER BY dl.attempted_at DESC LIMIT 1`).
		Scan(&r.ID, &r.EventID, &r.DeviceID, &r.DeviceName, &r.Status, &r.APNSID, &r.Error, &r.AttemptedAt)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, nil
	}
	return &r, err
}
