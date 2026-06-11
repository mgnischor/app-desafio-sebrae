/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/repository/AppRepositoryRecentActivitiesTest.kt
    Descrição: Testes focados nas consultas paginadas de atividades recentes para melhor desempenho.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
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
    whenever(dao.observeRecentActivitiesPaged(companyId = 1, limit = 10, offset = 20))
        .thenReturn(
            flowOf(
                listOf(
                    RecentActivityEntity(
                        companyId = 1,
                        id = 21,
                        title = "Curso atualizado",
                        subtitle = "Turma A",
                        iconKey = "course",
                        timeLabel = "Agora",
                    ),
                    RecentActivityEntity(
                        companyId = 1,
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
    val result =
        repository.observeRecentActivitiesPaged(companyId = 1, limit = 10, offset = 20).first()

    assertEquals(2, result.size)
    assertEquals(21, result[0].id)
    assertEquals("Curso atualizado", result[0].title)
    assertEquals(Icons.AutoMirrored.Outlined.MenuBook, result[0].icon)
    assertEquals(22, result[1].id)
    assertEquals(Icons.Outlined.Person, result[1].icon)
    verify(dao).observeRecentActivitiesPaged(companyId = 1, limit = 10, offset = 20)
  }

  @Test
  fun `observeRecentActivitiesCount deve refletir total reativo do DAO`() = runTest {
    val dao = mock<AppDao>()
    val database = mock<AppDatabase>()
    whenever(dao.observeRecentActivitiesCount(companyId = 1)).thenReturn(flowOf(137))

    val repository = AppRepository(database = database, dao = dao)
    val total = repository.observeRecentActivitiesCount(companyId = 1).first()

    assertEquals(137, total)
    verify(dao).observeRecentActivitiesCount(companyId = 1)
  }
}
