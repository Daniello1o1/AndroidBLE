plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")  // <-- agregar esto
}

android {
    namespace = "com.upiiz.ble_sipi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.upiiz.ble_sipi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // BOM primero
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    // Dependencias Firebase después — sin versión porque el BOM las maneja
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.mpandroidchart)
    implementation("com.github.wendykierp:JTransforms:3.1")

}
