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

    // The key of the published APK is not in the repository (decision 0010).
    // Without it, as on a developer machine, the release APK stays unsigned and the debug APK is the one to install.
    // [impl->adr~signing-key-as-secret~1]
    val keystoreFile = providers.environmentVariable("KOPPTER_KEYSTORE_FILE").orNull
    if (keystoreFile != null) {
        val password = providers.environmentVariable("KOPPTER_KEYSTORE_PASSWORD").get()
        signingConfigs {
            create("published") {
                storeFile = file(keystoreFile)
                storeType = "PKCS12"
                storePassword = password
                keyAlias = "koppter"
                keyPassword = password
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("published")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    // JGit and Apache MINA sshd are OSGi bundles; several of their jars carry bundle metadata under the same name.
    packaging {
        resources.excludes += setOf("OSGI-INF/**", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "about.html", "plugin.properties")
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core"))
    implementation(libs.compose.runtime)
    implementation(libs.androidx.activity.compose)
}
