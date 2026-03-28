/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/SchoolClass.kt
    Descrição: Modelo de domínio representando uma turma ou classe escolar.
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
 * Representa uma turma vinculada a um curso, com dados operacionais para acompanhamento.
 *
 * @property id Identificador único da turma.
 * @property name Nome ou código da turma.
 * @property course Curso associado à turma.
 * @property instructor Nome do instrutor responsável.
 * @property studentsCount Quantidade atual de alunos matriculados.
 * @property maxCapacity Capacidade máxima de alunos da turma.
 * @property schedule Descrição textual do horário das aulas.
 * @property status Estado atual da turma.
 */
@Immutable
data class SchoolClass(
    val id: Int,
    val name: String,
    val course: String,
    val instructor: String,
    val studentsCount: Int,
    val maxCapacity: Int,
    val schedule: String,
    val status: ClassStatus,
)

/** Estados de execução de uma turma ao longo do período letivo. */
enum class ClassStatus {
  Open,
  InProgress,
  Closed,
}
