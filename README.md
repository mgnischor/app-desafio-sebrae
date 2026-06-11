# Educalink

Aplicativo Android nativo para gestão educacional, com foco em acompanhamento de alunos, cursos, turmas, instrutores e indicadores operacionais. O projeto foi desenvolvido com Kotlin, Jetpack Compose, Room e integração com Firebase (Auth + Firestore).

## Sumário
- [Visão geral](#visão-geral)
- [Principais funcionalidades](#principais-funcionalidades)
- [Arquitetura do projeto](#arquitetura-do-projeto)
- [Fluxos principais da aplicação](#fluxos-principais-da-aplicação)
- [Estrutura de diretórios](#estrutura-de-diretórios)
- [Tecnologias e dependências](#tecnologias-e-dependências)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do ambiente](#configuração-do-ambiente)
- [Como executar](#como-executar)
- [Build, lint e testes](#build-lint-e-testes)
- [Observações de segurança](#observações-de-segurança)

## Visão geral
O **Educalink** organiza, em uma única experiência, as operações acadêmicas e administrativas de uma instituição de ensino:

- Gestão de alunos, cursos, turmas e instrutores
- Acompanhamento individual do aluno
- Relatórios, certificados e calendário
- Controle de acesso por perfil de usuário

A aplicação adota abordagem **local-first** com persistência em Room e integrações com Firebase para autenticação e sincronização remota.

## Principais funcionalidades
As funcionalidades são expostas por módulos navegáveis na Home e por rotas centralizadas no projeto:

- **Login e sessão de usuário**
- **Home com módulos e indicadores rápidos**
- **Alunos**: listagem, cadastro e monitoramento
- **Cursos**: listagem, cadastro e detalhamento
- **Turmas**: listagem, cadastro e detalhamento
- **Instrutores**: listagem, cadastro e detalhamento
- **Empresas**: listagem e detalhamento
- **Relatórios**
- **Certificados**
- **Calendário**
- **Configurações** com importação de dados via CSV
- **Gestão de usuários** (acesso administrativo)

## Arquitetura do projeto
O projeto adota uma estrutura **multi-módulo** com separação clara entre UI, navegação, regras de domínio, persistência local e integração remota. Os módulos são:

- **`app`** — ponto de entrada, configuração Hilt, grafo de navegação
- **`core`** — código compartilhado: dados, domínio, componentes de UI e tema
- **`feature/*`** — módulos de funcionalidade independentes (login, home, students, courses, classes, teachers, companies, reports, certificates, calendar, settings, users)

### 1) Camada de apresentação (UI)
Implementada com **Jetpack Compose**, responsável por renderização, interação e navegação entre telas. Cada módulo `feature` contém seus próprios `Screen` e `ViewModel`.

- Navegação centralizada: `app/navigation/AppNavHost.kt`
- Componentes e tema compartilhados: `core/ui/`

### 2) Camada de domínio
Use cases reativos que encapsulam regras de negócio e expõem `Flow` para a UI.

- Pacote: `core/domain/usecase/`
- Exemplos: `ObserveStudentsUseCase`, `ObserveCoursesUseCase`, `SyncScreenDataUseCase`

### 3) Camada de dados
Concentra regras de acesso e transformação de dados, localizada no módulo `core`.

- **Repository**: `core/data/repository/AppRepository.kt`
  - Exposição de `Flow` para consumo reativo na UI
  - CRUD e composições de métricas
- **AppGraph**: `app/data/repository/AppGraph.kt`
  - Acesso a dependências Hilt em contextos não-Hilt via `EntryPoint`

### 4) Injeção de dependências
**Hilt** gerencia o ciclo de vida de todas as dependências singleton.

- Módulo de DI: `app/di/AppModule.kt`

### 5) Persistência local
Persistência via **Room** + SQLite.

- Banco e entidades: `core/data/local/AppDatabase.kt`
- DAOs e consultas: `AppDao` no mesmo arquivo
- Base local: `sebrae_local.db`

### 6) Monitoramento de conectividade
Observação reativa do estado de rede do dispositivo.

- `core/data/connectivity/ConnectivityObserver.kt`
- `core/data/connectivity/NetworkConnectivityObserver.kt`

### 7) Autenticação e dados remotos
Integração com Firebase para autenticação e sincronização.

- Autenticação: `core/data/auth/AuthManager.kt`
  - Fluxo principal com Firebase Auth
  - Política de acesso por perfil: `AccessPolicy.kt`
  - Fallback local com credenciais seed para uso em ambiente local
- Remoto: `core/data/remote/firebase/`
  - `FirebaseDataConnectService.kt`
  - `FirebaseRemoteBootstrapper.kt`
  - `FirebaseScreenSyncPlanner.kt`
  - `FirebaseSeedCredentialStore.kt`

## Fluxos principais da aplicação

### Inicialização
1. `SebraeApplication` inicializa o Hilt e chama `AppGraph.warmUp(...)`
2. `AppGraph` acessa dependências via Hilt `EntryPoint` e dispara o bootstrap remoto
3. `MainActivity` monta o Compose e inicia `AppNavHost`
4. Rota inicial: `login`

### Autenticação
1. Usuário informa e-mail/senha na tela de login
2. `AuthManager` tenta autenticar no Firebase Auth
3. Em caso de sucesso, busca perfil no Firestore (`users`)
4. Perfil é carregado na sessão (`currentUser`) e usuário navega para `home`

### Dados
- A UI observa `Flow` do repositório
- `AppRepository` consulta o `AppDao` e mapeia entidades para modelos
- Fluxos de sincronização com Firebase complementam estratégia local-first

## Estrutura de diretórios
```text
app-desafio-sebrae/
├── app/                          # Módulo principal (entry point)
│   └── src/main/java/tech/datatower/sebrae/desafio/
│       ├── data/repository/      # AppGraph (EntryPoint Hilt)
│       ├── di/                   # AppModule (Hilt)
│       ├── navigation/           # AppNavHost.kt
│       ├── MainActivity.kt
│       └── SebraeApplication.kt
├── core/                         # Módulo compartilhado
│   └── src/main/java/tech/datatower/sebrae/desafio/
│       ├── data/
│       │   ├── auth/             # AuthManager, AccessPolicy
│       │   ├── connectivity/     # ConnectivityObserver
│       │   ├── local/            # AppDatabase, AppDao
│       │   ├── model/            # Entidades de domínio
│       │   ├── remote/firebase/  # Firebase services
│       │   └── repository/       # AppRepository
│       ├── domain/usecase/       # Use cases reativos
│       └── ui/                   # Componentes e tema compartilhados
├── feature/
│   ├── calendar/
│   ├── certificates/
│   ├── classes/
│   ├── companies/
│   ├── courses/
│   ├── home/
│   ├── login/
│   ├── reports/
│   ├── settings/
│   ├── students/
│   ├── teachers/
│   └── users/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Tecnologias e dependências
- **Kotlin** 2.4.0
- **Android Gradle Plugin (AGP)** 9.1.1
- **Jetpack Compose** (BOM 2026.05.01)
- **Material 3** com Material Icons Extended
- **Navigation Compose** 2.9.8
- **Room** 2.8.4
- **Hilt** 2.59.2 (injeção de dependência)
- **Firebase Auth** e **Firebase Firestore** (via Firebase BOM 34.14.1)
- **Coroutines** 1.11.0
- **Vico** 3.2.2 (gráficos e visualizações)
- **JUnit 4** e Mockito para testes unitários

As versões são centralizadas em `gradle/libs.versions.toml`.

## Pré-requisitos
- JDK 17
- Android SDK (compileSdk 36, minSdk 26, targetSdk 36)
- Android Studio atualizado

## Configuração do ambiente
1. Crie/edite o arquivo `local.properties` na raiz do projeto.
2. Configure credenciais de seed (opcional para bootstrap):

```properties
firebase.seed.email=seu-email@dominio.com
firebase.seed.password=sua-senha
```

3. Garanta o arquivo `google-services.json` em `app/` para uso completo do Firebase.

## Como executar
```bash
./gradlew assembleDebug
```

Para instalar no dispositivo/emulador via Android Studio, execute o app normalmente pelo IDE após sincronização do Gradle.

## Build, lint e testes
Comandos principais:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint
./gradlew test
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
```

Testes unitários estão em `app/src/test/java`.

## Observações de segurança
- Não versionar `local.properties` com credenciais.
- Em ambiente de desenvolvimento, há fallback local de autenticação para usuários seed.
- Em produção, priorizar fluxo Firebase com regras adequadas no Firestore/Auth.
