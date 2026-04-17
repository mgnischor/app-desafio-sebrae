# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ── Preserve line numbers for crash reporting ──────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Firebase Firestore / Auth ──────────────────────────────────────────────────
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.**

# ── Hilt / Dagger ──────────────────────────────────────────────────────────────
-dontwarn dagger.hilt.internal.**
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# ── Room entities (data classes mapped to DB) ──────────────────────────────────
-keep class tech.datatower.sebrae.desafio.data.local.** { *; }

# ── Domain models (used in reflection-based serialization) ─────────────────────
-keep class tech.datatower.sebrae.desafio.data.model.** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── Compose ────────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
