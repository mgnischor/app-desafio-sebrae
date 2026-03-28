/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/remote/firebase/FirebaseSeedCredentialStore.kt
    Descrição: Armazenamento local de credenciais de seed para autenticação no Firebase.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
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
 * O valor é cifrado com AES/GCM; a chave fica no Keystore do dispositivo.
 */
class FirebaseSeedCredentialStore(context: Context) {

  private val prefs: SharedPreferences =
      context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  /**
   * Executa a rotina de has credentials dentro do contexto deste componente.
   *
   * @return Resultado produzido pela operação em formato `Boolean`.
   */
  fun hasCredentials(): Boolean =
      decrypt(KEY_EMAIL_DATA, KEY_EMAIL_IV).isNotBlank() &&
          decrypt(KEY_PASSWORD_DATA, KEY_PASSWORD_IV).isNotBlank()

  /**
   * Obtém dados necessários para email de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `String`.
   */
  fun getEmail(): String = decrypt(KEY_EMAIL_DATA, KEY_EMAIL_IV)

  /**
   * Obtém dados necessários para password de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `String`.
   */
  fun getPassword(): String = decrypt(KEY_PASSWORD_DATA, KEY_PASSWORD_IV)

  /**
   * Executa a rotina de save credentials dentro do contexto deste componente.
   *
   * @param email Valor de entrada utilizado por esta operação.
   * @param password Valor de entrada utilizado por esta operação.
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
   * @param value Valor de entrada utilizado por esta operação.
   * @param dataKey Valor de entrada utilizado por esta operação.
   * @param ivKey Valor de entrada utilizado por esta operação.
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
   * @param dataKey Valor de entrada utilizado por esta operação.
   * @param ivKey Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `String`.
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
   * Obtém dados necessários para or create secret key de forma consistente.
   *
   * @return Resultado produzido pela operação em formato `SecretKey`.
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
