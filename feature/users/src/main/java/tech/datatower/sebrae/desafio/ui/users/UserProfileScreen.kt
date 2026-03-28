/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/users/UserProfileScreen.kt
    Descrição: Tela de perfil de usuário com dados de acesso e vínculos acadêmicos.
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
package tech.datatower.sebrae.desafio.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay

/**
 * Tela de perfil de um usuário específico.
 *
 * Exibe:
 * - Dados de acesso (nome, e-mail, perfil de permissão).
 * - Vínculo como instrutor, se existir registro em `teachers` com o mesmo e-mail.
 * - Vínculo como aluno, se existir registro em `students` com o mesmo e-mail.
 *
 * @param onBack Callback executado ao pressionar o botão voltar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onBack: () -> Unit) {
  val viewModel: UserProfileViewModel = hiltViewModel()
  val state by viewModel.state.collectAsState()

  Box(modifier = Modifier.fillMaxSize()) {
    DetailScaffold(
        title = stringResource(R.string.user_profile_title),
        onBack = onBack,
    ) { innerPadding, _ ->
      when (val s = state) {
        is UserProfileViewModel.ProfileState.Loading -> {
          LoadingOverlay(
              isVisible = true,
              message = stringResource(R.string.loading_syncing),
          )
        }

        is UserProfileViewModel.ProfileState.Error -> {
          Box(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
              contentAlignment = Alignment.Center,
          ) {
            Text(s.message, style = MaterialTheme.typography.bodyLarge)
          }
        }

        is UserProfileViewModel.ProfileState.Success -> {
          val data = s.data
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .padding(horizontal = 20.dp)
                      .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Spacer(Modifier.height(4.dp))

            // ── Dados de acesso ───────────────────────────────────
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(Icons.Outlined.Person, contentDescription = null)
                  Text(
                      stringResource(R.string.user_profile_section_access),
                      style = MaterialTheme.typography.titleMedium,
                  )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                ProfileInfoRow(
                    label = stringResource(R.string.user_profile_label_name),
                    value = data.user.name,
                )
                ProfileInfoRow(
                    label = stringResource(R.string.user_profile_label_email),
                    value = data.user.email,
                )
                ProfileInfoRow(
                    label = stringResource(R.string.user_profile_label_role),
                    value = roleLabel(data.user.role),
                )
              }
            }

            // ── Vínculo como Instrutor ────────────────────────────
            if (data.teacherProfile != null) {
              ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(Icons.Outlined.School, contentDescription = null)
                    Text(
                        stringResource(R.string.user_profile_section_teacher),
                        style = MaterialTheme.typography.titleMedium,
                    )
                  }
                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_specialty),
                      value = data.teacherProfile.specialty,
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_active_courses),
                      value = data.teacherProfile.activeCourses.toString(),
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_total_students),
                      value = data.teacherProfile.totalStudents.toString(),
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_rating),
                      value = "${"%.1f".format(data.teacherProfile.rating)} / 5,0",
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_teacher_status),
                      value =
                          if (data.teacherProfile.isActive) stringResource(R.string.status_active)
                          else stringResource(R.string.status_inactive),
                  )

                  if (data.classesTaught.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.user_profile_classes_taught),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    data.classesTaught.forEach { cls ->
                      Text(
                          "• ${cls.name} — ${cls.course}",
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.padding(start = 8.dp),
                      )
                    }
                  }
                }
              }
            }

            // ── Vínculo como Aluno ────────────────────────────────
            if (data.studentProfile != null) {
              ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                    )
                    Text(
                        stringResource(R.string.user_profile_section_student),
                        style = MaterialTheme.typography.titleMedium,
                    )
                  }
                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_course),
                      value = data.studentProfile.course,
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_class),
                      value = data.studentProfile.enrolledClass,
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_progress),
                      value = "${(data.studentProfile.progress * 100).toInt()}%",
                  )
                  ProfileInfoRow(
                      label = stringResource(R.string.user_profile_label_student_status),
                      value =
                          when (data.studentProfile.status) {
                            tech.datatower.sebrae.desafio.data.model.StudentStatus.Active ->
                                stringResource(R.string.status_active)
                            tech.datatower.sebrae.desafio.data.model.StudentStatus.Inactive ->
                                stringResource(R.string.status_inactive)
                            tech.datatower.sebrae.desafio.data.model.StudentStatus.Graduated ->
                                stringResource(R.string.status_graduated)
                          },
                  )

                  if (data.classesEnrolled.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.user_profile_classes_enrolled),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    data.classesEnrolled.forEach { cls ->
                      Text(
                          "• ${cls.name} — ${cls.course}",
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.padding(start = 8.dp),
                      )
                    }
                  }
                }
              }
            }

            // ── Sem vínculos ──────────────────────────────────────
            if (data.teacherProfile == null && data.studentProfile == null) {
              ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.user_profile_no_links),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
              }
            }

            Spacer(Modifier.height(24.dp))
          }
        }
      }
    }
  }
}

/**
 * Linha de informação com rótulo à esquerda e valor à direita.
 *
 * @param label Rótulo descritivo do dado.
 * @param value Valor formatado para exibição.
 */
@Composable
private fun ProfileInfoRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1.5f),
    )
  }
}

/** Converte UserRole em label de exibição. */
private fun roleLabel(role: UserRole): String =
    when (role) {
      UserRole.PROFESSOR -> "Professor"
      UserRole.COORDENADOR -> "Coordenador"
      UserRole.ADMINISTRADOR -> "Administrador"
    }
