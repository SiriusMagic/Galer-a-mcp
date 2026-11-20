plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.luma.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.luma.gallery"
        minSdk = 29 // RELATIVE_PATH (albums by folder) and scoped storage
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"
        // ML Kit's on-device models are native code: only 64-bit ARM phones (all recent ones), so the APK stays small.
        ndk { abiFilters += "arm64-v8a" }
        // Modo MCP: the same public relay as Stone (one device id per app). Empty = no MCP over the internet.
        val relayUrl = (project.findProperty("lumaRelayUrl") as String?).orEmpty().trimEnd('/')
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed like Stone and Gateway Hub, so the hub recognises Luma as the same developer.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // Phone app built for 64-bit ARM only (see abiFilters); ChromeOS/x86 would add ~23 MB of ML Kit code.
    lint { disable += "ChromeOsAbiSupport" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Screenshots of the screens land in app/build/screenshots (compare them with design/concepto.png).
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    // On-device only (bundled models, no internet): what a photo shows and the text inside it.
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    // Indexing runs in the background while charging with the screen off.
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    // Bóveda: the phone's own lock (fingerprint, face, PIN…).
    implementation("androidx.biometric:biometric:1.1.0")
    // Modo MCP: WebSocket to the relay (same library and version as Stone).
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
