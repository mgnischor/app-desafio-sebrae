package tech.datatower.sebrae.desafio.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppDatabase
import tech.datatower.sebrae.desafio.data.local.RecentActivityEntity

/** Testes focados nas consultas paginadas de atividades recentes para melhor desempenho. */
class AppRepositoryRecentActivitiesTest {

  @Test
  fun `observeRecentActivitiesPaged deve mapear apenas a pagina solicitada`() = runTest {
    val dao = mock<AppDao>()
    val database = mock<AppDatabase>()
    whenever(dao.observeRecentActivitiesPaged(limit = 10, offset = 20))
        .thenReturn(
            flowOf(
                listOf(
                    RecentActivityEntity(
                        id = 21,
                        title = "Curso atualizado",
                        subtitle = "Turma A",
                        iconKey = "course",
                        timeLabel = "Agora",
                    ),
                    RecentActivityEntity(
                        id = 22,
                        title = "Novo aluno",
                        subtitle = "João Silva",
                        iconKey = "person",
                        timeLabel = "1 min",
                    ),
                )
            )
        )

    val repository = AppRepository(database = database, dao = dao)
    val result = repository.observeRecentActivitiesPaged(limit = 10, offset = 20).first()

    assertEquals(2, result.size)
    assertEquals(21, result[0].id)
    assertEquals("Curso atualizado", result[0].title)
    assertEquals(Icons.AutoMirrored.Outlined.MenuBook, result[0].icon)
    assertEquals(22, result[1].id)
    assertEquals(Icons.Outlined.Person, result[1].icon)
    verify(dao).observeRecentActivitiesPaged(limit = 10, offset = 20)
  }

  @Test
  fun `observeRecentActivitiesCount deve refletir total reativo do DAO`() = runTest {
    val dao = mock<AppDao>()
    val database = mock<AppDatabase>()
    whenever(dao.observeRecentActivitiesCount()).thenReturn(flowOf(137))

    val repository = AppRepository(database = database, dao = dao)
    val total = repository.observeRecentActivitiesCount().first()

    assertEquals(137, total)
    verify(dao).observeRecentActivitiesCount()
  }
}
