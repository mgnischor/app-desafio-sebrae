package tech.datatower.sebrae.desafio.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.MenuModule
import tech.datatower.sebrae.desafio.data.model.QuickStat
import tech.datatower.sebrae.desafio.data.model.RealtimeNotificationRules
import tech.datatower.sebrae.desafio.data.model.RecentActivity
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import java.util.Calendar

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Tela inicial do aplicativo com visão geral operacional.
 *
 * Reúne atalhos de módulos, indicadores rápidos e atividades recentes, além de permitir busca
 * textual para filtrar conteúdo visível. Os módulos exibidos são filtrados de acordo com o perfil
 * do usuário autenticado.
 *
 * @param user Usuário autenticado; define os módulos acessíveis.
 * @param notificationCount Quantidade de notificações pendentes exibida na barra superior.
 * @param onModuleClick Callback chamado ao selecionar um módulo do grid.
 * @param onNotificationsClick Callback chamado ao tocar no ícone de notificações.
 * @param onLogout Callback chamado ao solicitar encerramento de sessão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: AppUser? = null,
    notificationCount: Int = 3,
    onModuleClick: (String) -> Unit = {},
    onOpenRecentActivities: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataConnectService =
      remember(context) { AppGraph.dataConnectService(context.applicationContext) }

  val allModules = rememberModules()
  val moduleTitleMap =
      remember(allModules, context) { allModules.associateBy { context.getString(it.titleRes) } }
  val moduleDescriptionMap =
      remember(allModules, context) {
        allModules.associateBy { context.getString(it.descriptionRes) }
      }
  val modules = remember(allModules, user) { filterModulesByRole(allModules, user?.role) }
  val canSeeBackofficeRecents =
      remember(user?.role) {
        RealtimeNotificationRules.canReceiveBackofficeNotifications(user?.role)
      }
  val stats by repository.observeHomeQuickStats().collectAsState(initial = emptyList())
  val recents by repository.observeRecentActivities(limit = 5).collectAsState(initial = emptyList())
  var query by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(Unit) { dataConnectService.syncScope(ScreenDataScope.HOME) }

  LaunchedEffect(user?.id, user?.role) {
    dataConnectService.observeRecentActivitiesRealtimeForBackoffice(user).collect { result ->
      if (
          result
              is
              tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService.Result.Error
      ) {
        Log.w("HomeScreen", "Falha no listener de atividades recentes: ${result.message}")
      }
    }
  }

  val filteredModules by
      remember(query, modules) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            modules
          } else {
            modules.filter {
              moduleTitleMap.keys.any { title ->
                title.contains(normalizedQuery, ignoreCase = true) && moduleTitleMap[title] == it
              } ||
                  moduleDescriptionMap.keys.any { desc ->
                    desc.contains(normalizedQuery, ignoreCase = true) &&
                        moduleDescriptionMap[desc] == it
                  }
            }
          }
        }
      }
  val filteredRecents by
      remember(query, recents) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          val source = if (canSeeBackofficeRecents) recents else emptyList()
          if (normalizedQuery.isBlank()) {
            source
          } else {
            source.filter {
              it.title.contains(normalizedQuery, ignoreCase = true) ||
                  it.subtitle.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }
  val homeRecents by remember(filteredRecents) { derivedStateOf { filteredRecents } }

  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        HomeTopBar(
            scrollBehavior = scrollBehavior,
            notificationCount = notificationCount,
            onNotificationsClick = onNotificationsClick,
            onLogout = onLogout,
        )
      },
      containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {

      // ── Greeting + search ──────────────────────────────────────────
      item { GreetingSection(userName = user?.name ?: "") }

      item {
        SearchBar(
            query = query,
            onQueryChange = { query = it },
        )
      }

      // ── Quick stats ───────────────────────────────────────────────
      item {
        SectionTitle(
            text = stringResource(R.string.section_overview),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
      }

      item { StatsRow(stats = stats) }

      // ── Modules grid ──────────────────────────────────────────────
      item {
        SectionTitle(
            text = stringResource(R.string.section_modules),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )
      }

      item {
        ModulesGrid(
            modules = filteredModules,
            onModuleClick = onModuleClick,
        )
      }

      // ── Recent activity ───────────────────────────────────────────
      item {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          SectionTitle(text = stringResource(R.string.section_recent))
          if (canSeeBackofficeRecents) {
            Text(
                text = stringResource(R.string.see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenRecentActivities() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (homeRecents.isEmpty()) {
        item {
          Text(
              text = stringResource(R.string.recent_empty),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(20.dp),
          )
        }
      } else {
        items(
            count = homeRecents.size,
            key = { index -> homeRecents[index].id },
            contentType = { "recent" },
        ) { index ->
          RecentActivityItem(
              item = homeRecents[index],
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

// ── TopAppBar ─────────────────────────────────────────────────────────────────

/**
 * Barra superior da tela inicial com branding e ações rápidas.
 *
 * @param scrollBehavior Comportamento de scroll aplicado ao `TopAppBar`.
 * @param notificationCount Quantidade de notificações pendentes.
 * @param onNotificationsClick Ação para abrir notificações.
 * @param onLogout Ação para encerrar a sessão do usuário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    notificationCount: Int,
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit,
) {
  TopAppBar(
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Logo placeholder — swap with your actual drawable
          Box(
              modifier =
                  Modifier.size(32.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      },
      actions = {
        BadgedBox(
            badge = {
              if (notificationCount > 0) {
                Badge { Text(text = notificationCount.toString()) }
              }
            }
        ) {
          IconButton(onClick = onNotificationsClick) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onLogout) {
          Icon(
              imageVector = Icons.Outlined.AccountCircle,
              contentDescription = stringResource(R.string.logout),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ),
      scrollBehavior = scrollBehavior,
  )
}

// ── Greeting ──────────────────────────────────────────────────────────────────

/**
 * Bloco de saudação com mensagem contextual baseada no horário atual.
 *
 * @param userName Nome a ser exibido na saudação.
 */
@Composable
private fun GreetingSection(userName: String) {
  val greetingRes =
      when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..17 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
      }
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                              MaterialTheme.colorScheme.background,
                          )
                  )
              )
              .padding(horizontal = 20.dp, vertical = 20.dp),
  ) {
    Column {
      Text(
          text = "${stringResource(greetingRes)}, $userName!",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
          text = stringResource(R.string.home_summary_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

// ── Search ────────────────────────────────────────────────────────────────────

/**
 * Campo de busca principal da Home.
 *
 * @param query Valor atual do filtro textual.
 * @param onQueryChange Callback acionado quando o texto de busca é alterado.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = {
        Text(
            text = stringResource(R.string.home_search_placeholder),
            style = MaterialTheme.typography.bodyMedium,
        )
      },
      leadingIcon = {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
  )
}

// ── Section title ─────────────────────────────────────────────────────────────

/**
 * Título padronizado para seções internas da tela inicial.
 *
 * @param text Texto do título.
 * @param modifier Modificador opcional para posicionamento externo.
 */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
  Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = modifier,
  )
}

// ── Stats row ─────────────────────────────────────────────────────────────────

/**
 * Linha horizontal de indicadores rápidos.
 *
 * @param stats Lista de métricas a serem renderizadas.
 */
@Composable
private fun StatsRow(stats: List<QuickStat>) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    stats.forEach { stat -> StatCard(stat = stat, modifier = Modifier.weight(1f)) }
  }
}

/**
 * Cartão de indicador rápido com valor, rótulo e dados opcionais de tendência.
 *
 * @param stat Métrica exibida no cartão.
 * @param modifier Modificador opcional para composição externa.
 */
@Composable
private fun StatCard(stat: QuickStat, modifier: Modifier = Modifier) {
  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
          text = stat.value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
      )
      Text(
          text = stringResource(stat.labelRes),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      stat.progress?.let { p ->
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { p },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            strokeCap = StrokeCap.Round,
        )
      }
      stat.trendLabelRes?.let { labelRes ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
      stat.trendLabel?.let { label ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }
}

// ── Modules grid ──────────────────────────────────────────────────────────────

/**
 * Grid fixo de módulos navegáveis exibido na Home.
 *
 * O componente desabilita rolagem própria para evitar conflito com a `LazyColumn` pai.
 *
 * @param modules Lista de módulos a serem exibidos.
 * @param onModuleClick Callback acionado com a rota do módulo selecionado.
 */
@Composable
private fun ModulesGrid(
    modules: List<MenuModule>,
    onModuleClick: (String) -> Unit,
) {
  // Fixed-height grid (avoids nested scroll issues inside LazyColumn)
  val rows = (modules.size + 1) / 2
  val cardHeight = 100.dp
  val verticalSpacing = 12.dp
  val gridHeight = cardHeight * rows + verticalSpacing * (rows - 1)

  LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.fillMaxWidth().height(gridHeight).padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(verticalSpacing),
      userScrollEnabled = false,
  ) {
    items(
        items = modules,
        key = { it.route },
        contentType = { "module" },
    ) { module ->
      ModuleCard(module = module, onClick = { onModuleClick(module.route) })
    }
  }
}

/**
 * Cartão clicável de um módulo da aplicação.
 *
 * @param module Metadados do módulo (título, descrição, ícone e rota).
 * @param onClick Ação executada ao selecionar o cartão.
 */
@Composable
private fun ModuleCard(module: MenuModule, onClick: () -> Unit) {
  Card(
      onClick = onClick,
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
      modifier = Modifier.fillMaxWidth().height(100.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          BadgedBox(
              badge = {
                if (module.badgeCount > 0) {
                  Badge { Text(text = module.badgeCount.toString()) }
                }
              }
          ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
          }
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
            text = stringResource(module.titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(module.descriptionRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// ── Recent activity ───────────────────────────────────────────────────────────

/**
 * Item de atividade recente no feed da tela inicial.
 *
 * @param item Dados da atividade a ser exibida.
 * @param modifier Modificador opcional para customização de layout.
 */
@Composable
private fun RecentActivityItem(item: RecentActivity, modifier: Modifier = Modifier) {
  ElevatedCard(
      modifier = modifier.fillMaxWidth(),
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
      Box(
          modifier =
              Modifier.size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
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

// ── Static data helpers ───────────────────────────────────────────────────────

/**
 * Filtra a lista completa de módulos de acordo com o perfil do usuário.
 *
 * - **PROFESSOR**: Alunos, Calendário e Certificados.
 * - **COORDENADOR**: Alunos, Cursos, Turmas, Instrutores, Calendário, Certificados e Relatórios.
 * - **ADMINISTRADOR**: Todos os módulos.
 *
 * @param allModules Lista completa de módulos disponíveis na aplicação.
 * @param role Perfil do usuário autenticado; `null` exibe apenas módulos básicos.
 * @return Lista filtrada de módulos permitidos para o perfil informado.
 */
private fun filterModulesByRole(allModules: List<MenuModule>, role: UserRole?): List<MenuModule> {
  val allowedRoutes =
      when (role) {
        UserRole.PROFESSOR ->
            setOf(
                AppRoutes.STUDENTS,
                AppRoutes.COURSES,
                AppRoutes.CLASSES,
                AppRoutes.CALENDAR,
            )
        UserRole.COORDENADOR ->
            setOf(
                AppRoutes.STUDENTS,
                AppRoutes.COURSES,
                AppRoutes.CLASSES,
                AppRoutes.TEACHERS,
                AppRoutes.CALENDAR,
            )
        UserRole.ADMINISTRADOR,
        null -> null // null = all routes visible
      }
  return if (allowedRoutes == null) allModules else allModules.filter { it.route in allowedRoutes }
}

/**
 * Memoriza os módulos disponíveis na Home para evitar recriações desnecessárias.
 *
 * @return Lista imutável com todos os módulos e suas respectivas rotas.
 */
@Composable
private fun rememberModules(): List<MenuModule> = remember {
  listOf(
      MenuModule(
          R.string.menu_students,
          R.string.menu_students_desc,
          Icons.Outlined.Person,
          AppRoutes.STUDENTS,
      ),
      MenuModule(
          R.string.menu_courses,
          R.string.menu_courses_desc,
          Icons.AutoMirrored.Outlined.MenuBook,
          AppRoutes.COURSES,
          badgeCount = 2,
      ),
      MenuModule(
          R.string.menu_classes,
          R.string.menu_classes_desc,
          Icons.Outlined.Group,
          AppRoutes.CLASSES,
      ),
      MenuModule(
          R.string.menu_teachers,
          R.string.menu_teachers_desc,
          Icons.Outlined.School,
          AppRoutes.TEACHERS,
      ),
      MenuModule(
          R.string.menu_reports,
          R.string.menu_reports_desc,
          Icons.Outlined.InsertChart,
          AppRoutes.REPORTS,
      ),
      MenuModule(
          R.string.menu_certificates,
          R.string.menu_certificates_desc,
          Icons.Outlined.Bookmarks,
          AppRoutes.CERTIFICATES,
      ),
      MenuModule(
          R.string.menu_calendar,
          R.string.menu_calendar_desc,
          Icons.Outlined.DateRange,
          AppRoutes.CALENDAR,
      ),
      MenuModule(
          R.string.menu_settings,
          R.string.menu_settings_desc,
          Icons.Outlined.Settings,
          AppRoutes.SETTINGS,
      ),
  )
}

// ── Preview ───────────────────────────────────────────────────────────────────

/** Pré-visualização da Home no tema claro. */
@Preview(showBackground = true, showSystemUi = true, name = "Home — Light")
@Composable
private fun HomeScreenPreviewLight() {
  AppDesafioSEBRAETheme(darkTheme = false) { HomeScreen() }
}

/** Pré-visualização da Home no tema escuro. */
@Preview(showBackground = true, showSystemUi = true, name = "Home — Dark")
@Composable
private fun HomeScreenPreviewDark() {
  AppDesafioSEBRAETheme(darkTheme = true) { HomeScreen() }
}
