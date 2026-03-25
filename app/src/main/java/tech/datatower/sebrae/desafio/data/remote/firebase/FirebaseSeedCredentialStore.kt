package tech.datatower.sebrae.desafio.data.remote.firebase

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Armazena credenciais de seed do Firebase em armazenamento criptografado.
 *
 * Usa EncryptedSharedPreferences com chave protegida por Android Keystore.
 */
class FirebaseSeedCredentialStore(context: Context) {

  private val prefs: SharedPreferences by lazy {
    val masterKey =
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  fun hasCredentials(): Boolean =
      prefs.getString(KEY_EMAIL, null).isNullOrBlank().not() &&
          prefs.getString(KEY_PASSWORD, null).isNullOrBlank().not()

  fun getEmail(): String = prefs.getString(KEY_EMAIL, "").orEmpty()

  fun getPassword(): String = prefs.getString(KEY_PASSWORD, "").orEmpty()

  fun saveCredentials(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) return
    prefs.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply()
  }

  fun clearCredentials() {
    prefs.edit().remove(KEY_EMAIL).remove(KEY_PASSWORD).apply()
  }

  companion object {
    private const val PREFS_FILE = "firebase_seed_secure_prefs"
    private const val KEY_EMAIL = "firebase_seed_email"
    private const val KEY_PASSWORD = "firebase_seed_password"
  }
}

