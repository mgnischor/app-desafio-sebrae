# Padrões de Código — Educalink

## Linguagem e Versão

- **Kotlin 2.4.0** com compilação via **KSP 2.3.6** (sem KAPT)
- **AGP 9.1.1** + Gradle com Version Catalogs (`libs.versions.toml`)
- Target SDK / Compile SDK: Android 36 (configurado no AGP 9.1.1)

---

## Organização de Pacotes

O código segue organização **multi-módulo**, com separação por camada dentro de cada módulo:

```
app/                          — ponto de entrada; configuração Hilt e navegação
  di/                         — AppModule (singletons Hilt)
  navigation/                 — AppNavHost (grafo de rotas e argumentos)
  data/repository/            — AppGraph (bridge Hilt/Compose)

core/                         — código compartilhado entre módulos
  data/
    auth/                     — autenticação, autorização e seleção de empresa
    connectivity/             — observação reativa do estado de rede
    local/                    — Room (entidades, DAOs, converters, DB)
    model/                    — modelos de domínio (imutáveis, @Immutable)
    remote/                   — integrações externas (Firebase)
    repository/               — AppRepository (orquestração de fontes de dados)
  domain/usecase/             — use cases reativos (ObserveXxxUseCase, SyncScreenDataUseCase)
  ui/
    components/               — componentes Compose reutilizáveis
    theme/                    — Color, Theme, Type

feature/<nome>/               — um módulo por feature (12 no total)
  ui/<nome>/                  — Screen + ViewModel por tela
```

---

## Convenções de Nomenclatura

### Arquivos e Classes

| Tipo | Convenção | Exemplo |
|---|---|---|
| Tela Compose | `<Contexto>Screen.kt` | `StudentsScreen.kt` |
| Modelos de domínio | `<Nome>.kt` (PascalCase) | `Student.kt`, `Course.kt` |
| Entidades Room | `<Nome>Entity.kt` | `StudentEntity.kt` |
| DAOs Room | `<Nome>Dao` (interface/abstract) | `AppDao` |
| Repositórios | `<Contexto>Repository.kt` | `AppRepository.kt` |
| Objeto singleton | `<Contexto>` + `object` | `AppGraph`, `AuthManager` |
| Políticas de domínio | `<Contexto>Rules` | `EntityLifecycleRules`, `RelationshipRules` |
| Use Cases | `Observe<Entidade>UseCase.kt` | `ObserveStudentsUseCase.kt` |

### Variáveis e Funções

- **camelCase** para variáveis e funções
- Prefixo `_` para `MutableStateFlow`/`MutableState` privados, expostos sem prefixo
  via propriedade pública `val`:

```kotlin
private val _currentUser = MutableStateFlow<AppUser?>(null)
val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()
```

- Funções de observação reativa com prefixo `observe`: `observeCourses()`, `observeClassById()`
- Funções suspend de escrita: verbos de ação `insertStudent()`, `upsertSettings()`, `deleteTeacher()`

---

## Modelos de Domínio

Todos os modelos de domínio são `data class` imutáveis anotados com `@Immutable` do Compose,
garantindo que o compilador do Compose trate rerenderizações de forma otimizada:

```kotlin
@Immutable
data class Student(
    val id: Int,
    val name: String,
    val email: String,
    val course: String,
    val enrolledClass: String,
    val progress: Float, // 0f..1f
    val status: StudentStatus,
)
```

**Regras:**
- Nenhum modelo de domínio contém lógica de negócio
- Mutações via `copy()` encapsuladas em `EntityLifecycleRules`
- Enums tipados para status/estado (`StudentStatus`, `ClassStatus`, `EventType`)

---

## Padrão de Repositório

`AppRepository` é a única fonte de dados para a UI. Expõe dados como `Flow<T>`:

```kotlin
fun observeCourses(): Flow<List<Course>> =
    combine(dao.observeCourses(), dao.observeStudents()) { courseEntities, studentEntities ->
        // transformação e enriquecimento dos dados
    }
```

