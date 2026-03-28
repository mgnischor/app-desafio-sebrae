/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/Student.kt
    Descrição: Modelo de domínio representando um aluno matriculado na instituição.
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
 * Representa um aluno exibido nas telas de gestão acadêmica.
 *
 * O modelo é imutável para favorecer estabilidade de recomposição no Compose.
 *
 * @property id Identificador único do aluno.
 * @property name Nome completo do aluno.
 * @property email E-mail de contato.
 * @property course Nome do curso principal no qual o aluno está matriculado.
 * @property enrolledClass Identificação da turma atual do aluno.
 * @property progress Progresso de conclusão do curso no intervalo de `0f..1f`.
 * @property status Situação acadêmica atual do aluno.
 */
@Immutable
data class Student(
    val id: Int,
    val name: String,
    val email: String,
    val course: String,
    val enrolledClass: String,
    val progress: Float, // 0f..1f
    val status: StudentStatus,
)

/** Situações possíveis para o ciclo acadêmico do aluno. */
enum class StudentStatus {
  Active,
  Inactive,
  Graduated,
}
