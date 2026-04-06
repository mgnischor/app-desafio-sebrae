pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Educalink"

include(":app")

include(":core")

include(":feature:login")

include(":feature:home")

include(":feature:students")

include(":feature:courses")

include(":feature:classes")

include(":feature:teachers")

include(":feature:reports")

include(":feature:certificates")

include(":feature:calendar")

include(":feature:settings")

include(":feature:users")

include(":feature:companies")