**Regras:**
- A UI **nunca** acessa `AppDao` ou `FirebaseDataConnectService` diretamente
- Operações de gravação são `suspend fun` chamadas com `lifecycleScope` ou scope do componente
- Dados sempre transitam por `Flow`; sem callbacks ou interfaces listener na camada de domínio

---

## Sealed Classes para Resultados

Operações assíncronas com Firebase retornam `sealed class Result<T>`:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String = "") : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

O mesmo padrão é usado no `AuthManager`:

```kotlin
sealed class FirebaseLoginResult {
    data class Success(val user: AppUser) : FirebaseLoginResult()
    data class Error(val message: String) : FirebaseLoginResult()
}
```

---

## Compose UI

### Estrutura de uma Tela

Cada tela segue este padrão geral:

```kotlin
// Comentário KDoc descrevendo a tela e parâmetros
@Composable
fun StudentsScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
    onCreateStudent: () -> Unit,
    onOpenStudentMonitoring: (Int) -> Unit,
) {
    // 1. Coleta de estado reativo
    val context = LocalContext.current
    val repository = remember(context) { AppGraph.repository(context.applicationContext) }
    val students by repository.observeStudents().collectAsState(initial = emptyList())

    // 2. Estado local da UI
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filtered by remember(students, searchQuery) { derivedStateOf { /* filtro */ } }

    // 3. Scaffold + conteúdo
    DetailScaffold(title = "...", onBack = onBack) { padding, scrollBehavior ->
        // conteúdo
    }
}
```

### Regras de Composição

- **`rememberSaveable`** para estado que deve sobreviver a recomposição e rotação de tela
  (campos de formulário, filtros)
- **`remember`** para caches de cálculo derivado (`derivedStateOf`) e objetos que não
  precisam sobreviver à recriação de Activity
- **`collectAsState(initial = ...)`** para observar `Flow`/`StateFlow` do repositório
- **`LazyColumn` / `LazyVerticalGrid`** para listas; nunca `Column` com scroll para listas
  dinâmicas
- Callbacks são lambdas sem lógica; lógica de negócio fica no repositório

---

## Componentes Reutilizáveis

| Componente | Localização | Responsabilidade |
|---|---|---|
| `DetailScaffold` | `ui/components/` | Scaffold padrão com TopAppBar e botão voltar |
| `EmptyState` | `ui/components/` | Estado vazio para listas sem resultados |
| `ListSearchHeader` | `ui/components/` | Cabeçalho com campo de busca para listas |
| `StatusChip` | `ui/components/` | Chip de status com cor semântica |

---

## Gestão de Dependências

O projeto usa **Gradle Version Catalogs** (`gradle/libs.versions.toml`) como única fonte de
verdade para versões de bibliotecas:

```toml
[versions]
room = "2.8.4"
hilt = "2.59.2"
composeBom = "2026.05.01"
navigationCompose = "2.9.8"
vico = "3.2.2"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**Regras:**
- Nenhuma versão hardcoded diretamente no `build.gradle.kts`
- Dependências de test sempre no escopo correto (`testImplementation`, `androidTestImplementation`)
- Firebase gerenciado via BOM (`firebase-bom`) para garantir compatibilidade entre libs
- Hilt usa **KSP** (`ksp(libs.hilt.compiler)`) — sem KAPT

## Injeção de Dependências

O projeto usa **Hilt** para gerenciar singletons da camada de dados. As regras são:

- **`@HiltAndroidApp`** em `SebraeApplication` — gerado pelo processador KSP; é o entrypoint do
  grafo.
- **`@AndroidEntryPoint`** em `MainActivity` — habilita injeção de campo na Activity.
- **`@Inject`** para receber dependências em Activities, Fragments e ViewModels Hilt.
- **`@Module @InstallIn(SingletonComponent::class)`** em `di/AppModule.kt` — fonte única de
  provisionamento dos singletons (`AppDatabase`, `AppDao`, `AppRepository`,
  `FirebaseDataConnectService`, `FirebaseSeedCredentialStore`).
- **`@EntryPoint`** em `AppGraphEntryPoint` — permite que `AppGraph` (bridge para composables
  sem ViewModel) acesse o container Hilt via `EntryPointAccessors.fromApplication()`.

```kotlin
// Composable sem ViewModel — acessa via bridge
val repository = remember(context) { AppGraph.repository(context.applicationContext) }

