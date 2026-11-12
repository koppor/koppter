// Root project: cross-cutting configuration. Code lives in the subprojects.

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.openfasttrace)
}

// [impl->adr~openfasttrace~1]
requirementTracing {
    inputDirectories = files(
        "docs/requirements",
        "docs/decisions",
        "build.gradle.kts",
        "core/build.gradle.kts",
        "core/src",
        "app/build.gradle.kts",
        "app/src",
        "android/build.gradle.kts",
        "android/src",
        ".github/workflows",
    )
}
