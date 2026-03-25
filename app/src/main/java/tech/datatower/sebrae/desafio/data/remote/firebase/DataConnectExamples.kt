package tech.datatower.sebrae.desafio.data.remote.firebase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.Course

/**
 * Exemplo de como usar FirebaseDataConnectService com Composable.
 *
 * Demonstra padrões de:
 * - Inicializar ViewModel
 * - Coletar estados
 * - Renderizar UI baseada em estados
 * - Sincronizar com LaunchedEffect
 */

/**
 * Exemplo 1: Tela de Cursos com sincronização automática.
 */
@Composable
fun CursosScreenExample(
    viewModel: DataConnectSyncViewModel,
) {
  val coursesState by viewModel.coursesState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.loadCourses()
  }

  Scaffold { innerPadding ->
    Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
      when (val state = coursesState) {
        is DataConnectSyncViewModel.DataState.Loading -> {
          CircularProgressIndicator()
        }
        is DataConnectSyncViewModel.DataState.Success -> {
          LazyColumn {
            items(state.data) { curso ->
              CursoCard(curso)
            }
          }
        }
        is DataConnectSyncViewModel.DataState.Error -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Erro ao carregar cursos", color = MaterialTheme.colorScheme.error)
            Text(state.message, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { viewModel.loadCourses() }) {
              Text("Tentar Novamente")
            }
          }
        }
        is DataConnectSyncViewModel.DataState.Idle -> {
          Button(onClick = { viewModel.loadCourses() }) {
            Text("Carregar Cursos")
          }
        }
      }
    }
  }
}

/**
 * Exemplo 2: Tela de Sincronização Geral.
 */
@Composable
fun SyncStatusScreen(
    viewModel: DataConnectSyncViewModel,
) {
  val syncState by viewModel.syncState.collectAsState()

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Status de Sincronização", style = MaterialTheme.typography.headlineMedium)

    when (syncState) {
      is DataConnectSyncViewModel.SyncState.Idle -> {
        Button(onClick = { viewModel.syncAllData() }) {
          Text("Iniciar Sincronização")
        }
      }
      is DataConnectSyncViewModel.SyncState.Loading -> {
        CircularProgressIndicator()
        Text("Sincronizando...")
      }
      is DataConnectSyncViewModel.SyncState.Success -> {
        Text("✅ Sincronização concluída com sucesso!", color = MaterialTheme.colorScheme.primary)
        Button(onClick = { viewModel.syncAllData() }) {
          Text("Sincronizar Novamente")
        }
      }
      is DataConnectSyncViewModel.SyncState.Error -> {
        val error = syncState as DataConnectSyncViewModel.SyncState.Error
        Text("❌ Erro na sincronização", color = MaterialTheme.colorScheme.error)
        Text(error.message, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { viewModel.syncAllData() }) {
          Text("Tentar Novamente")
        }
      }
    }
  }
}

/**
 * Exemplo 3: Card de Curso para exibição em lista.
 */
@Composable
private fun CursoCard(curso: Course) {
  Surface(
      modifier =
          Modifier.fillMaxSize()
              .padding(8.dp), // Simples - usar em LazyColumn
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(curso.title, style = MaterialTheme.typography.headlineSmall)
      Text(curso.category, style = MaterialTheme.typography.bodySmall)
      Text("Instrutor: ${curso.instructor}", style = MaterialTheme.typography.bodySmall)
      Text(
          "Taxa: ${(curso.completionRate * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

/**
 * Exemplo de como integrar com AppGraph no onCreate do Activity.
 *
 * Adicione esta lógica quando precisar sincronizar na inicialização:
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *       super.onCreate(savedInstanceState)
 *       enableEdgeToEdge()
 *       
 *       // Sincronizar dados do Firebase na inicialização
 *       lifecycleScope.launch {
 *           val repository = AppGraph.repository(this@MainActivity)
 *           val service = AppGraph.dataConnectService(repository.dao())
 *           val result = service.syncAllData()
 *           when (result) {
 *               is FirebaseDataConnectService.Result.Success -> {
 *                   Log.d("DataConnect", "Sincronização bem-sucedida!")
 *               }
 *               is FirebaseDataConnectService.Result.Error -> {
 *                   Log.e("DataConnect", "Erro: ${result.message}")
 *               }
 *               else -> {}
 *           }
 *       }
 *       
 *       setContent { /* ... */ }
 *   }
 */

/**
 * Factory para criar ViewModel com dependências.
 *
 * Necessário se não usando Hilt/DI automático.
 */
// TODO: Implementar quando DI estiver configurado

/**
 * Padrão de Erro Tratado (completo).
 *
 * Composable que mostra diferentes estados com UI apropriada.
 */
@Composable
fun RobustDataConnectScreen(
    viewModel: DataConnectSyncViewModel,
) {
  val coursesState by viewModel.coursesState.collectAsState()
  val studentsState by viewModel.studentsState.collectAsState()
  val teachersState by viewModel.teachersState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.loadCourses()
    viewModel.loadStudents()
    viewModel.loadTeachers()
  }

  Scaffold { innerPadding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
    ) {
      Text("Dados do Firebase Data Connect", style = MaterialTheme.typography.headlineMedium)

      // Seção de Cursos
      Text("Cursos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
      RenderDataState(coursesState) { cursos ->
        LazyColumn {
          items(cursos) { Text(it.title) }
        }
      }

      // Seção de Estudantes
      Text("Estudantes", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
      RenderDataState(studentsState) { estudantes ->
        LazyColumn {
          items(estudantes) { Text(it.name) }
        }
      }

      // Seção de Professores
      Text("Professores", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
      RenderDataState(teachersState) { professores ->
        LazyColumn {
          items(professores) { Text(it.name) }
        }
      }
    }
  }
}

/**
 * Composable auxiliar para renderizar qualquer DataState<T>.
 */
@Composable
private inline fun <T> RenderDataState(
    state: DataConnectSyncViewModel.DataState<T>,
    crossinline content: @Composable (T) -> Unit,
) {
  when (state) {
    is DataConnectSyncViewModel.DataState.Loading -> {
      CircularProgressIndicator()
    }
    is DataConnectSyncViewModel.DataState.Success -> {
      content(state.data)
    }
    is DataConnectSyncViewModel.DataState.Error -> {
      Text("Erro: ${state.message}", color = MaterialTheme.colorScheme.error)
    }
    is DataConnectSyncViewModel.DataState.Idle -> {
      Text("Clique para carregar dados...")
    }
  }
}

