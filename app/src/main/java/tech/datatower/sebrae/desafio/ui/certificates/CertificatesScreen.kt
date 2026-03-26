package tech.datatower.sebrae.desafio.ui.certificates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.Certificate
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de consulta de certificados emitidos.
 *
 * Permite filtrar por aluno, curso e código do certificado.
 *
 * @param onBack Ação de retorno para a tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatesScreen(onBack: () -> Unit = {}) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataConnectService =
      remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val allCerts by repository.observeCertificates().collectAsState(initial = emptyList())
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(Unit) { dataConnectService.syncScope(ScreenDataScope.CERTIFICATES) }

  val filtered by
      remember(query, allCerts) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allCerts
          } else {
            allCerts.filter {
              it.studentName.contains(normalizedQuery, ignoreCase = true) ||
                  it.courseName.contains(normalizedQuery, ignoreCase = true) ||
                  it.code.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  DetailScaffold(title = stringResource(R.string.certificates_title), onBack = onBack) {
      innerPadding,
      _ ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        ListSearchHeader(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.certificates_search_placeholder),
            resultCount = filtered.size,
            resultLabel = stringResource(R.string.certificates_result_label),
        )
      }

      if (filtered.isEmpty()) {
        item {
          EmptyState(
              icon = Icons.Outlined.Bookmarks,
              message = stringResource(R.string.certificates_empty_state),
          )
        }
      } else {
        items(
            items = filtered,
            key = { it.id },
            contentType = { "certificate" },
        ) { cert ->
          CertificateCard(cert)
        }
      }
    }
  }
}

/**
 * Cartão com os dados essenciais de um certificado.
 *
 * @param cert Certificado apresentado no item da lista.
 */
@Composable
private fun CertificateCard(cert: Certificate) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Outlined.WorkspacePremium,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(36.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = cert.studentName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = cert.courseName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
              text = cert.issuedDate,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
              text = "${cert.hours}${stringResource(R.string.unit_hours)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
            text = cert.code,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

/** Pré-visualização da tela de certificados. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CertificatesPreview() {
  AppDesafioSEBRAETheme { CertificatesScreen() }
}
