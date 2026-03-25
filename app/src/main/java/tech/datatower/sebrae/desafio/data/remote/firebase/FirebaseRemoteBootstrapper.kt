package tech.datatower.sebrae.desafio.data.remote.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.CourseEntity
import tech.datatower.sebrae.desafio.data.local.SchoolClassEntity
import tech.datatower.sebrae.desafio.data.local.StudentEntity
import tech.datatower.sebrae.desafio.data.local.TeacherEntity
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.remote.NoOpRemoteBootstrapper
import tech.datatower.sebrae.desafio.data.remote.RemoteBootstrapper

/**
 * Initial Firebase adapter used to hydrate Room from Firestore collections.
 *
 * This keeps the app local-first while enabling gradual migration to Firebase.
 */
class FirebaseRemoteBootstrapper(
    private val firestore: FirebaseFirestore,
    private val dao: AppDao,
) : RemoteBootstrapper {

  override suspend fun bootstrapIntoLocalCache(): Boolean {
    return runCatching {
          val courses = fetchCourses()
          val classes = fetchClasses()
          val teachers = fetchTeachers()
          val students = fetchStudents()

          if (courses.isNotEmpty()) dao.insertCourses(courses)
          if (classes.isNotEmpty()) dao.insertClasses(classes)
          if (teachers.isNotEmpty()) dao.insertTeachers(teachers)
          if (students.isNotEmpty()) dao.insertStudents(students)

          courses.isNotEmpty() || classes.isNotEmpty() || teachers.isNotEmpty() || students.isNotEmpty()
        }
        .getOrDefault(false)
  }

  private suspend fun fetchCourses(): List<CourseEntity> =
      firestore
          .collection(COLLECTION_COURSES)
          .get()
          .await()
          .documents
          .mapNotNull { it.toCourseEntity() }

  private suspend fun fetchClasses(): List<SchoolClassEntity> =
      firestore
          .collection(COLLECTION_CLASSES)
          .get()
          .await()
          .documents
          .mapNotNull { it.toClassEntity() }

  private suspend fun fetchTeachers(): List<TeacherEntity> =
      firestore
          .collection(COLLECTION_TEACHERS)
          .get()
          .await()
          .documents
          .mapNotNull { it.toTeacherEntity() }

  private suspend fun fetchStudents(): List<StudentEntity> =
      firestore
          .collection(COLLECTION_STUDENTS)
          .get()
          .await()
          .documents
          .mapNotNull { it.toStudentEntity() }

  private fun DocumentSnapshot.toCourseEntity(): CourseEntity? {
    val id = int("id") ?: id.toIntOrNull() ?: return null
    return CourseEntity(
        id = id,
        title = string("title"),
        category = string("category"),
        instructor = string("instructor"),
        totalStudents = int("totalStudents") ?: 0,
        durationHours = int("durationHours") ?: 0,
        completionRate = float("completionRate") ?: 0f,
        isPublished = boolean("isPublished") ?: false,
    )
  }

  private fun DocumentSnapshot.toClassEntity(): SchoolClassEntity? {
    val id = int("id") ?: id.toIntOrNull() ?: return null
    return SchoolClassEntity(
        id = id,
        name = string("name"),
        course = string("course"),
        instructor = string("instructor"),
        studentsCount = int("studentsCount") ?: 0,
        maxCapacity = int("maxCapacity") ?: 0,
        schedule = string("schedule"),
        status = enumOrDefault(string("status"), ClassStatus.InProgress),
    )
  }

  private fun DocumentSnapshot.toTeacherEntity(): TeacherEntity? {
    val id = int("id") ?: id.toIntOrNull() ?: return null
    return TeacherEntity(
        id = id,
        name = string("name"),
        email = string("email"),
        specialty = string("specialty"),
        activeCourses = int("activeCourses") ?: 0,
        totalStudents = int("totalStudents") ?: 0,
        rating = float("rating") ?: 0f,
    )
  }

  private fun DocumentSnapshot.toStudentEntity(): StudentEntity? {
    val id = int("id") ?: id.toIntOrNull() ?: return null
    return StudentEntity(
        id = id,
        name = string("name"),
        email = string("email"),
        course = string("course"),
        enrolledClass = string("enrolledClass"),
        progress = float("progress") ?: 0f,
        status = enumOrDefault(string("status"), StudentStatus.Active),
    )
  }

  private fun DocumentSnapshot.string(field: String): String = getString(field).orEmpty()

  private fun DocumentSnapshot.int(field: String): Int? = getLong(field)?.toInt()

  private fun DocumentSnapshot.float(field: String): Float? = getDouble(field)?.toFloat()

  private fun DocumentSnapshot.boolean(field: String): Boolean? = getBoolean(field)

  private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
    if (value.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default
  }

  companion object {
    private const val COLLECTION_COURSES = "courses"
    private const val COLLECTION_CLASSES = "classes"
    private const val COLLECTION_TEACHERS = "teachers"
    private const val COLLECTION_STUDENTS = "students"

    fun create(context: Context, dao: AppDao): RemoteBootstrapper {
      if (FirebaseApp.getApps(context).isEmpty()) return NoOpRemoteBootstrapper
      val firestore = FirebaseFirestore.getInstance()
      return FirebaseRemoteBootstrapper(firestore = firestore, dao = dao)
    }
  }
}

