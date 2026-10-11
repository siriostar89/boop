package fcm

import (
	"encoding/json"
	"errors"
	"testing"
)

func TestPayload(t *testing.T) {
	b, err := Payload("tok-123", Notification{
		Title:     "Shop · Payment received",
		Body:      "£19.99",
		EventID:   "evt_1",
		ProjectID: "prj_1",
		Actions:   []Action{{Label: "Open in Stripe", URL: "https://dashboard.stripe.com"}},
	})
	if err != nil {
		t.Fatalf("Payload: %v", err)
	}
	var got struct {
		Message struct {
			Token        string            `json:"token"`
			Notification map[string]string `json:"notification"`
			Data         map[string]string `json:"data"`
			Android      struct {
				Priority string `json:"priority"`
			} `json:"android"`
		} `json:"message"`
	}
	if err := json.Unmarshal(b, &got); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if got.Message.Token != "tok-123" {
		t.Errorf("token = %q, want tok-123", got.Message.Token)
	}
	if got.Message.Notification["title"] != "Shop · Payment received" {
		t.Errorf("title = %q", got.Message.Notification["title"])
	}
	if got.Message.Data["event_id"] != "evt_1" || got.Message.Data["project_id"] != "prj_1" {
		t.Errorf("data = %v", got.Message.Data)
	}
	if got.Message.Android.Priority != "high" {
		t.Errorf("priority = %q, want high", got.Message.Android.Priority)
	}
	if got.Message.Data["actions"] == "" {
		t.Errorf("actions missing from data")
	} else {
		var actions []Action
		if err := json.Unmarshal([]byte(got.Message.Data["actions"]), &actions); err != nil {
			t.Fatalf("actions not JSON: %v", err)
		}
		if len(actions) != 1 || actions[0].Label != "Open in Stripe" || actions[0].URL != "https://dashboard.stripe.com" {
			t.Errorf("actions = %+v", actions)
		}
	}
}

func TestPayloadNoActions(t *testing.T) {
	b, err := Payload("tok", Notification{Title: "t", Body: "b"})
	if err != nil {
		t.Fatalf("Payload: %v", err)
	}
	var got struct {
		Message struct {
			Data map[string]string `json:"data"`
		} `json:"message"`
	}
	if err := json.Unmarshal(b, &got); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if _, ok := got.Message.Data["actions"]; ok {
		t.Errorf("actions should be absent when there are none")
	}
}

func TestErrorUnregistered(t *testing.T) {
	cases := []struct {
		name string
		err  *Error
		want bool
	}{
		{"unregistered", &Error{Status: 404, StatusStr: "NOT_FOUND", ErrorCode: "UNREGISTERED"}, true},
		{"not found", &Error{Status: 404}, true},
		{"invalid argument", &Error{Status: 400, ErrorCode: "INVALID_ARGUMENT"}, true},
		{"malformed token message", &Error{Status: 400, Message: "The registration token is not valid"}, true},
		{"server error", &Error{Status: 500}, false},
		{"quota", &Error{Status: 429, StatusStr: "RESOURCE_EXHAUSTED"}, false},
		{"permission", &Error{Status: 403, StatusStr: "PERMISSION_DENIED"}, false},
	}
	for _, c := range cases {
		if got := c.err.Unregistered(); got != c.want {
			t.Errorf("%s: Unregistered() = %v, want %v", c.name, got, c.want)
		}
	}
}

func TestSendErrorUnregisteredIsSentinel(t *testing.T) {
	// A 404 wrapped as the sentinel must satisfy errors.Is, which is what the
	// delivery dispatcher uses to decide whether to clear a device token.
	inner := parseError(404, []byte(`{"error":{"code":404,"status":"NOT_FOUND","details":[{"errorCode":"UNREGISTERED"}]}}`))
	if !inner.Unregistered() {
		t.Fatalf("parsed error should be unregistered: %+v", inner)
	}
	wrapped := errors.Join(ErrUnregistered, inner)
	if !errors.Is(wrapped, ErrUnregistered) {
		t.Errorf("errors.Is should match ErrUnregistered")
	}
}
