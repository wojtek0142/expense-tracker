plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.expense_tracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.expense_tracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)   // NOWE – ikony

    // AndroidX Core / Lifecycle / Activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)        // NOWE – collectAsState()
    implementation(libs.androidx.activity.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)                 // NOWE

    // Hilt
    implementation(libs.hilt.android)                                // NOWE
    ksp(libs.hilt.compiler)                                          // NOWE
    implementation(libs.androidx.hilt.navigation.compose)            // NOWE – hiltViewModel()

    // Google ML Kit Text Recognition (OCR na telefonie)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Material / Coordinator
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.material)

    // Testy
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}