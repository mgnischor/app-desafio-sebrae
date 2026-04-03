/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/AppSettings.kt
    Descrição: Modelo de domínio representando as preferências e configurações do aplicativo.
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
package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Preferências persistidas do usuário que controlam o comportamento global da aplicação.
 *
 * @property darkMode Indica se o tema escuro está habilitado.
 * @property pushEnabled Indica se as notificações push estão ativadas.
 * @property emailEnabled Indica se o envio de notificações por e-mail está ativado.
 * @property language Código do idioma da interface; valores suportados: `"pt"`, `"en"`, `"es"`.
 */
@Immutable
data class AppSettings(
    val darkMode: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
    val language: String = "pt", // pt, en, es
)
