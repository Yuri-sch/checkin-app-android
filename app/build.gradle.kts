plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sistemaseventos.checkinapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sistemaseventos.checkinapp"
        minSdk = 24
        targetSdk = 35
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
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Para fazer chamadas de API (API Gateway)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)

    // Para o banco de dados local OFFLINE (SQLite)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Para a sincronização automática em segundo plano
    implementation(libs.work.runtime)

    // Para ViewModels (parte do padrão Observer/MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.converter.scalars)
    implementation(libs.logging.interceptor)
}