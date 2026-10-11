# Boop per Android

Client Android nativo (Kotlin + Jetpack Compose) per un server [Boop](../README.md) self-hosted.

> Stato: **scaffold**. Il progetto Gradle compila una app minima (una schermata segnaposto).
> Pairing, inbox e push FCM arrivano nelle fasi successive. Piano completo in
> [`../QWEN.md`](../QWEN.md); lavorazioni in [`../DEVLOG.md`](../DEVLOG.md).

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3), `minSdk 26`, `targetSdk 35`, Java 17.
- **Retrofit + OkHttp + kotlinx.serialization** per le API REST.
- **Coroutines + Flow**; architettura MVVM (ViewModel + StateFlow).
- Version catalog in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Build

Servono Android Studio (o un JDK 17 + Android SDK con `compileSdk 35`).

```bash
# La prima volta genera il wrapper Gradle (il JAR non è committato):
cd android && gradle wrapper
# Poi:
./gradlew :app:assembleDebug        # APK di debug in app/build/outputs/apk/debug/
```

In alternativa apri la cartella `android/` con Android Studio: genera il wrapper e sincronizza.

> ⚠️ Le versioni in `libs.versions.toml` sono un set stabile noto, **non ancora verificate** su
> questa macchina (qui non c'è l'Android SDK). Android Studio può proporre aggiornamenti.

## Nota FCM (fase 2)

Per la ricezione delle notifiche servirà un **progetto Firebase** dedicato ed il file
`app/google-services.json`, che è **specifico del progetto e non va committato** (già in
`.gitignore`). A quel punto si aggiungeranno il plugin `com.google.gms.google-services` e la
dipendenza `firebase-messaging`.

## Struttura

```
android/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml          # version catalog
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── google-services.json        # fase 2, locale, non committare
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/             # strings, themes
        └── kotlin/dev/boop/android/
            ├── BoopApp.kt          # Application
            ├── MainActivity.kt
            ├── data/
            │   ├── model/          # Level, Device, Pairing, HealthStatus
            │   └── remote/         # BoopApi (Retrofit), ApiFactory
            └── ui/
                ├── RootScreen.kt   # schermata segnaposto
                └── theme/          # Color, Theme
```

Cartelle previste ma non ancora create: `domain/` (use case), `push/` (servizio FCM),
`pairing/` (scanner QR).

## Identificatori

`namespace`/`applicationId` sono `dev.boop.android` (segnaposto): cambiali con il tuo, come il
bundle id dell'app iOS.

## Riferimenti

- API server: `../README.md` (sezione *API*) e `../integration-llms.md`
- App iOS (riferimento funzionale): `../ios/`
- Piano e decisioni: `../QWEN.md`
