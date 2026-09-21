plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard"
    compileSdk = 37
}

dependencies {
    implementation(project(":core:domain"))
}
