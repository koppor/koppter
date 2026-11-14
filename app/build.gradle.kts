// [impl->adr~kotlin-multiplatform~1]
// The application: Compose UI shared by all targets, plus the desktop entry point.

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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

// The version of a package is the day of the release as YY.M.D:
// the formats of Windows and macOS take three numbers, of which the first is above 0 and below 256.
val releaseDay = providers.environmentVariable("KOPPTER_VERSION").orNull?.let { Regex("""\d\d(\d\d)-(\d\d)-(\d\d)""").matchEntire(it) }
val desktopVersion = releaseDay?.destructured?.let { (year, month, day) -> "$year.${month.toInt()}.${day.toInt()}" } ?: "1.0.0"

compose.desktop {
    application {
        mainClass = "io.github.koppor.koppter.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "koppter"
            packageVersion = desktopVersion
            description = "Tasks in a todo.txt file"
            // What `suggestRuntimeModules` finds for JGit, its SSH client and JNA, plus naming for Kerberos and the bridge for screen readers.
            // All modules cannot be included: that list contains jlink itself, which a JDK without packaged modules cannot link.
            modules("java.instrument", "java.management", "java.naming", "java.rmi", "java.security.jgss", "java.sql", "jdk.accessibility", "jdk.unsupported")
            linux {
                iconFile = project.file("src/desktopMain/resources/koppter.png")
            }
            windows {
                iconFile = project.file("packaging/koppter.ico")
                // Identifies the application to the installer, so that a newer package replaces the installed one.
                upgradeUuid = "7978242a-0e73-4e9d-a3ff-1ade94c65369"
                menu = true
                perUserInstall = true
            }
            macOS {
                iconFile = project.file("packaging/koppter.icns")
                bundleID = "io.github.koppor.koppter"
            }
        }
    }
}
