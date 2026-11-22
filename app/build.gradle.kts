import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""

val releaseStoreFilePath: String? = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("KEYSTORE_PATH")
val releaseStorePassword: String? = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("KEY_ALIAS")
val releaseKeyPassword: String? = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("KEY_PASSWORD")
val hasReleaseSigning: Boolean = !releaseStoreFilePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "io.github.nemynew.expressiveweather"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.nemynew.expressiveweather"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_PLAY_DISTRIBUTION", "true")
            buildConfigField("boolean", "ENABLE_EXTERNAL_DONATION_LINKS", "false")
            buildConfigField("boolean", "ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG", "false")
        }
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_PLAY_DISTRIBUTION", "false")
            buildConfigField("boolean", "ENABLE_EXTERNAL_DONATION_LINKS", "true")
            buildConfigField("boolean", "ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Haze Glassmorphism / Backdrop Blur
  implementation(libs.haze)
  implementation(libs.haze.materials)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Kotlinx Serialization JSON
  implementation(libs.kotlinx.serialization.json)

  // Google Maps SDK for Android (Mobile Native Dynamic Maps - $0 Unlimited Free)
  implementation(libs.play.services.maps)

  // OkHttp (HTTP/2 Multiplexing & Connection Pooling for ultra-fast tile streaming)
  implementation(libs.okhttp)

  // WorkManager (Persistent, doze-proof background scheduling)
  implementation(libs.androidx.work)
}

ksp {
  arg("room.generateKotlin", "true")
  arg("room.schemaLocation", "$projectDir/schemas")
}
