package tech.datatower.sebrae.desafio.data.remote.firebase

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Armazena credenciais de seed do Firebase em storage protegido por Android Keystore.
 *
 * O valor eh cifrado com AES/GCM; a chave fica no Keystore do dispositivo.
 */
class FirebaseSeedCredentialStore(context: Context) {

  private val prefs: SharedPreferences =
      context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  /**
   * Executa a rotina de has credentials dentro do contexto deste componente.
   *
   * @return Resultado produzido pela opera??o em formato `Boolean`.
   */
  fun hasCredentials(): Boolean =
      decrypt(KEY_EMAIL_DATA, KEY_EMAIL_IV).isNotBlank() &&
          decrypt(KEY_PASSWORD_DATA, KEY_PASSWORD_IV).isNotBlank()

  /**
   * Obt?m dados necess?rios para email de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  fun getEmail(): String = decrypt(KEY_EMAIL_DATA, KEY_EMAIL_IV)

  /**
   * Obt?m dados necess?rios para password de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  fun getPassword(): String = decrypt(KEY_PASSWORD_DATA, KEY_PASSWORD_IV)

  /**
   * Executa a rotina de save credentials dentro do contexto deste componente.
   *
   * @param email Valor de entrada utilizado por esta opera??o.
   * @param password Valor de entrada utilizado por esta opera??o.
   */
  fun saveCredentials(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) return
    encryptAndSave(email, KEY_EMAIL_DATA, KEY_EMAIL_IV)
    encryptAndSave(password, KEY_PASSWORD_DATA, KEY_PASSWORD_IV)
  }

  /** Executa a rotina de clear credentials dentro do contexto deste componente. */
  fun clearCredentials() {
    prefs
        .edit()
        .remove(KEY_EMAIL_DATA)
        .remove(KEY_EMAIL_IV)
        .remove(KEY_PASSWORD_DATA)
        .remove(KEY_PASSWORD_IV)
        .apply()
  }

  /**
   * Executa a rotina de encrypt and save dentro do contexto deste componente.
   *
   * @param value Valor de entrada utilizado por esta opera??o.
   * @param dataKey Valor de entrada utilizado por esta opera??o.
   * @param ivKey Valor de entrada utilizado por esta opera??o.
   */
  private fun encryptAndSave(value: String, dataKey: String, ivKey: String) {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
    val ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    prefs.edit().putString(dataKey, encryptedBase64).putString(ivKey, ivBase64).apply()
  }

  /**
   * Executa a rotina de decrypt dentro do contexto deste componente.
   *
   * @param dataKey Valor de entrada utilizado por esta opera??o.
   * @param ivKey Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  private fun decrypt(dataKey: String, ivKey: String): String {
    val encryptedBase64 = prefs.getString(dataKey, null) ?: return ""
    val ivBase64 = prefs.getString(ivKey, null) ?: return ""
    return runCatching {
          val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
          val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
          val cipher = Cipher.getInstance(TRANSFORMATION)
          val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
          cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
          String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }
        .getOrDefault("")
  }

  /**
   * Obt?m dados necess?rios para or create secret key de forma consistente.
   *
   * @return Resultado produzido pela opera??o em formato `SecretKey`.
   */
  private fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existingKey != null) return existingKey

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec =
        KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  companion object {
    private const val PREFS_FILE = "firebase_seed_secure_prefs"
    private const val KEY_EMAIL_DATA = "firebase_seed_email_data"
    private const val KEY_EMAIL_IV = "firebase_seed_email_iv"
    private const val KEY_PASSWORD_DATA = "firebase_seed_password_data"
    private const val KEY_PASSWORD_IV = "firebase_seed_password_iv"

    private const val KEY_ALIAS = "firebase_seed_keystore_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
  }
}
