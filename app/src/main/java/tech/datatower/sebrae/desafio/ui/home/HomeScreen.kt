package tech.datatower.sebrae.desafio.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import java.util.Calendar

// ── Data models ───────────────────────────────────────────────────────────────

data class MenuModule(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

data class QuickStat(
    val labelRes: Int,
    val value: String,
    val progress: Float? = null,   // 0f..1f  — null = não mostrar barra
    val trendLabel: String? = null,
)

data class RecentActivity(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val timeLabel: String,
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String = "Maria",
    notificationCount: Int = 3,
    onModuleClick: (Int) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val modules = rememberModules()
    val stats = rememberStats()
    val recents = rememberRecents()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                scrollBehavior    = scrollBehavior,
                notificationCount = notificationCount,
                onNotificationsClick = onNotificationsClick,
                onProfileClick    = onProfileClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {

            // ── Greeting + search ──────────────────────────────────────────
            item {
                GreetingSection(userName = userName)
            }

            item {
                SearchBar()
            }

            // ── Quick stats ───────────────────────────────────────────────
            item {
                SectionTitle(
                    text = stringResource(R.string.section_overview),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            item {
                StatsRow(stats = stats)
            }

            // ── Modules grid ──────────────────────────────────────────────
            item {
                SectionTitle(
                    text = stringResource(R.string.section_modules),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                )
            }

            item {
                ModulesGrid(
                    modules = modules,
                    onModuleClick = onModuleClick,
                )
            }

            // ── Recent activity ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(text = stringResource(R.string.section_recent))
                    Text(
                        text = stringResource(R.string.see_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            items(recents.size) { index ->
                RecentActivityItem(
                    item = recents[index],
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── TopAppBar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    notificationCount: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo placeholder — swap with your actual drawable
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Perfil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        scrollBehavior = scrollBehavior,
    )
}

// ── Greeting ──────────────────────────────────────────────────────────────────

@Composable
private fun GreetingSection(userName: String) {
    val greetingRes = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> R.string.home_greeting_morning
        in 12..17 -> R.string.home_greeting_afternoon
        else      -> R.string.home_greeting_evening
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
                text = "Veja o resumo de hoje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Search ────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar() {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
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
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor    = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

// ── Section title ─────────────────────────────────────────────────────────────

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

@Composable
private fun StatsRow(stats: List<QuickStat>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.forEach { stat ->
            StatCard(stat = stat, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(stat: QuickStat, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = StrokeCap.Round,
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

@Composable
private fun ModulesGrid(
    modules: List<MenuModule>,
    onModuleClick: (Int) -> Unit,
) {
    // Fixed-height grid (avoids nested scroll issues inside LazyColumn)
    val rows = (modules.size + 1) / 2
    val cardHeight = 100.dp
    val verticalSpacing = 12.dp
    val gridHeight = cardHeight * rows + verticalSpacing * (rows - 1)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = false,
    ) {
        items(modules) { module ->
            ModuleCard(module = module, onClick = { onModuleClick(module.titleRes) })
        }
    }
}

@Composable
private fun ModuleCard(module: MenuModule, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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

@Composable
private fun RecentActivityItem(item: RecentActivity, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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

@Composable
private fun rememberModules(): List<MenuModule> = remember {
    listOf(
        MenuModule(R.string.menu_students,     R.string.menu_students_desc,     Icons.Outlined.Person),
        MenuModule(R.string.menu_courses,      R.string.menu_courses_desc,      Icons.AutoMirrored.Outlined.MenuBook, badgeCount = 2),
        MenuModule(R.string.menu_classes,      R.string.menu_classes_desc,      Icons.Outlined.Group),
        MenuModule(R.string.menu_teachers,     R.string.menu_teachers_desc,     Icons.Outlined.School),
        MenuModule(R.string.menu_reports,      R.string.menu_reports_desc,      Icons.Outlined.InsertChart),
        MenuModule(R.string.menu_certificates, R.string.menu_certificates_desc, Icons.Outlined.Bookmarks),
        MenuModule(R.string.menu_calendar,     R.string.menu_calendar_desc,     Icons.Outlined.DateRange),
        MenuModule(R.string.menu_settings,     R.string.menu_settings_desc,     Icons.Outlined.Settings),
    )
}

@Composable
private fun rememberStats(): List<QuickStat> = remember {
    listOf(
        QuickStat(R.string.stat_students,   "1.240", trendLabel = "+12 este mês"),
        QuickStat(R.string.stat_courses,    "38"),
        QuickStat(R.string.stat_classes,    "74"),
        QuickStat(R.string.stat_completion, "82%", progress = 0.82f),
    )
}

@Composable
private fun rememberRecents(): List<RecentActivity> = remember {
    listOf(
        RecentActivity("Novo aluno matriculado", "Carlos Souza — Turma B3", Icons.Outlined.Person, "há 5 min"),
        RecentActivity("Curso publicado", "Excel para Negócios · Nível 2", Icons.AutoMirrored.Outlined.MenuBook, "há 1h"),
        RecentActivity("Certificado emitido", "Ana Lima — Marketing Digital", Icons.Outlined.Bookmarks, "há 3h"),
        RecentActivity("Aula agendada", "Empreendedorismo · Quinta-feira", Icons.Outlined.DateRange, "ontem"),
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "Home — Light")
@Composable
private fun HomeScreenPreviewLight() {
    AppDesafioSEBRAETheme(darkTheme = false) {
        HomeScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Home — Dark")
@Composable
private fun HomeScreenPreviewDark() {
    AppDesafioSEBRAETheme(darkTheme = true) {
        HomeScreen()
    }
}

