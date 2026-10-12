plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The google-services plugin hard-fails when google-services.json is absent,
// so apply it only when the file exists. Without it the project still builds
// (anyone can compile with their own Firebase project); push stays disabled
// at runtime because the Firebase SDK cannot fetch a token. See FIREBASE.md.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "dev.boop.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.boop.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // QR pairing scanner (pure, no Google Play Services).
    implementation(libs.zxing.embedded)

    // Push notifications via Firebase Cloud Messaging. Works only with the
    // developer's own google-services.json (see FIREBASE.md); without it the
    // project still builds and the app runs, but PushRegistrar silently skips
    // token registration and no pushes arrive.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.ui.tooling)
}
