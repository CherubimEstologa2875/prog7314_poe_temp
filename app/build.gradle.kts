

// Build script for the Runway Android app (IIE, 2026; Square, Inc., n.d.).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Which API the app talks to. Set to false to use the API running on this PC instead.
val useHostedApi = true

val hostedApiUrl = "https://prog7314-poe-p2-group11.onrender.com/"
// 10.0.2.2 is how the emulator reaches this PC's localhost. Only works on an emulator.
val localApiUrl = "http://10.0.2.2:8080/"
val apiBaseUrl = if (useHostedApi) hostedApiUrl else localApiUrl

android {
    namespace = "com.example.runway"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.runway"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    // Every machine normally signs debug builds with its own ~/.android/debug.keystore,
    // and Google Sign-In only accepts certificates registered in Firebase. Signing with
    // a keystore kept in the repo means anyone who clones the project can sign in.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/runway-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // ViewBinding generates a binding class per XML layout, so no findViewById.
        viewBinding = true
        buildConfig = true
    }

    // Lets unit tests run code that logs with android.util.Log instead of crashing.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Room exports its schema so migrations are reviewable in a pull request.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Material widgets that the Runway component library styles.
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.biometric)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)

    // Removes the background from a garment photo, on the device.
    implementation(libs.mlkit.subject.segmentation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/
