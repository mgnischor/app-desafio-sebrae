/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/Teacher.kt
    Descrição: Modelo de domínio representando um professor da instituição.
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
 * Representa um instrutor cadastrado no sistema.
 *
 * @property id Identificador único do instrutor.
 * @property name Nome completo do instrutor.
 * @property email E-mail institucional.
 * @property specialty Área principal de atuação.
 * @property activeCourses Quantidade de cursos em que atua atualmente.
 * @property totalStudents Total de alunos atendidos em suas turmas ativas.
 * @property rating Avaliação média do instrutor no intervalo de `0f..5f`.
 * @property isActive Indica se o instrutor está ativo no sistema.
 */
@Immutable
data class Teacher(
    val id: Int,
    val name: String,
    val email: String,
    val specialty: String,
    val activeCourses: Int,
    val totalStudents: Int,
    val rating: Float, // 0f..5f
    val isActive: Boolean,
)
