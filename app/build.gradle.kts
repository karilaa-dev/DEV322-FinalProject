import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String, fallback: String = ""): String {
    return localProperties.getProperty(name, fallback)
}

fun quoted(value: String): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bananaginger.noisedetector"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bananaginger.noisedetector"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            localProperty("GOOGLE_MAPS_API_KEY")

        buildConfigField(
            "String",
            "ATLAS_DATABASE",
            quoted(localProperty("ATLAS_DATABASE", "bananaginger"))
        )
        buildConfigField(
            "String",
            "ATLAS_ANOMALY_COLLECTION",
            quoted(localProperty("ATLAS_ANOMALY_COLLECTION", "anomalies"))
        )
        buildConfigField(
            "String",
            "ATLAS_EARTHQUAKE_COLLECTION",
            quoted(localProperty("ATLAS_EARTHQUAKE_COLLECTION", "earthquakes"))
        )
        buildConfigField(
            "String",
            "MONGODB_CONNECTION_STRING",
            quoted(localProperty("MONGODB_CONNECTION_STRING"))
        )
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/native-image/**"
        }
    }
}

dependencies {
    // Core AndroidX and Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Other libraries
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(project(":sasl-stubs"))
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.mongodb:mongodb-driver-sync:5.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:6.12.0")

    // Room (use version catalog)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
