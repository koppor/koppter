// [impl->adr~kotlin-multiplatform~1]
// The application: Compose UI shared by all targets, plus the desktop entry point.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(25)

    jvm("desktop")

    // [impl->dsn~android-shell~1]
    android {
        namespace = "io.github.koppor.koppter.ui"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons)
            implementation(libs.kotlinx.coroutines.core)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.appdirs)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.koppor.koppter.MainKt"
    }
}
