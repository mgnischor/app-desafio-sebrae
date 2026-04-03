# Arquitetura — App Desafio SEBRAE

## Visão Geral

O aplicativo é um sistema de **Gestão Educacional** para Android, desenvolvido em Kotlin com
Jetpack Compose. Segue uma arquitetura em camadas inspirada no **Clean Architecture**, combinada
com o padrão **Repository** e injeção de dependências via **Hilt**.

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
| Injeção de Dependências | Hilt | 2.59.2 |
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
│   settings, users, companies)                        │
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
├── SebraeApplication.kt          # @HiltAndroidApp — pre-warm + bootstrap via AppGraph
├── MainActivity.kt               # @AndroidEntryPoint — @Inject AppRepository; aplica tema
│
├── di/
│   └── AppModule.kt              # @Module @InstallIn(SingletonComponent) — provê todos os singletons
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
│       └── AppGraph.kt           # @EntryPoint bridge — acesso ao grafo Hilt em contextos Compose
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
    ├── users/                    # UserManagementScreen
    └── companies/                # CompanyManagementScreen, CompanyDetailScreen, CompanySelectorScreen
```

---

## Injeção de Dependências (Hilt)

O projeto utiliza **Hilt** (`com.google.dagger:hilt-android:2.56.2`) para gerenciar o ciclo de vida
de todas as dependências singleton da camada de dados. O processamento de anotações é feito via
**KSP** (sem KAPT).

### Entrypoints Android

- `SebraeApplication` — anotada com `@HiltAndroidApp`; dispara a inicialização do grafo Hilt e
  chama `AppGraph.warmUp()` para pré-aquecer singletons e Bootstrap remoto antes do primeiro frame.
- `MainActivity` — anotada com `@AndroidEntryPoint`; recebe `AppRepository` via `@Inject`.

### Módulo de Dados (`di/AppModule.kt`)

Todo provisionamento de singletons está centralizado em `AppModule`, instalado em
`SingletonComponent`:

| Binding | Escopo | Descrição |
|---|---|---|
| `AppDatabase` | `@Singleton` | Room DB `sebrae_local.db` |
| `AppDao` | `@Singleton` | DAO extraído do `AppDatabase` |
| `MutableStateFlow<Int>` (`@DataSourceLabelFlow`) | `@Singleton` | Rótulo reativo da fonte de dados |
| `AppRepository` | `@Singleton` | Repositório central da UI |
| `FirebaseFirestore` | `@Singleton` | Instância do Firestore |
| `FirebaseSeedCredentialStore` | `@Singleton` | Store de credenciais cifradas |
| `FirebaseDataConnectService` | `@Singleton` | Serviço de sync Firestore → Room |

### Bridge para Composables (`AppGraph`)

Composables que não são injetados via ViewModel ainda acessam dependências através de
`AppGraph.repository(context)` e `AppGraph.dataConnectService(context)`. Internamente,
`AppGraph` usa `@EntryPoint` + `EntryPointAccessors` para delegar ao container Hilt,
garantindo que **uma única instância singleton** seja usada em toda a aplicação:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppGraphEntryPoint {
    fun repository(): AppRepository
    fun dataConnectService(): FirebaseDataConnectService
    fun dao(): AppDao
}

object AppGraph {
    fun repository(context: Context): AppRepository =
        EntryPointAccessors.fromApplication(context.applicationContext,
            AppGraphEntryPoint::class.java).repository()
}
```

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

| TELA | Coleções sincronizadas |
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
| COMPANIES | companies, user_companies |

---

## Multi-Tenancy (Empresas)

O sistema suporta **múltiplas empresas (escolas)** com isolamento de dados por empresa. Cada
registro de dados (cursos, turmas, alunos, professores, etc.) está vinculado a uma empresa
através do campo `companyId`.

### Modelo de Dados

- **`CompanyEntity`** (tabela `companies`) — cadastro de empresas (id, name, cnpj, isActive).
- **`UserCompanyEntity`** (tabela `user_companies`) — vínculo N:N entre usuários e empresas
  (userId, companyId).
- Todas as 13 entidades de dados (courses, students, classes, teachers, certificates,
  calendar_events, recent_activities, monthly_enrollments, attendance, behaviors,
  pedagogical_needs, psychological_needs, parent_follow_ups) possuem campo `companyId`.

### Controle de Acesso por Empresa

- **ADMINISTRADOR** — acesso a todas as empresas ativas; pode criar, editar e excluir empresas;
  pode vincular/desvincular usuários a empresas.
- **COORDENADOR / PROFESSOR** — acesso somente às empresas explicitamente vinculadas via
  `user_companies`.

### Fluxo de Seleção de Empresa

1. Após login, `AuthManager.loadUserCompaniesFromFirestore()` carrega as empresas do usuário.
2. A primeira empresa da lista é automaticamente selecionada como empresa ativa.
3. No HomeScreen, o nome da empresa ativa aparece no TopAppBar. Se o usuário tem acesso a mais
   de uma empresa, um botão permite abrir o `CompanySelectorScreen`.
4. Ao trocar de empresa via `AuthManager.switchCompany()`, todos os ViewModels reativamente
   filtram seus dados pelo novo `companyId` via `flatMapLatest`.

### Filtragem por Empresa nos ViewModels

Cada ViewModel observa `AuthManager.currentCompany` e usa `flatMapLatest` para reobservar os
dados do `AppRepository` com o `companyId` correto:

```kotlin
private val companyId = AuthManager.currentCompany
    .map { it?.id }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

val data = companyId.filterNotNull().flatMapLatest { cid ->
    repository.observeXxx(cid)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

### Telas de Empresa

| Tela | Rota | Descrição |
|---|---|---|
| CompanyManagementScreen | `companies` | CRUD de empresas (admin) |
| CompanyDetailScreen | `companies/{companyId}` | Edição + gestão de usuários vinculados |
| CompanySelectorScreen | `company_selector` | Troca de empresa ativa |

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

- [x] **Adotar Hilt** para substituir o grafo manual (`AppGraph`), reduzindo boilerplate e
  tornando as dependências testáveis
- [x] **Introduzir ViewModels** para cada feature, separando lógica de estado da camada de UI
  e sobrevivendo a reconfiguração de tela; com Hilt já configurado, basta adicionar
  `@HiltViewModel` e `@Inject constructor`
- [x] **Criar módulos Gradle por feature** (`students`, `courses`, etc.) para melhorar tempos
  de build incremental e limitar visibilidade entre módulos
- [x] **Definir UseCases** (interactors) entre o `AppRepository` e a UI, evitando que regras de
  negócio fiquem espalhadas nos Composables
- [x] **Implementar o `RemoteBootstrapper`** com hydration real do Firestore (atualmente retorna
  `false` sem fazer nada)
- [x] **Adicionar tratamento de estado offline** explícito: exibir indicador de dados desatualizados
  quando não há conectividade
- [ ] **Cobrir o repositório com testes de integração** usando Room in-memory database;
  com Hilt, usar `@HiltAndroidTest` + `HiltTestApplication`
- [ ] **Migrar de `fallbackToDestructiveMigration`** para migrações versionadas no Room, evitando
  perda de dados em atualizações
- [ ] **Centralizar CoroutineScope** de operações de dados: evitar criação de scope local em
  `FirebaseDataConnectService`
- [ ] **Definir camada de mapeamento explícita** (mappers) entre entidades Room e modelos de
  domínio, atualmente embutidos nos próprios arquivos de entidade
