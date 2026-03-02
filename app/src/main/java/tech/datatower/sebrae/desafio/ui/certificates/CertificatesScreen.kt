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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.Certificate
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatesScreen(onBack: () -> Unit = {}) {
    val allCerts = remember { sampleCertificates() }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) allCerts
        else allCerts.filter {
            it.studentName.contains(query, ignoreCase = true) ||
            it.courseName.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }
    }

    DetailScaffold(title = "Certificados", onBack = onBack) { innerPadding, _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por aluno, curso ou código…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${filtered.size} certificados encontrados",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }

            if (filtered.isEmpty()) {
                item { EmptyState(icon = Icons.Outlined.Bookmarks, message = "Nenhum certificado encontrado.") }
            } else {
                items(filtered) { cert -> CertificateCard(cert) }
            }
        }
    }
}

@Composable
private fun CertificateCard(cert: Certificate) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        text = "${cert.hours}h",
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

private fun sampleCertificates() = listOf(
    Certificate(1, "Ana Lima",          "Marketing Digital",    "15/02/2026", 40,  "CERT-2026-0148"),
    Certificate(2, "Fernanda Costa",    "Empreendedorismo",     "12/02/2026", 30,  "CERT-2026-0147"),
    Certificate(3, "Juliana Santos",    "Empreendedorismo",     "10/02/2026", 30,  "CERT-2026-0146"),
    Certificate(4, "Lucas Oliveira",    "Excel para Negócios",  "05/02/2026", 20,  "CERT-2026-0145"),
    Certificate(5, "Mariana Pereira",   "Design Gráfico",       "01/02/2026", 24,  "CERT-2026-0144"),
    Certificate(6, "Pedro Rodrigues",   "Finanças Pessoais",    "28/01/2026", 16,  "CERT-2026-0143"),
    Certificate(7, "Carlos Souza",      "Vendas e Negociação",  "20/01/2026", 12,  "CERT-2026-0142"),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CertificatesPreview() {
    AppDesafioSEBRAETheme { CertificatesScreen() }
}

