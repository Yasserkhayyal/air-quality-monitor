plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yasserkhayal.androidprojects.airqualitymonitor.core.data"
    compileSdk = 37
}

dependencies {
    implementation(project(":core:domain"))
}
