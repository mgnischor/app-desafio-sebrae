package tech.datatower.sebrae.desafio.data.model

/** Tipo de mutação persistida no sistema para geração de atividade recente. */
enum class MutationAction {
  Created,
  Updated,
  Deleted,
}

/** Estrutura intermediária para criação de notificações no feed de atividades recentes. */
data class RecentActivityDraft(
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val timeLabel: String,
)

/** Regras de negócio para notificação em tempo real do painel administrativo. */
object RealtimeNotificationRules {

  /**
   * Indica se o perfil pode receber atividades recentes em tempo real.
   *
   * @param role Perfil do usuário autenticado.
   * @return `true` para Administrador e Coordenador.
   */
  fun canReceiveBackofficeNotifications(role: UserRole?): Boolean {
    return role == UserRole.ADMINISTRADOR || role == UserRole.COORDENADOR
  }

  /**
   * Monta o conteúdo textual padronizado para o feed de atividade recente.
   *
   * @param action Tipo da ação ocorrida.
   * @param entityName Nome do recurso alterado (ex.: "Aluno", "Curso").
   * @param targetLabel Identificação do registro alterado.
   */
  fun buildDraft(
      action: MutationAction,
      entityName: String,
      targetLabel: String,
  ): RecentActivityDraft {
    val title =
        when (action) {
          MutationAction.Created -> "$entityName cadastrado"
          MutationAction.Updated -> "$entityName alterado"
          MutationAction.Deleted -> "$entityName excluído"
        }

    val iconKey =
        when {
          entityName.equals("Aluno", ignoreCase = true) -> "person"
          entityName.equals("Curso", ignoreCase = true) -> "course"
          else -> "calendar"
        }

    return RecentActivityDraft(
        title = title,
        subtitle = targetLabel,
        iconKey = iconKey,
        timeLabel = "agora",
    )
  }
}
