/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/home/RecentActivitiesScreen.kt
    Descrição: Tela de listagem completa das atividades recentes registradas no sistema.
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
package tech.datatower.sebrae.desafio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.RealtimeNotificationRules
import tech.datatower.sebrae.desafio.data.model.RecentActivity
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/**
 * Tela com histórico completo de atividades recentes com paginação.
 *
 * Apenas perfis de backoffice (Administrador e Coordenador) possuem visualização de conteúdo.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecentActivitiesScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
) {
  val viewModel: RecentActivitiesViewModel = hiltViewModel()
  val canSeeBackofficeRecents =
      remember(currentUser?.role) {
        RealtimeNotificationRules.canReceiveBackofficeNotifications(currentUser?.role)
      }

  val allRecents by viewModel.allRecents.collectAsState()
  var pageIndex by remember { mutableIntStateOf(0) }
  val pageSize = 10
  val pagedSource = if (canSeeBackofficeRecents) allRecents else emptyList()
  val totalPages = ((pagedSource.size + pageSize - 1) / pageSize).coerceAtLeast(1)
  val safePageIndex = pageIndex.coerceIn(0, totalPages - 1)
  val fromIndex = safePageIndex * pageSize
  val toIndex = minOf(fromIndex + pageSize, pagedSource.size)
  val pageItems = if (fromIndex < toIndex) pagedSource.subList(fromIndex, toIndex) else emptyList()

  LaunchedEffect(currentUser?.id, currentUser?.role) {
    viewModel.observeRealtimeActivities(currentUser)
  }

  DetailScaffold(
      title = stringResource(R.string.recent_activities_title),
      onBack = onBack,
  ) { innerPadding, _ ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (pageItems.isEmpty()) {
        item {
          Text(
              text = stringResource(R.string.recent_empty),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        items(items = pageItems, key = { it.id }) { item -> RecentActivityCard(item = item) }
      }

      item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
              onClick = { pageIndex = (safePageIndex - 1).coerceAtLeast(0) },
              enabled = safePageIndex > 0,
          ) {
            Text(text = stringResource(R.string.pagination_previous))
          }

          Text(
              text =
                  stringResource(R.string.pagination_page_indicator, safePageIndex + 1, totalPages),
              style = MaterialTheme.typography.labelLarge,
          )

          TextButton(
              onClick = { pageIndex = (safePageIndex + 1).coerceAtMost(totalPages - 1) },
              enabled = safePageIndex < totalPages - 1,
          ) {
            Text(text = stringResource(R.string.pagination_next))
          }
        }
      }
    }
  }
}

/** Cartão de atividade usado na listagem completa de atividades recentes. */
@Composable
private fun RecentActivityCard(item: RecentActivity) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      androidx.compose.foundation.layout.Box(
          modifier =
              Modifier.size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
      ) {
        androidx.compose.material3.Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
          text = item.timeLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
      )
    }
  }
}
