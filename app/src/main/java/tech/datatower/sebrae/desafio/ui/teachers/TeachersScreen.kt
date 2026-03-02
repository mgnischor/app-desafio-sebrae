package tech.datatower.sebrae.desafio.ui.teachers

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(onBack: () -> Unit = {}) {
    val allTeachers = remember { sampleTeachers() }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) allTeachers
        else allTeachers.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.specialty.contains(query, ignoreCase = true)
        }
    }

    DetailScaffold(title = "Instrutores", onBack = onBack) { innerPadding, _ ->
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Novo instrutor")
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { fabPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(fabPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar instrutor ou especialidade…") },
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
                        text = "${filtered.size} instrutores encontrados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }

                if (filtered.isEmpty()) {
                    item { EmptyState(icon = Icons.Outlined.School, message = "Nenhum instrutor encontrado.") }
                } else {
                    items(filtered) { teacher -> TeacherCard(teacher) }
                }
            }
        }
    }
}

@Composable
private fun TeacherCard(teacher: Teacher) {
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = teacher.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = teacher.specialty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "${teacher.activeCourses} cursos ativos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${teacher.totalStudents} alunos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", teacher.rating),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

private fun sampleTeachers() = listOf(
    Teacher(1, "Profa. Helena Martins", "helena@inst.com", "Marketing & Comunicação", 3, 320, 4.9f),
    Teacher(2, "Prof. André Nunes",     "andre@inst.com",  "Tecnologia da Informação", 2, 210, 4.7f),
    Teacher(3, "Profa. Carla Faria",    "carla@inst.com",  "Empreendedorismo & Gestão", 2, 180, 4.8f),
    Teacher(4, "Prof. Roberto Lima",    "roberto@inst.com","Finanças & Contabilidade", 1, 95,  4.5f),
    Teacher(5, "Profa. Bianca Torres",  "bianca@inst.com", "Design & Criatividade",   1, 140, 4.6f),
    Teacher(6, "Prof. Sérgio Campos",   "sergio@inst.com", "Vendas & Negociação",     1, 260, 4.4f),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TeachersPreview() {
    AppDesafioSEBRAETheme { TeachersScreen() }
}

