// Package fcm sends push notifications to Android devices through Firebase
// Cloud Messaging (FCM) using the HTTP v1 API and a service-account credential.
package fcm

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"

	"golang.org/x/oauth2/jwt"

	"github.com/chrisgreg/boop/server/internal/config"
)

// messagingScope is the OAuth2 scope required to call the FCM send endpoint.
const messagingScope = "https://www.googleapis.com/auth/firebase.messaging"

// defaultTokenURL is Google's OAuth2 token endpoint, used when the service
// account JSON does not carry its own token_uri.
const defaultTokenURL = "https://oauth2.googleapis.com/token"

// sendURL is the FCM HTTP v1 endpoint template.
const sendURL = "https://fcm.googleapis.com/v1/projects/%s/messages:send"

// ErrUnregistered reports that a device token is no longer valid and the
// caller should drop it.
var ErrUnregistered = errors.New("fcm: device token is not registered")

// Notification is the alert to deliver.
type Notification struct {
	Title     string
	Body      string
	EventID   string
	ProjectID string
	// Actions are buttons the Android app renders on the notification.
	Actions []Action
}

// Action is a notification button that opens a URL.
type Action struct {
	Label string `json:"label"`
	URL   string `json:"url"`
}

// Client sends notifications through FCM.
type Client struct {
	projectID string
	http      *http.Client
}

// serviceAccount is the subset of a Google service-account JSON key we need.
type serviceAccount struct {
	Type         string `json:"type"`
	ClientEmail  string `json:"client_email"`
	PrivateKey   string `json:"private_key"`
	PrivateKeyID string `json:"private_key_id"`
	TokenURI     string `json:"token_uri"`
}

// New builds a Client from configuration. The returned HTTP client injects and
// refreshes the OAuth2 access token automatically.
func New(cfg config.FCM) (*Client, error) {
	if missing := cfg.Missing(); len(missing) > 0 {
		return nil, fmt.Errorf("missing %s", strings.Join(missing, ", "))
	}
	key, err := serviceAccountJSON(cfg)
	if err != nil {
		return nil, err
	}
	var sa serviceAccount
	if err := json.Unmarshal(key, &sa); err != nil {
		return nil, fmt.Errorf("parse FCM service account: %w", err)
	}
	if sa.ClientEmail == "" || sa.PrivateKey == "" {
		return nil, errors.New("FCM service account is missing client_email or private_key")
	}
	tokenURL := sa.TokenURI
	if tokenURL == "" {
		tokenURL = defaultTokenURL
	}
	// The service account signs an RS256 JWT that is exchanged for an access
	// token; jwt.Config yields an HTTP client that refreshes it as needed.
	jwtCfg := &jwt.Config{
		Email:        sa.ClientEmail,
		PrivateKey:   []byte(sa.PrivateKey),
		PrivateKeyID: sa.PrivateKeyID,
		Scopes:       []string{messagingScope},
		TokenURL:     tokenURL,
	}
	return &Client{projectID: cfg.ProjectID, http: jwtCfg.Client(context.Background())}, nil
}

// serviceAccountJSON returns the service-account JSON, reading the configured
// file when set, otherwise decoding the inline value (raw JSON or base64).
func serviceAccountJSON(cfg config.FCM) ([]byte, error) {
	if cfg.ServiceAccountPath != "" {
		b, err := os.ReadFile(cfg.ServiceAccountPath)
		if err != nil {
			return nil, fmt.Errorf("read FCM_SERVICE_ACCOUNT_PATH: %w", err)
		}
		return b, nil
	}
	raw := strings.TrimSpace(cfg.ServiceAccountJSON)
	if !strings.HasPrefix(raw, "{") {
		if decoded, err := base64.StdEncoding.DecodeString(raw); err == nil {
			return decoded, nil
		}
	}
	return []byte(raw), nil
}

// Payload builds the FCM HTTP v1 request body for n targeting deviceToken.
func Payload(deviceToken string, n Notification) ([]byte, error) {
	data := map[string]string{
		"event_id":   n.EventID,
		"project_id": n.ProjectID,
	}
	if len(n.Actions) > 0 {
		actions, err := json.Marshal(n.Actions)
		if err != nil {
			return nil, err
		}
		// The Android app turns these into notification action buttons.
		data["actions"] = string(actions)
	}
	body := map[string]any{
		"message": map[string]any{
			"token":        deviceToken,
			"notification": map[string]string{"title": n.Title, "body": n.Body},
			"data":         data,
			"android":      map[string]any{"priority": "high"},
		},
	}
	return json.Marshal(body)
}

// Send delivers n to deviceToken and returns the FCM message name on success.
func (c *Client) Send(ctx context.Context, deviceToken string, n Notification) (string, error) {
	body, err := Payload(deviceToken, n)
	if err != nil {
		return "", err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, fmt.Sprintf(sendURL, c.projectID), bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.http.Do(req)
	if err != nil {
		return "", &Error{Status: http.StatusServiceUnavailable, Message: err.Error()}
	}
	defer resp.Body.Close()
	respBody, _ := io.ReadAll(io.LimitReader(resp.Body, 8192))
	if resp.StatusCode == http.StatusOK {
		var ok struct {
			Name string `json:"name"`
		}
		if err := json.Unmarshal(respBody, &ok); err == nil {
			return ok.Name, nil
		}
		return "", nil
	}
	e := parseError(resp.StatusCode, respBody)
	if e.Unregistered() {
		return "", fmt.Errorf("%w: %s", ErrUnregistered, e.Message)
	}
	return "", e
}

// Error is a non-2xx response from FCM.
type Error struct {
	Status    int
	StatusStr string
	ErrorCode string
	Message   string
}

func (e *Error) Error() string {
	return fmt.Sprintf("fcm: %d %s %s", e.Status, e.StatusStr, e.Message)
}

// Unregistered reports whether the device token is no longer deliverable and
// should be cleared. FCM signals this either as UNREGISTERED (404) or as an
// INVALID_ARGUMENT (400) for a malformed registration token.
func (e *Error) Unregistered() bool {
	if e.ErrorCode == "UNREGISTERED" || e.Status == http.StatusNotFound {
		return true
	}
	return e.Status == http.StatusBadRequest &&
		(e.ErrorCode == "INVALID_ARGUMENT" || strings.Contains(strings.ToLower(e.Message), "registration token"))
}

// parseError decodes an FCM error body.
func parseError(status int, body []byte) *Error {
	var parsed struct {
		Error struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Status  string `json:"status"`
			Details []struct {
				ErrorCode string `json:"errorCode"`
			} `json:"details"`
		} `json:"error"`
	}
	_ = json.Unmarshal(body, &parsed)
	msg := parsed.Error.Message
	if msg == "" {
		msg = strings.TrimSpace(string(body))
	}
	code := ""
	for _, d := range parsed.Error.Details {
		if d.ErrorCode != "" {
			code = d.ErrorCode
			break
		}
	}
	return &Error{Status: status, StatusStr: parsed.Error.Status, ErrorCode: code, Message: msg}
}
