package tech.datatower.sebrae.desafio.ui.companies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.Company
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompanySelectorScreen(
    onBack: () -> Unit,
    onCompanySelected: () -> Unit = {},
) {
  val userCompanies by AuthManager.userCompanies.collectAsState()
  val currentCompany by AuthManager.currentCompany.collectAsState()

  DetailScaffold(
      title = "Selecionar Empresa",
      onBack = onBack,
  ) { innerPadding, _ ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
          text = "Selecione a empresa em que deseja trabalhar:",
          style = MaterialTheme.typography.bodyMedium,
      )

      if (userCompanies.isEmpty()) {
        Text(
            text = "Nenhuma empresa disponível. Solicite acesso a um administrador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Column
      }

      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(userCompanies, key = { it.id }) { company ->
          val isSelected = currentCompany?.id == company.id
          CompanySelectorItem(
              company = company,
              isSelected = isSelected,
              onClick = {
                AuthManager.switchCompany(company)
                onCompanySelected()
              },
          )
        }
      }
    }
  }
}

@Composable
private fun CompanySelectorItem(
    company: Company,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = company.name, style = MaterialTheme.typography.bodyLarge)
        if (company.cnpj.isNotBlank()) {
          Text(text = company.cnpj, style = MaterialTheme.typography.bodySmall)
        }
      }
      if (isSelected) {
        Icon(
            Icons.Filled.Check,
            contentDescription = "Selecionada",
            tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
