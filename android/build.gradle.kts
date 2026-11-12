// [impl->dsn~android-shell~1]
// The Android application: a thin shell around the shared UI in :app.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.koppor.koppter.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.koppor.koppter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core"))
    implementation(libs.compose.runtime)
    implementation(libs.androidx.activity.compose)
}
