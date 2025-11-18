plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.st10028374.vitality_vault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.st10028374.vitality_vault"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Use the default debug signingConfig
    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Later, you can create("release") for Play Store
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug") // temporary for now
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}


dependencies {

    // =========== CORE DEPENDENCIES ===========
    implementation(libs.androidx.gridlayout)
    implementation(libs.auth.v125)
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Location Services
    implementation(libs.play.services.location)

    // =========== FIREBASE (via BOM) ===========
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging.ktx)

    // Credentials API
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)


    // =========== COROUTINES ===========
    implementation(libs.kotlinx.coroutines.android)


    // =========== LIFECYCLE ===========
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Additional Lifecycle (not in catalog yet)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")


    // =========== ROOM ===========
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.room.ktx)

    val roomVersion = "2.8.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")


    // =========== WORK MANAGER ===========
    implementation(libs.androidx.work.runtime.ktx)


    // =========== NAVIGATION ===========
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")


    // =========== COMPOSE ===========
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Activity Compose
    implementation(libs.androidx.activity.compose)


    // =========== IMAGE LOADING ===========
    implementation("io.coil-kt:coil-compose:2.5.0")


    // =========== SPOTIFY ===========
    implementation(libs.auth.v125)


    // =========== GOOGLE AUTH (Legacy libs included) ===========
    implementation("com.google.android.gms:play-services-auth:20.7.0")


    // =========== NETWORKING ===========
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")


    // =========== MAPBOX ===========
    implementation("com.mapbox.maps:android:10.16.1")


    // =========== UI: RecyclerView ===========
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")


    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


    // =========== TESTING ===========
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.swiperefreshlayout)

}