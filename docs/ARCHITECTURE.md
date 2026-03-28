# Arquitetura — App Desafio SEBRAE

## Visão Geral

O aplicativo é um sistema de **Gestão Educacional** para Android, desenvolvido em Kotlin com
Jetpack Compose. Segue uma arquitetura em camadas inspirada no **Clean Architecture**, combinada
com o padrão **Repository** e um grafo de dependências manual (sem framework de DI).

---

## Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Kotlin | 2.3.20 |
| UI | Jetpack Compose (BOM) | 2026.03.00 |
| Design System | Material 3 | via BOM |
| Navegação | Navigation Compose | 2.9.7 |
| Banco local | Room | 2.8.4 |
| Backend remoto | Firebase Firestore | 26.1.2 |
| Autenticação | Firebase Auth | 24.0.1 |
| Geração de código | KSP | 2.3.6 |
| Coroutines | Kotlinx Coroutines | 1.10.2 |
| Build | AGP | 9.1.0 |

---

## Camadas da Aplicação

```
┌──────────────────────────────────────────────────────┐
│                     UI Layer                         │
│  Jetpack Compose Screens + Shared Components         │
│  (login, home, students, courses, classes,           │
│   teachers, reports, certificates, calendar,         │
│   settings, users)                                   │
└───────────────────────┬──────────────────────────────┘
                        │ coleta StateFlow / Flow
┌───────────────────────▼──────────────────────────────┐
│                  Navigation Layer                    │
│  AppNavHost  ·  AppRoutes                            │
└───────────────────────┬──────────────────────────────┘
                        │ callbacks / lambdas
┌───────────────────────▼──────────────────────────────┐
│                   Data Layer                         │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              AppRepository                      │ │
│  │  Orquestra Room (cache local) + Firebase        │ │
│  │  Expõe Flow<T> para a UI                        │ │
│  └──────────────┬──────────────────────┬───────────┘ │
│                 │                      │             │
│  ┌──────────────▼───────┐  ┌───────────▼───────────┐ │
│  │   Local (Room)       │  │   Remote (Firebase)   │ │
│  │   AppDatabase        │  │   FirebaseDataConnect │ │
│  │   AppDao             │  │   Service             │ │
│  │   AppConverters      │  │   FirebaseRemote      │ │
│  └──────────────────────┘  │   Bootstrapper        │ │
│                            └───────────────────────┘ │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              Auth (transversal)                  │ │
│  │   AuthManager  ·  AccessPolicy                  │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              Domain Models                       │ │
│  │   data/model/  (imutáveis, @Immutable)           │ │
│  └─────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## Módulos e Pacotes Principais

```
tech.datatower.sebrae.desafio/
├── SebraeApplication.kt          # Application — pre-warm do grafo
├── MainActivity.kt               # Única Activity; aplica tema e inicia NavHost
│
├── navigation/
│   ├── AppNavHost.kt             # Declaração do NavHost com todas as rotas
│   └── AppRoutes.kt              # Catálogo de rotas; args de navegação
│
├── data/
│   ├── model/                    # DTOs / modelos de domínio imutáveis
│   ├── local/
│   │   └── AppDatabase.kt        # Room: entidades, DAOs, converters, DB
│   ├── remote/
│   │   └── firebase/
│   │       ├── FirebaseDataConnectService.kt   # Sync Firestore → Room
│   │       ├── FirebaseRemoteBootstrapper.kt   # Bootstrap inicial
│   │       ├── FirebaseScreenSyncPlanner.kt    # Escopo mínimo por tela
│   │       └── FirebaseSeedCredentialStore.kt  # Credenciais cifradas (AES/GCM)
│   ├── auth/
│   │   ├── AuthManager.kt        # Login Firebase + fallback local
│   │   └── AccessPolicy.kt       # RBAC — regras por papel e recurso
│   └── repository/
│       ├── AppRepository.kt      # Único repositório; expõe Flow<T>
│       └── AppGraph.kt           # Grafo manual de dependências (singleton)
│
└── ui/
    ├── theme/                    # Color.kt, Theme.kt, Type.kt
    ├── components/               # Componentes reutilizáveis (DetailScaffold, etc.)
    ├── home/                     # HomeScreen, RecentActivitiesScreen
    ├── login/                    # LoginScreen
    ├── students/                 # StudentsScreen, StudentCreateScreen, StudentMonitoringScreen
    ├── courses/                  # CoursesScreen, CourseCreateScreen, CourseDetailScreen
    ├── classes/                  # ClassesScreen, ClassCreateScreen, ClassDetailScreen
    ├── teachers/                 # TeachersScreen, TeacherCreateScreen, TeacherDetailScreen
    ├── reports/                  # ReportsScreen
    ├── certificates/             # CertificatesScreen
    ├── calendar/                 # CalendarScreen
    ├── settings/                 # SettingsScreen
    └── users/                    # UserManagementScreen