// Activity com @AndroidEntryPoint — injeção direta
@Inject lateinit var repository: AppRepository
```

**Não usar** `AppGraph.repository()` em novas classes que podem ser anotadas
com `@AndroidEntryPoint` ou `@HiltViewModel`.

---



- **Kotlin null safety** estritamente respeitado; sem `!!` exceto nos casos em que a invariante
  é verificada imediatamente antes
- `return@composable` para saída antecipada em Composables com argumento obrigatório ausente:

```kotlin
composable(...) { backStackEntry ->
    val studentId = backStackEntry.arguments?.getInt(STUDENT_ID_ARG) ?: return@composable
    StudentMonitoringScreen(studentId = studentId, ...)
}
```

- Extensão `.orZero()` para tratar `Int?` em cálculos de contagem de relacionamentos

---

## Internacionalização (i18n)

- **Todas** as strings visíveis ao usuário estão em `res/values/strings.xml`
- Suporte a 3 idiomas: **pt** (padrão), **en** (`values-en/`), **es** (`values-es/`)
- Formato de string com substituição via `%s` / `%1$d` / `%2$d`
- Strings de debug/log nunca passam por recursos; ficam inline no Kotlin

---

## Testes

| Tipo | Framework | Localização |
|---|---|---|
| Unitários | JUnit 4, Mockito 5, Mockito-Kotlin | `app/src/test/` |
| Coroutines | `kotlinx-coroutines-test` | `app/src/test/` |
| Instrumentados | Espresso, Compose UI Test | `app/src/androidTest/` |

Para testes instrumentados que dependem de Hilt, usar `@HiltAndroidTest` na classe de teste
e declarar `HiltAndroidRule` como `@get:Rule`. O `testInstrumentationRunner` deve ser
configurado para `HiltTestRunner` quando ViewModels injetados estiverem em escopo.

---

## Checklist de Melhorias de Código

- [x] **Introduzir ViewModels por feature**: ViewModels com `@HiltViewModel` e `StateFlow<T>`
  implementados em todos os módulos `feature/*`, separando lógica de estado da camada de UI
- [x] **Eliminar acesso direto ao `AppGraph` em Activities**: `MainActivity` agora recebe
  `AppRepository` via `@Inject` gracias ao Hilt; composables ainda usam `AppGraph` como bridge
  até a migração para ViewModels
- [ ] **Adotar `UiState` data class por tela**: substituir múltiplos `mutableStateOf` soltos
  por um único estado coeso que representa loading/success/error
- [ ] **Criar interface para `AppRepository`**: facilita mocking em testes unitários e permite
  múltiplas implementações (fake, remote-only, etc.)
- [ ] **Padronizar tratamento de erros na UI**: atualmente erros de repositório não chegam à UI
  de forma uniforme — definir um `Snackbar`/`Toast` handler global ou padrão de UiEvent
- [ ] **Remover uso de `!!` nos pontos identificados**: substituir por `checkNotNull`
  com mensagem descritiva ou pelo operador `?: return`
- [ ] **Ativar lint e detekt** no pipeline de CI com regras mínimas para `WildcardImport`,
  `MagicNumber`, `LongMethod`, `LongParameterList`
- [ ] **Documentar funções públicas do repositório**: completar KDoc de todas as funções
  `observe*` e `suspend fun` de mutação com `@param` e `@return`
- [ ] **Aumentar cobertura de testes unitários** do `AppRepository`, `AccessPolicy` e
  `EntityLifecycleRules`: são classes com lógica pura, ideais para teste unitário
- [ ] **Adicionar testes de snapshot** para os principais Composables usando
  `paparazzi` ou a API de screenshot do Compose Test
