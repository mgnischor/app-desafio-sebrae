import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.kotlin.compose)

  id("com.google.gms.google-services")
}

val localProperties =
    Properties().apply {
      val localPropertiesFile = rootProject.file("local.properties")
      if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
      }
    }

val firebaseSeedEmail = (localProperties.getProperty("firebase.seed.email") ?: "").trim()
val firebaseSeedPassword = (localProperties.getProperty("firebase.seed.password") ?: "").trim()

fun toBuildConfigString(value: String): String {
  val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"")
  return "\"$escapedValue\""
}

kotlin {
  jvmToolchain(17)
}

android {
  namespace = "tech.datatower.sebrae.desafio"
  compileSdk = 36

  defaultConfig {
    applicationId = "tech.datatower.sebrae.desafio"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    buildConfigField("boolean", "FIREBASE_REMOTE_BOOTSTRAP_ENABLED", "true")
    buildConfigField("String", "FIREBASE_SEED_EMAIL", toBuildConfigString(firebaseSeedEmail))
    buildConfigField(
        "String",
        "FIREBASE_SEED_PASSWORD",
        toBuildConfigString(firebaseSeedPassword),
    )
    buildConfigField(
        "boolean",
        "FIREBASE_SEED_CREDENTIALS_CONFIGURED",
        (firebaseSeedEmail.isNotBlank() && firebaseSeedPassword.isNotBlank()).toString(),
    )

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.firestore.ktx)
  implementation(libs.hilt.android)
  implementation(libs.jetbrains.kotlinx.coroutines.play.services)
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))

  ksp(libs.androidx.room.compiler)
  ksp(libs.hilt.android.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
