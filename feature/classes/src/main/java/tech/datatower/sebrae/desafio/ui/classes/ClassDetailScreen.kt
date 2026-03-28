/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/classes/ClassDetailScreen.kt
    Descrição: Tela de detalhes de uma turma, exibindo alunos matriculados e informações gerais.
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
package tech.datatower.sebrae.desafio.ui.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/**
 * Executa a rotina de class detail screen dentro do contexto deste componente.
 *
 * @param classId Valor de entrada utilizado por esta operação.
 * @param onBack Valor de entrada utilizado por esta operação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    onBack: () -> Unit = {},
) {
  val viewModel: ClassDetailViewModel = hiltViewModel()
  val schoolClass by viewModel.schoolClass.collectAsState()
  val students by viewModel.students.collectAsState()

  DetailScaffold(title = stringResource(R.string.class_detail_title), onBack = onBack) {
      innerPadding,
      _ ->
    if (schoolClass == null) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
        Text(stringResource(R.string.class_not_found), style = MaterialTheme.typography.bodyLarge)
      }
      return@DetailScaffold
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
                schoolClass!!.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.label_course, schoolClass!!.course),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.label_instructor, schoolClass!!.instructor),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.label_schedule, schoolClass!!.schedule),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    R.string.label_occupation,
                    schoolClass!!.studentsCount,
                    schoolClass!!.maxCapacity,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.status_label, classStatusLabel(schoolClass!!.status)),
                style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }

      item {
        Text(
            text = stringResource(R.string.label_class_students, students.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      if (students.isEmpty()) {
        item {
          Text(
              stringResource(R.string.no_class_students),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      } else {
        items(students, key = { it.id }) { student ->
          ElevatedCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors =
                  CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
          ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                  student.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Medium,
              )
              Text(student.email, style = MaterialTheme.typography.bodySmall)
              Text(
                  stringResource(R.string.label_progress, (student.progress * 100).toInt()),
                  style = MaterialTheme.typography.bodySmall,
              )
              Text(
                  stringResource(R.string.status_label, studentStatusLabel(student.status)),
                  style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Executa a rotina de class status label dentro do contexto deste componente.
 *
 * @param status Valor de entrada utilizado por esta operação.
 * @return Resultado produzido pela operação em formato `String`.
 */
@Composable
private fun classStatusLabel(status: ClassStatus): String =
    when (status) {
      ClassStatus.Open -> stringResource(R.string.class_status_open)
      ClassStatus.InProgress -> stringResource(R.string.class_status_in_progress)
      ClassStatus.Closed -> stringResource(R.string.class_status_closed)
    }

/**
 * Executa a rotina de student status label dentro do contexto deste componente.
 *
 * @param status Valor de entrada utilizado por esta operação.
 * @return Resultado produzido pela operação em formato `String`.
 */
@Composable
private fun studentStatusLabel(status: StudentStatus): String =
    when (status) {
      StudentStatus.Active -> stringResource(R.string.status_active)
      StudentStatus.Inactive -> stringResource(R.string.status_inactive)
      StudentStatus.Graduated -> stringResource(R.string.status_graduated)
    }
