plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yasserkhayal.androidprojects.airqualitymonitor.core.domain"
    compileSdk = 37
}

dependencies {
    // Domain-only; keep dependencies minimal in Phase 0
}
