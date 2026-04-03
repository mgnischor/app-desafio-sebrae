plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt)
}

kotlin { jvmToolchain(17) }

android {
  namespace = "tech.datatower.sebrae.desafio.core"
  compileSdk = 36

  defaultConfig { minSdk = 26 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures { compose = true }
}

dependencies {
  // Exposed to consumers (app module needs these for DI wiring)
  api(libs.androidx.room.runtime)
  api(libs.androidx.room.ktx)
  api(libs.firebase.auth.ktx)
  api(libs.firebase.firestore.ktx)
  api(platform(libs.firebase.bom))
  api(libs.jetbrains.kotlinx.coroutines.play.services)

  // Internal
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(platform(libs.androidx.compose.bom))

  ksp(libs.androidx.room.compiler)
  implementation(libs.hilt.android)
  ksp(libs.hilt.android.compiler)
}
