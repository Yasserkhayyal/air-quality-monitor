// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.compose.screenshot) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.android.library) apply false
}

spotless {
    kotlin {
        target("**/src/*/java/**/*.kt", "**/src/*/kotlin/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "android" to "true",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}
