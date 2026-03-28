/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/Certificate.kt
    Descrição: Modelo de domínio representando um certificado emitido a um aluno.
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
 * Representa um certificado emitido para conclusão de curso.
 *
 * @property id Identificador único do certificado.
 * @property studentName Nome do aluno certificado.
 * @property courseName Nome do curso concluído.
 * @property issuedDate Data de emissão no formato de exibição da interface.
 * @property hours Carga horária certificada em horas.
 * @property code Código único de validação do certificado.
 */
@Immutable
data class Certificate(
    val id: Int,
    val studentName: String,
    val courseName: String,
    val issuedDate: String,
    val hours: Int,
    val code: String,
)
