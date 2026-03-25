package tech.datatower.sebrae.desafio.data.remote.firebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.Teacher

/**
 * ViewModel para gerenciar sincronização com Firebase Data Connect.
 *
 * Fornece estados de carregamento, sucesso e erro para a camada UI. Executa operações em background
 * usando coroutines.
 */
class DataConnectSyncViewModel(
    private val service: FirebaseDataConnectService,
) : ViewModel() {

  // ── Estados de Sincronização ──────────────────────────────────────

  private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
  val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

  private val _coursesState: MutableStateFlow<DataState<List<Course>>> =
      MutableStateFlow(DataState.Idle)
  val coursesState: StateFlow<DataState<List<Course>>> = _coursesState.asStateFlow()

  private val _studentsState: MutableStateFlow<DataState<List<Student>>> =
      MutableStateFlow(DataState.Idle)
  val studentsState: StateFlow<DataState<List<Student>>> = _studentsState.asStateFlow()

  private val _teachersState: MutableStateFlow<DataState<List<Teacher>>> =
      MutableStateFlow(DataState.Idle)
  val teachersState: StateFlow<DataState<List<Teacher>>> = _teachersState.asStateFlow()

  /** Sincroniza todos os dados do Data Connect. */
  fun syncAllData() {
    viewModelScope.launch {
      _syncState.value = SyncState.Loading
      val result = service.syncAllData()
      _syncState.value =
          when (result) {
            is FirebaseDataConnectService.Result.Success -> SyncState.Success
            is FirebaseDataConnectService.Result.Error -> SyncState.Error(result.message)
            is FirebaseDataConnectService.Result.Loading -> SyncState.Loading
          }
    }
  }

  /** Busca apenas cursos do Data Connect. */
  fun loadCourses() {
    viewModelScope.launch {
      _coursesState.value = DataState.Loading
      val result = service.fetchCourses()
      _coursesState.value =
          when (result) {
            is FirebaseDataConnectService.Result.Success -> DataState.Success(result.data)
            is FirebaseDataConnectService.Result.Error -> DataState.Error(result.message)
            is FirebaseDataConnectService.Result.Loading -> DataState.Loading
          }
    }
  }

  /** Busca apenas estudantes do Data Connect. */
  fun loadStudents() {
    viewModelScope.launch {
      _studentsState.value = DataState.Loading
      val result = service.fetchStudents()
      _studentsState.value =
          when (result) {
            is FirebaseDataConnectService.Result.Success -> DataState.Success(result.data)
            is FirebaseDataConnectService.Result.Error -> DataState.Error(result.message)
            is FirebaseDataConnectService.Result.Loading -> DataState.Loading
          }
    }
  }

  /** Busca apenas professores do Data Connect. */
  fun loadTeachers() {
    viewModelScope.launch {
      _teachersState.value = DataState.Loading
      val result = service.fetchTeachers()
      _teachersState.value =
          when (result) {
            is FirebaseDataConnectService.Result.Success -> DataState.Success(result.data)
            is FirebaseDataConnectService.Result.Error -> DataState.Error(result.message)
            is FirebaseDataConnectService.Result.Loading -> DataState.Loading
          }
    }
  }

  /** Estados de sincronização geral. */
  sealed class SyncState {
    data object Idle : SyncState()

    data object Loading : SyncState()

    data object Success : SyncState()

    /** Modelo e comportamento relacionados a error. */
    data class Error(val message: String) : SyncState()
  }

  /** Estados genéricos para dados específicos. */
  sealed class DataState<out T> {
    data object Idle : DataState<Nothing>()

    data object Loading : DataState<Nothing>()

    /** Modelo e comportamento relacionados a success. */
    data class Success<T>(val data: T) : DataState<T>()

    /** Modelo e comportamento relacionados a error. */
    data class Error(val message: String) : DataState<Nothing>()
  }
}
