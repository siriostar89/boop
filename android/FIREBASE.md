# Firebase setup (bring your own keys)

Push notifications on Android go through **Firebase Cloud Messaging (FCM)**.
Firebase credentials are **per developer and never committed**: each person
compiling the Boop Android app must create their own Firebase project and
drop their own keys in place. Without them the app still builds and runs —
inbox, event detail and pairing all work — but no pushes arrive.

Two separate artifacts are needed: one for the **app**, one for the **server**.

## 1. Create a Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com) and sign in.
2. **Add project** → any name (e.g. `boop-android`) → Continue.
3. Google Analytics is optional (Boop does not use it) → **Create project**.

## 2. App key: `google-services.json`

4. On the project home click **Add an app** → **Android**.
5. **Android package name** must be exactly `dev.boop.android`
   (it must match `applicationId` in `app/build.gradle.kts`).
6. **App nickname**: anything (e.g. `Boop`). Leave the signing certificate empty.
7. **Register app** → at the **Download config file** step, download
   `google-services.json`.
8. Place it at `android/app/google-services.json` (next to `build.gradle.kts`).
   It is git-ignored (`app/google-services.json` in `android/.gitignore`),
   so it never lands in the repo.
9. You can **skip** the console's remaining steps ("Add Firebase SDK",
   "Run your app"): the code is already wired in this repo.

## 3. Server key: service account

The Boop server does **not** use `google-services.json`. It sends pushes via
the FCM HTTP v1 API authenticating as a **service account**:

10. Firebase console → gear icon → **Project settings** → **Service accounts**.
11. **Generate new private key** → you get a JSON file. **It is a secret**:
    never commit it.
12. Put it on the server host outside the repo (e.g.
    `/etc/boop/firebase-key.json`, mode `600`) and set:
    ```sh
    FCM_PROJECT_ID=<project_id from step 7>
    FCM_SERVICE_ACCOUNT_PATH=/etc/boop/firebase-key.json
    ```
    Alternative without a file: paste the JSON (or its base64) into
    `FCM_SERVICE_ACCOUNT_JSON`. Either variable works.
13. Without these env vars FCM stays **disabled** and everything works as
    before (iOS/APNs only) — this is by design.

## 4. How the app behaves

- With `google-services.json` present, the google-services Gradle plugin is
  applied and on first launch (and whenever FCM rotates the token) the app
  registers it via `POST /api/v1/devices` (`PushRegistrar`, mirrored on
  iOS's APNs re-registration). Tapping a notification opens the event detail
  (`MainActivity.EXTRA_EVENT_ID`, same `event_id` key the server puts in the
  FCM data payload).
- Without `google-services.json`, the plugin is skipped (the build would
  otherwise hard-fail), Firebase cannot fetch a token and registration is
  silently skipped. All non-push features keep working.

## 5. Local development layout (maintainer)

The maintainer keeps secrets under the git-ignored `private.local/` folder
at the repo root (see `.gitignore`):

```text
private.local/
  <project>-firebase-adminsdk-*.json   # service-account key (server only)
```

`android/app/google-services.json` is the app key (client only). The two
files serve different ends and are not interchangeable.
