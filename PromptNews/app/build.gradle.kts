plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.digitalturbine.promptnews"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.digitalturbine.promptnews"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "SERPAPI_KEY",
            "\"${project.findProperty("SERPAPI_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "SPORTS_API_BASE_URL",
            "\"${project.findProperty("SPORTS_API_BASE_URL") ?: ""}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // No composeCompiler block needed with the plugin; defaults are fine
    composeOptions { }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging { resources { excludes += setOf("META-INF/*") } }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Navigation for NavHost in MainActivity
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material Icons (Search/Home/Heart)
    implementation("androidx.compose.material:material-icons-extended")

    // Material Components (provides Theme.Material3.* XML themes)
    implementation("com.google.android.material:material:1.12.0")

    // Images
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Networking + logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // HTML parsing for og:image / twitter:image, etc.
    implementation("org.jsoup:jsoup:1.17.2")

    // Room for caching
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
