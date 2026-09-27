plugins {
    id("com.android.library")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.yasserkhayal.androidprojects.airqualitymonitor.core.data"
    compileSdk = 37
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.hilt.android)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