```

---

## Padrão de Injeção de Dependência

O projeto **não utiliza Hilt nem Koin**. A injeção é feita via **grafo manual** (`AppGraph`),
um `object` singleton seguro para threads que inicializa `AppDatabase`, `AppDao`,
`AppRepository` e `FirebaseDataConnectService` com `Double-Checked Locking`:

```kotlin
object AppGraph {
    @Volatile private var repository: AppRepository? = null

    fun repository(context: Context): AppRepository =
        repository ?: synchronized(this) {
            repository ?: buildRepository(context.applicationContext).also { repository = it }
        }
}
```

O `AppGraph.warmUp()` é chamado no `SebraeApplication.onCreate()` para pré-aquecer as
dependências antes do primeiro frame da UI.

---

## Fluxo de Dados (Unidirecional)

```
Firebase Firestore
      │  sync on demand (por escopo de tela)
      ▼
  Room (cache local: sebrae_local.db)
      │  Flow<List<T>> via AppDao
      ▼
  AppRepository
      │  Flow<T> transformado/combinado
      ▼
  Composable (collectAsState)
      │  eventos do usuário
      ▼
  AppRepository (mutações via suspend fun)
      │  upsert/delete
      ▼
  Room + Firestore (escrita bidirecional)
```

---

## Estratégia de Sincronização Remota

O `FirebaseScreenSyncPlanner` aplica **mínimo de dados necessários por tela** (`ScreenDataScope`)
para evitar downloads desnecessários. Cada tela solicita apenas as coleções que precisa:

| Tela | Coleções sincronizadas |
|---|---|
| HOME | courses, classes, students, recent_activities |
| COURSES / COURSE_DETAIL | courses, classes |
| STUDENTS | students, classes |
| STUDENT_MONITORING | students, attendance, behaviors, pedagogical_needs, psychological_needs, parent_follow_ups |
| TEACHERS / TEACHER_DETAIL | teachers, courses |
| CERTIFICATES | certificates |
| CALENDAR | calendar_events |
| REPORTS | courses, classes, students, teachers, certificates, monthly_enrollments |
| SETTINGS | settings |

---

## Single Activity + Compose Navigation

A aplicação possui **uma única Activity** (`MainActivity`). Toda a navegação é declarativa via
`NavHost`. As rotas são definidas como constantes tipadas em `AppRoutes`, com argumentos
inteiros passados via path parameter (`{studentId}`, `{courseId}`, etc.).

A tela de login é o `startDestination`. Após autenticação bem-sucedida, o back-stack do login
é limpo com `popUpTo(LOGIN) { inclusive = true }`, impedindo que o usuário volte para ela.

O tema (claro/escuro) é controlado reativamente via `AppSettings.darkMode`, observado diretamente
na `MainActivity`.

---

## Checklist de Melhorias de Arquitetura

- [ ] **Adotar Hilt** para substituir o grafo manual (`AppGraph`), reduzindo boilerplate e
  tornando as dependências testáveis
- [ ] **Introduzir ViewModels** para cada feature, separando lógica de estado da camada de UI
  e sobrevivendo a reconfiguração de tela
- [ ] **Criar módulos Gradle por feature** (`students`, `courses`, etc.) para melhorar tempos
  de build incremental e limitar visibilidade entre módulos
- [ ] **Definir UseCases** (interactors) entre o `AppRepository` e a UI, evitando que regras de
  negócio fiquem espalhadas nos Composables
- [ ] **Implementar o `RemoteBootstrapper`** com hydration real do Firestore (atualmente retorna
  `false` sem fazer nada)
- [ ] **Adicionar tratamento de estado offline** explícito: exibir indicador de dados desatualizados
  quando não há conectividade
- [ ] **Cobrir o repositório com testes de integração** usando Room in-memory database
- [ ] **Migrar de `fallbackToDestructiveMigration`** para migrações versionadas no Room, evitando
  perda de dados em atualizações
- [ ] **Centralizar CoroutineScope** de operações de dados: evitar criação de scope local em
  `FirebaseDataConnectService`
- [ ] **Definir camada de mapeamento explícita** (mappers) entre entidades Room e modelos de
  domínio, atualmente embutidos nos próprios arquivos de entidade
