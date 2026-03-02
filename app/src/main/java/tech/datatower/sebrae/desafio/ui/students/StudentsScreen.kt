package tech.datatower.sebrae.desafio.ui.students

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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(onBack: () -> Unit = {}) {
    val allStudents = remember { sampleStudents() }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) allStudents
        else allStudents.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.course.contains(query, ignoreCase = true) ||
            it.enrolledClass.contains(query, ignoreCase = true)
        }
    }

    DetailScaffold(title = "Alunos", onBack = onBack) { innerPadding, _ ->
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Novo aluno")
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
                        placeholder = { Text("Buscar aluno, curso ou turma…") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
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
                        text = "${filtered.size} alunos encontrados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }

                if (filtered.isEmpty()) {
                    item { EmptyState(icon = Icons.Outlined.Person, message = "Nenhum aluno encontrado.") }
                } else {
                    items(filtered) { student ->
                        StudentCard(student = student)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCard(student: Student) {
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
            // Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = student.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StatusChip(
                        label = when (student.status) {
                            StudentStatus.Active    -> "Ativo"
                            StudentStatus.Inactive  -> "Inativo"
                            StudentStatus.Graduated -> "Formado"
                        },
                        containerColor = when (student.status) {
                            StudentStatus.Active    -> MaterialTheme.colorScheme.primaryContainer
                            StudentStatus.Inactive  -> MaterialTheme.colorScheme.errorContainer
                            StudentStatus.Graduated -> MaterialTheme.colorScheme.tertiaryContainer
                        },
                        contentColor = when (student.status) {
                            StudentStatus.Active    -> MaterialTheme.colorScheme.onPrimaryContainer
                            StudentStatus.Inactive  -> MaterialTheme.colorScheme.onErrorContainer
                            StudentStatus.Graduated -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    )
                }
                Text(
                    text = "${student.course} · ${student.enrolledClass}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinearProgressIndicator(
                        progress = { student.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(student.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun sampleStudents() = listOf(
    Student(1, "Ana Lima",         "ana.lima@email.com",    "Marketing Digital",   "Turma A1", 0.87f, StudentStatus.Active),
    Student(2, "Carlos Souza",     "carlos@email.com",      "Excel para Negócios", "Turma B3", 0.42f, StudentStatus.Active),
    Student(3, "Fernanda Costa",   "fernanda@email.com",    "Empreendedorismo",    "Turma C2", 0.95f, StudentStatus.Graduated),
    Student(4, "Ricardo Alves",    "ricardo@email.com",     "Finanças Pessoais",   "Turma A2", 0.20f, StudentStatus.Inactive),
    Student(5, "Mariana Pereira",  "mariana@email.com",     "Marketing Digital",   "Turma A1", 0.65f, StudentStatus.Active),
    Student(6, "Lucas Oliveira",   "lucas@email.com",       "Excel para Negócios", "Turma B3", 0.78f, StudentStatus.Active),
    Student(7, "Juliana Santos",   "juliana@email.com",     "Empreendedorismo",    "Turma C2", 1.0f,  StudentStatus.Graduated),
    Student(8, "Pedro Rodrigues",  "pedro@email.com",       "Finanças Pessoais",   "Turma A2", 0.10f, StudentStatus.Active),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudentsPreview() {
    AppDesafioSEBRAETheme { StudentsScreen() }
}

