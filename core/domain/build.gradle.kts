plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.yasserkhayal.androidprojects.airqualitymonitor.core.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
