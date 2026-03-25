package tech.datatower.sebrae.desafio.data.remote.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.Teacher

/**
 * Serviço de integração com Firebase Data Connect.
 *
 * Fornece operações de sincronização com o Data Connect do Firebase,
 * permitindo buscar dados remotos e sincronizar com cache local (Room).
 *
 * Cada operação retorna Result<T> para melhor tratamento de erros.
 */
class FirebaseDataConnectService(
    private val firestore: FirebaseFirestore,
    private val dao: AppDao,
) {

  /**
   * Resultado genérico para operações assíncronas.
   *
   * Success: operação bem-sucedida com dados
   * Error: falha na operação com mensagem de erro
   * Loading: operação em progresso
   */
  sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String = "") : Result<Nothing>()
    data object Loading : Result<Nothing>()
  }

  companion object {
    private const val TAG = "DataConnect"
    private const val COLLECTION_STUDENTS = "students"
    private const val COLLECTION_COURSES = "courses"
    private const val COLLECTION_TEACHERS = "teachers"
  }

  /**
   * Busca lista de cursos do Data Connect.
   *
   * Operação de Query do Data Connect:
   * - Conecta ao Firestore
   * - Busca documentos da coleção "courses"
   * - Converte para modelo Course
   * - Sincroniza com cache local
   *
   * @return Result<List<Course>> sucesso ou erro
   */
  suspend fun fetchCourses(): Result<List<Course>> {
    return try {
      Log.d(TAG, "Iniciando busca de cursos do Data Connect...")
      
      val snapshot = firestore.collection(COLLECTION_COURSES).get().await()
      val courses = snapshot.documents.mapNotNull { doc ->
        try {
          Course(
              id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
              title = doc.getString("title") ?: "",
              category = doc.getString("category") ?: "",
              instructor = doc.getString("instructor") ?: "",
              totalStudents = (doc.get("totalStudents") as? Number)?.toInt() ?: 0,
              durationHours = (doc.get("durationHours") as? Number)?.toInt() ?: 0,
              completionRate = (doc.get("completionRate") as? Number)?.toFloat() ?: 0f,
              isPublished = doc.getBoolean("isPublished") ?: false,
          )
        } catch (e: Exception) {
          Log.w(TAG, "Erro ao converter curso: ${doc.id}", e)
          null
        }
      }

      // Sincronizar com cache local
      if (courses.isNotEmpty()) {
        dao.insertCourses(courses.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${courses.size} cursos para cache local")
      }

      Result.Success(courses)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar cursos do Data Connect", e)
      Result.Error(e, "Falha ao buscar cursos: ${e.message}")
    }
  }

  /**
   * Busca lista de estudantes do Data Connect.
   */
  suspend fun fetchStudents(): Result<List<Student>> {
    return try {
      Log.d(TAG, "Iniciando busca de estudantes do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_STUDENTS).get().await()
      val students = snapshot.documents.mapNotNull { doc ->
        try {
          Student(
              id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
              name = doc.getString("name") ?: "",
              email = doc.getString("email") ?: "",
              course = doc.getString("course") ?: "",
              enrolledClass = doc.getString("enrolledClass") ?: "",
              progress = (doc.get("progress") as? Number)?.toFloat() ?: 0f,
              status = tech.datatower.sebrae.desafio.data.model.StudentStatus.Active,
          )
        } catch (e: Exception) {
          Log.w(TAG, "Erro ao converter estudante: ${doc.id}", e)
          null
        }
      }

      // Sincronizar com cache local
      if (students.isNotEmpty()) {
        dao.insertStudents(students.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${students.size} estudantes para cache local")
      }

      Result.Success(students)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar estudantes do Data Connect", e)
      Result.Error(e, "Falha ao buscar estudantes: ${e.message}")
    }
  }

  /**
   * Busca lista de professores do Data Connect.
   */
  suspend fun fetchTeachers(): Result<List<Teacher>> {
    return try {
      Log.d(TAG, "Iniciando busca de professores do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_TEACHERS).get().await()
      val teachers = snapshot.documents.mapNotNull { doc ->
        try {
          Teacher(
              id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
              name = doc.getString("name") ?: "",
              email = doc.getString("email") ?: "",
              specialty = doc.getString("specialty") ?: "",
              activeCourses = (doc.get("activeCourses") as? Number)?.toInt() ?: 0,
              totalStudents = (doc.get("totalStudents") as? Number)?.toInt() ?: 0,
              rating = (doc.get("rating") as? Number)?.toFloat() ?: 0f,
          )
        } catch (e: Exception) {
          Log.w(TAG, "Erro ao converter professor: ${doc.id}", e)
          null
        }
      }

      // Sincronizar com cache local
      if (teachers.isNotEmpty()) {
        dao.insertTeachers(teachers.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${teachers.size} professores para cache local")
      }

      Result.Success(teachers)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar professores do Data Connect", e)
      Result.Error(e, "Falha ao buscar professores: ${e.message}")
    }
  }

  /**
   * Sincroniza todos os dados do Data Connect com cache local.
   *
   * Executa em paralelo para melhor performance.
   */
  suspend fun syncAllData(): Result<Boolean> {
    return try {
      Log.d(TAG, "Iniciando sincronização completa de dados...")

      val coursesResult = fetchCourses()
      val studentsResult = fetchStudents()
      val teachersResult = fetchTeachers()

      val allSuccess =
          coursesResult is Result.Success &&
              studentsResult is Result.Success &&
              teachersResult is Result.Success

      if (allSuccess) {
        Log.d(TAG, "Sincronização completa concluída com sucesso")
        Result.Success(true)
      } else {
        val errors = buildString {
          if (coursesResult is Result.Error) append("Cursos: ${coursesResult.message}. ")
          if (studentsResult is Result.Error) append("Estudantes: ${studentsResult.message}. ")
          if (teachersResult is Result.Error) append("Professores: ${teachersResult.message}. ")
        }
        Result.Error(Exception("Falha parcial"), errors)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Erro durante sincronização completa", e)
      Result.Error(e, "Falha na sincronização: ${e.message}")
    }
  }

  /**
   * Verifica conexão com Firebase.
   */
  suspend fun checkConnection(): Result<Boolean> {
    return try {
      firestore.collection("_metadata").limit(1).get().await()
      Log.d(TAG, "Conexão com Firebase validada")
      Result.Success(true)
    } catch (e: Exception) {
      Log.w(TAG, "Falha na conexão com Firebase", e)
      Result.Error(e, "Sem conexão com Firebase: ${e.message}")
    }
  }

  // Conversões de modelo para entidade (para cache local)
  private fun Course.toEntity() =
      tech.datatower.sebrae.desafio.data.local.CourseEntity(
          id = id,
          title = title,
          category = category,
          instructor = instructor,
          totalStudents = totalStudents,
          durationHours = durationHours,
          completionRate = completionRate,
          isPublished = isPublished,
      )

  private fun Student.toEntity() =
      tech.datatower.sebrae.desafio.data.local.StudentEntity(
          id = id,
          name = name,
          email = email,
          course = course,
          enrolledClass = enrolledClass,
          progress = progress,
          status = status,
      )

  private fun Teacher.toEntity() =
      tech.datatower.sebrae.desafio.data.local.TeacherEntity(
          id = id,
          name = name,
          email = email,
          specialty = specialty,
          activeCourses = activeCourses,
          totalStudents = totalStudents,
          rating = rating,
      )
}

