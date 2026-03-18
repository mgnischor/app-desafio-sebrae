package tech.datatower.sebrae.desafio.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import tech.datatower.sebrae.desafio.ui.calendar.CalendarScreen
import tech.datatower.sebrae.desafio.ui.certificates.CertificatesScreen
import tech.datatower.sebrae.desafio.ui.classes.ClassesScreen
import tech.datatower.sebrae.desafio.ui.courses.CoursesScreen
import tech.datatower.sebrae.desafio.ui.home.HomeScreen
import tech.datatower.sebrae.desafio.ui.reports.ReportsScreen
import tech.datatower.sebrae.desafio.ui.settings.SettingsScreen
import tech.datatower.sebrae.desafio.ui.students.StudentsScreen
import tech.datatower.sebrae.desafio.ui.teachers.TeachersScreen

@Composable
fun AppNavHost(navController: NavHostController) {
  NavHost(
      navController = navController,
      startDestination = AppRoutes.HOME,
  ) {
    composable(AppRoutes.HOME) {
      HomeScreen(
          onModuleClick = { route -> navController.navigate(route) },
      )
    }
    composable(AppRoutes.STUDENTS) { StudentsScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.COURSES) { CoursesScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.CLASSES) { ClassesScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.TEACHERS) { TeachersScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.REPORTS) { ReportsScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.CERTIFICATES) {
      CertificatesScreen(onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.CALENDAR) { CalendarScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
  }
}
