package tech.datatower.sebrae.desafio.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/** Tipos de dados que podem ser importados via CSV. */
enum class CsvImportType(val label: String) {
  STUDENTS("Alunos"),
  COURSES("Cursos"),
  CLASSES("Turmas"),
  TEACHERS("Instrutores"),
}

/**
 * Tela de importação de dados via arquivo CSV.
 *
 * Acessível apenas pelo Administrador a partir das Configurações.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
) {
  val viewModel: CsvImportViewModel = hiltViewModel()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedType by rememberSaveable { mutableStateOf(CsvImportType.STUDENTS.name) }
  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var selectedFileName by rememberSaveable { mutableStateOf("") }
  var isProcessing by rememberSaveable { mutableStateOf(false) }

  val filePickerLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
          selectedUri = uri
          selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "arquivo.csv"
        }
      }

  DetailScaffold(
      title = stringResource(R.string.csv_import_title),
      onBack = onBack,
  ) { innerPadding, _ ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SnackbarHost(hostState = snackbarHostState)

      if (currentUser?.role != UserRole.ADMINISTRADOR) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(),
        ) {
          Text(
              text = "Apenas administradores podem importar dados via CSV.",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
        return@Column
      }

      ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.elevatedCardColors(),
      ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
              text = stringResource(R.string.csv_import_select_type),
              style = MaterialTheme.typography.titleMedium,
          )

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CsvImportType.entries.forEach { type ->
              FilterChip(
                  selected = selectedType == type.name,
                  onClick = { selectedType = type.name },
                  label = { Text(type.label) },
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
              text = stringResource(R.string.csv_import_select_file),
              style = MaterialTheme.typography.titleMedium,
          )

          Button(
              onClick = { filePickerLauncher.launch(arrayOf("text/*", "application/csv")) },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
                Icons.Outlined.FileUpload,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                if (selectedFileName.isBlank()) stringResource(R.string.csv_import_select_file)
                else selectedFileName
            )
          }

          if (isProcessing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.csv_import_processing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
              onClick = {
                val uri = selectedUri
                if (uri == null) {
                  scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.csv_import_no_file))
                  }
                  return@Button
                }

                isProcessing = true
                val importType =
                    CsvImportType.entries.firstOrNull { it.name == selectedType }
                        ?: CsvImportType.STUDENTS

                viewModel.importCsv(
                    context = context,
                    uri = uri,
                    type = importType,
                    onSuccess = { count ->
                      isProcessing = false
                      selectedUri = null
                      selectedFileName = ""
                      scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.csv_import_success, count)
                        )
                      }
                    },
                    onError = { message ->
                      isProcessing = false
                      scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.csv_import_error, message)
                        )
                      }
                    },
                )
              },
              modifier = Modifier.fillMaxWidth(),
              enabled = selectedUri != null && !isProcessing,
          ) {
            Text(stringResource(R.string.csv_import_action))
          }
        }
      }

      ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.elevatedCardColors(),
      ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
              text = "Formato esperado do CSV",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )

          Text(
              text = "Delimitadores aceitos: vírgula (,) ou ponto-e-vírgula (;)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
          )

          val formatHint =
              when (CsvImportType.entries.firstOrNull { it.name == selectedType }) {
                CsvImportType.STUDENTS ->
                    "nome,email,curso,turma,progresso,status\n" +
                        "Ex: João Silva,joao@email.com,Gestão,Turma A,50,Active"
                CsvImportType.COURSES ->
                    "titulo,categoria,instrutor,alunos,carga_horaria,conclusao,publicado\n" +
                        "Ex: Gestão Básica,Gestão,Prof. Ana,30,40,75,true"
                CsvImportType.CLASSES ->
                    "nome,curso,instrutor,alunos,capacidade,horario,status\n" +
                        "Ex: Turma A,Gestão Básica,Prof. Ana,25,30,Seg 14h,Open"
                CsvImportType.TEACHERS ->
                    "nome,email,especialidade,cursos_ativos,total_alunos,avaliacao\n" +
                        "Ex: Prof. Ana,ana@email.com,Gestão,3,45,4.5"
                null -> "Selecione o tipo de importação."
              }

          Text(
              text = formatHint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
