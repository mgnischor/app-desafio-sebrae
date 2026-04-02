package tech.datatower.sebrae.desafio.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.local.CourseEntity
import tech.datatower.sebrae.desafio.data.local.SchoolClassEntity
import tech.datatower.sebrae.desafio.data.local.StudentEntity
import tech.datatower.sebrae.desafio.data.local.TeacherEntity
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class CsvImportViewModel
@Inject
constructor(
    private val repository: AppRepository,
) : ViewModel() {

  fun importCsv(
      context: Context,
      uri: Uri,
      type: CsvImportType,
      onSuccess: (Int) -> Unit,
      onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      try {
        val lines = withContext(Dispatchers.IO) { readCsvLines(context, uri) }
        if (lines.isEmpty()) {
          onError("Arquivo vazio.")
          return@launch
        }

        val companyId = AuthManager.currentCompany.value?.id ?: 1
        val dataLines = if (looksLikeHeader(lines.first(), type)) lines.drop(1) else lines
        if (dataLines.isEmpty()) {
          onError("Nenhum registro encontrado após o cabeçalho.")
          return@launch
        }

        val count =
            when (type) {
              CsvImportType.STUDENTS -> importStudents(companyId, dataLines)
              CsvImportType.COURSES -> importCourses(companyId, dataLines)
              CsvImportType.CLASSES -> importClasses(companyId, dataLines)
              CsvImportType.TEACHERS -> importTeachers(companyId, dataLines)
            }

        onSuccess(count)
      } catch (e: Exception) {
        onError(e.message ?: "Erro desconhecido.")
      }
    }
  }

  private fun readCsvLines(context: Context, uri: Uri): List<String> {
    val inputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Não foi possível abrir o arquivo.")
    return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
      reader.readLines().filter { it.isNotBlank() }
    }
  }

  private fun looksLikeHeader(line: String, type: CsvImportType): Boolean {
    val lower = line.lowercase()
    return when (type) {
      CsvImportType.STUDENTS -> lower.contains("nome") && lower.contains("email")
      CsvImportType.COURSES -> lower.contains("titulo") || lower.contains("categoria")
      CsvImportType.CLASSES -> lower.contains("nome") && lower.contains("curso")
      CsvImportType.TEACHERS -> lower.contains("nome") && lower.contains("especialidade")
    }
  }

  private fun parseLine(line: String): List<String> = line.split(",").map { it.trim() }

  private suspend fun importStudents(companyId: Int, lines: List<String>): Int {
    val baseId = System.currentTimeMillis().toInt().let { if (it < 0) -it else it } % 100_000
    val entities = lines.mapIndexedNotNull { index, line ->
      val parts = parseLine(line)
      if (parts.size < 4) return@mapIndexedNotNull null
      StudentEntity(
          id = baseId + index,
          companyId = companyId,
          name = parts[0],
          email = parts.getOrElse(1) { "" },
          course = parts.getOrElse(2) { "" },
          enrolledClass = parts.getOrElse(3) { "" },
          progress = parts.getOrElse(4) { "0" }.toFloatOrNull()?.div(100f) ?: 0f,
          status =
              runCatching { StudentStatus.valueOf(parts.getOrElse(5) { "Active" }) }
                  .getOrDefault(StudentStatus.Active),
      )
    }
    repository.importStudents(entities)
    return entities.size
  }

  private suspend fun importCourses(companyId: Int, lines: List<String>): Int {
    val baseId = System.currentTimeMillis().toInt().let { if (it < 0) -it else it } % 100_000
    val entities = lines.mapIndexedNotNull { index, line ->
      val parts = parseLine(line)
      if (parts.size < 3) return@mapIndexedNotNull null
      CourseEntity(
          id = baseId + index,
          companyId = companyId,
          title = parts[0],
          category = parts.getOrElse(1) { "" },
          instructor = parts.getOrElse(2) { "" },
          totalStudents = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0,
          durationHours = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0,
          completionRate = parts.getOrElse(5) { "0" }.toFloatOrNull()?.div(100f) ?: 0f,
          isPublished = parts.getOrElse(6) { "true" }.toBooleanStrictOrNull() ?: true,
      )
    }
    repository.importCourses(entities)
    return entities.size
  }

  private suspend fun importClasses(companyId: Int, lines: List<String>): Int {
    val baseId = System.currentTimeMillis().toInt().let { if (it < 0) -it else it } % 100_000
    val entities = lines.mapIndexedNotNull { index, line ->
      val parts = parseLine(line)
      if (parts.size < 3) return@mapIndexedNotNull null
      SchoolClassEntity(
          id = baseId + index,
          companyId = companyId,
          name = parts[0],
          course = parts.getOrElse(1) { "" },
          instructor = parts.getOrElse(2) { "" },
          studentsCount = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0,
          maxCapacity = parts.getOrElse(4) { "30" }.toIntOrNull() ?: 30,
          schedule = parts.getOrElse(5) { "" },
          status =
              runCatching { ClassStatus.valueOf(parts.getOrElse(6) { "Open" }) }
                  .getOrDefault(ClassStatus.Open),
      )
    }
    repository.importClasses(entities)
    return entities.size
  }

  private suspend fun importTeachers(companyId: Int, lines: List<String>): Int {
    val baseId = System.currentTimeMillis().toInt().let { if (it < 0) -it else it } % 100_000
    val entities = lines.mapIndexedNotNull { index, line ->
      val parts = parseLine(line)
      if (parts.size < 3) return@mapIndexedNotNull null
      TeacherEntity(
          id = baseId + index,
          companyId = companyId,
          name = parts[0],
          email = parts.getOrElse(1) { "" },
          specialty = parts.getOrElse(2) { "" },
          activeCourses = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0,
          totalStudents = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0,
          rating = parts.getOrElse(5) { "0" }.toFloatOrNull() ?: 0f,
          isActive = true,
      )
    }
    repository.importTeachers(entities)
    return entities.size
  }
}
