# App Desafio SEBRAE

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
O **App Desafio SEBRAE** organiza, em uma única experiência, as operações acadêmicas e administrativas de uma instituição de ensino:

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
- **Relatórios**
- **Certificados**
- **Calendário**
- **Configurações**
- **Gestão de usuários** (acesso administrativo)

## Arquitetura do projeto
A arquitetura é organizada em camadas com separação clara entre UI, navegação, regras de dados, persistência local e integração remota.

### 1) Camada de apresentação (UI)
Implementada com **Jetpack Compose**, responsável por renderização, interação e navegação entre telas.

- Pacotes: `ui/*`
- Navegação: `navigation/AppNavHost.kt` e `navigation/AppRoutes.kt`

### 2) Camada de domínio/modelos
Modelos de dados imutáveis que representam as entidades do app (aluno, curso, turma, usuário etc.).

- Pacote: `data/model/*`

### 3) Camada de dados
Concentra regras de acesso e transformação de dados.

- **Repository**: `data/repository/AppRepository.kt`
  - Exposição de `Flow` para consumo reativo na UI
  - CRUD e composições de métricas
- **Graph/Service Locator**: `data/repository/AppGraph.kt`
  - Inicialização singleton de dependências
  - Criação do banco Room e serviços remotos

### 4) Persistência local
Persistência via **Room** + SQLite.

- Banco e entidades: `data/local/AppDatabase.kt`
- DAOs e consultas: `AppDao` no mesmo arquivo
- Base local: `sebrae_local.db`

### 5) Autenticação e dados remotos
Integração com Firebase para autenticação e sincronização.

- Autenticação: `data/auth/AuthManager.kt`
  - Fluxo principal com Firebase Auth
  - Fallback local com credenciais seed para uso em ambiente local
- Remoto: `data/remote/firebase/*`
  - `FirebaseDataConnectService.kt`
  - `FirebaseRemoteBootstrapper.kt`
  - `FirebaseSeedCredentialStore.kt`

## Fluxos principais da aplicação

### Inicialização
1. `SebraeApplication` chama `AppGraph.warmUp(...)`
2. `AppGraph` inicializa repositório, Room e bootstrap remoto
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
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/tech/datatower/sebrae/desafio/
│   │   │   │   ├── data/
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── local/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── remote/
│   │   │   │   │   └── repository/
│   │   │   │   ├── navigation/
│   │   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── SebraeApplication.kt
│   │   │   └── AndroidManifest.xml
│   │   ├── test/         # testes unitários (JUnit4)
│   │   └── androidTest/  # testes instrumentados
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Tecnologias e dependências
- **Kotlin** 2.3.20
- **Android Gradle Plugin (AGP)** 9.1.0
- **Jetpack Compose** (BOM 2026.03.00)
- **Material 3**
- **Navigation Compose**
- **Room** 2.8.4
- **Firebase Auth** e **Firebase Firestore** (via Firebase BOM 34.11.0)
- **Coroutines**
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
