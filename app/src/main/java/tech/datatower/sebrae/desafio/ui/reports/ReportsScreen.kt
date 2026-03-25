package tech.datatower.sebrae.desafio.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import java.util.Locale

/**
 * Executa a rotina de reports screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit = {}) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val summary by
      repository
          .observeReportSummary()
          .collectAsState(
              initial =
                  tech.datatower.sebrae.desafio.data.repository.ReportSummary(
                      activeStudents = 0,
                      activeCourses = 0,
                      totalClasses = 0,
                      completionRate = 0f,
                      certificates = 0,
                      averageTeacherRating = 0f,
                  )
          )
  val courseCompletion by
      repository.observeCourseCompletionMetrics().collectAsState(initial = emptyList())
  val monthly by repository.observeMonthlyEnrollmentMetrics().collectAsState(initial = emptyList())

  DetailScaffold(title = stringResource(R.string.reports_title), onBack = onBack) { innerPadding, _
    ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { SectionHeader(stringResource(R.string.section_overview)) }
      item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          KpiCard(
              summary.activeStudents.toString(),
              stringResource(R.string.label_active_students),
              Modifier.weight(1f),
          )
          KpiCard(
              summary.activeCourses.toString(),
              stringResource(R.string.label_active_courses),
              Modifier.weight(1f),
          )
          KpiCard(
              summary.totalClasses.toString(),
              stringResource(R.string.stat_classes),
              Modifier.weight(1f),
          )
        }
      }
      item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          KpiCard(
              stringResource(R.string.label_progress, (summary.completionRate * 100).toInt()),
              stringResource(R.string.label_completion_rate),
              Modifier.weight(1f),
          )
          KpiCard(
              summary.certificates.toString(),
              stringResource(R.string.menu_certificates),
              Modifier.weight(1f),
          )
          KpiCard(
              String.format(Locale.getDefault(), "%.1f★", summary.averageTeacherRating),
              stringResource(R.string.label_average_rating),
              Modifier.weight(1f),
          )
        }
      }
      item { SectionHeader(stringResource(R.string.label_completion_by_course)) }
      item { CourseCompletionReport(courseCompletion) }
      item { SectionHeader(stringResource(R.string.label_enrollments_by_month)) }
      item { EnrollmentMonthlyReport(monthly) }
    }
  }
}

/**
 * Executa a rotina de section header dentro do contexto deste componente.
 *
 * @param text Valor de entrada utilizado por esta opera??o.
 */
@Composable
private fun SectionHeader(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground,
  )
}

/**
 * Executa a rotina de kpi card dentro do contexto deste componente.
 *
 * @param value Valor de entrada utilizado por esta opera??o.
 * @param label Valor de entrada utilizado por esta opera??o.
 * @param modifier Valor de entrada utilizado por esta opera??o.
 */
@Composable
private fun KpiCard(value: String, label: String, modifier: Modifier = Modifier) {
  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/**
 * Executa a rotina de course completion report dentro do contexto deste componente.
 *
 * @param items Valor de entrada utilizado por esta opera??o.
 */
@Composable
private fun CourseCompletionReport(
    items: List<tech.datatower.sebrae.desafio.data.repository.CourseCompletionMetric>
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = item.name,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.width(156.dp),
          )
          LinearProgressIndicator(
              progress = { item.rate },
              modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.primaryContainer,
              strokeCap = StrokeCap.Round,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
              text = stringResource(R.string.label_progress, (item.rate * 100).toInt()),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.width(48.dp),
          )
        }
      }
    }
  }
}

/**
 * Executa a rotina de enrollment monthly report dentro do contexto deste componente.
 *
 * @param items Valor de entrada utilizado por esta opera??o.
 */
@Composable
private fun EnrollmentMonthlyReport(
    items: List<tech.datatower.sebrae.desafio.data.repository.MonthlyEnrollmentMetric>
) {
  val maxCount = items.maxOfOrNull { it.count } ?: 1
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom,
    ) {
      items.forEach { item ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
              text = "${item.count}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          val barHeight = (item.count.toFloat() / maxCount * 80).dp
          Box(
              modifier =
                  Modifier.width(26.dp)
                      .height(barHeight)
                      .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = item.month,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

/** Executa a rotina de reports preview dentro do contexto deste componente. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReportsPreview() {
  AppDesafioSEBRAETheme { ReportsScreen() }
}
