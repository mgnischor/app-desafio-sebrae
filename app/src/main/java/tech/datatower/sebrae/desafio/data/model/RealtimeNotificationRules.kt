/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/RealtimeNotificationRules.kt
    Descrição: Regras de notificação em tempo real para eventos do sistema.
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
