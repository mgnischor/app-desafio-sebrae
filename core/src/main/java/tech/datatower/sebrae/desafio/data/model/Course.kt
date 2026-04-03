/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/Course.kt
    Descrição: Modelo de domínio representando um curso oferecido pela instituição.
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
 * Representa um curso oferecido pela instituição.
 *
 * @property id Identificador único do curso.
 * @property title Título de exibição do curso.
 * @property category Categoria temática do curso.
 * @property instructor Nome do instrutor principal.
 * @property totalStudents Quantidade total de alunos matriculados.
 * @property durationHours Carga horária total em horas.
 * @property completionRate Taxa de conclusão dos alunos no intervalo de `0f..1f`.
 * @property isPublished Indica se o curso está publicado para matrícula.
 * @property startDate Data de início do curso no formato ISO-8601 (`"YYYY-MM-DD"`), ou `null` se
 *   não definida.
 */
@Immutable
data class Course(
    val id: Int,
    val title: String,
    val category: String,
    val instructor: String,
    val totalStudents: Int,
    val durationHours: Int,
    val completionRate: Float, // 0f..1f
    val isPublished: Boolean,
    val startDate: String? = null,
)
