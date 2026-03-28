# Segurança — App Desafio SEBRAE

## Visão Geral

O aplicativo adota uma estratégia de defesa em profundidade com múltiplas camadas de segurança:
autenticação delegada ao Firebase Auth, autorização via RBAC centralizado (`AccessPolicy`),
armazenamento seguro de credenciais via Android Keystore e boas práticas de segurança Android
(backup rules, edge-to-edge, verificação de permissões).

---

## 1. Autenticação

### Firebase Authentication (modo principal)

O login de produção é delegado ao **Firebase Authentication**:

```kotlin
suspend fun loginWithFirebase(email: String, password: String): FirebaseLoginResult {
    val normalizedEmail = email.trim().lowercase()
    if (normalizedEmail.isBlank() || password.isBlank()) {
        return FirebaseLoginResult.Error("Informe e-mail e senha.")
    }
    // signInWithEmailAndPassword via Firebase SDK
    firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password).await()
    // ...
}
```

**Medidas de segurança aplicadas:**
- E-mail normalizado (`trim().lowercase()`) antes de qualquer verificação, prevenindo
  bypass por variação de case ou espaços
- Validação de campos vazios **antes** da chamada ao Firebase
- Sessão Firebase encerrada com `firebaseAuth.signOut()` se o perfil no Firestore não for
  encontrado, evitando autenticação parcial
- Resultado tipado (`FirebaseLoginResult.Success` / `FirebaseLoginResult.Error`) que nunca
  expõe exceções brutas à UI

### Login Local com SHA-256 (modo fallback de desenvolvimento)

Durante desenvolvimento/demo, um mecanismo de fallback autentica contra credenciais
pré-cadastradas com hash SHA-256:

```kotlin
private val hashedCredentials = mutableMapOf(
    "professor@sebrae.edu.br"   to sha256("prof123"),
    "coordenador@sebrae.edu.br" to sha256("coord123"),
    "admin@sebrae.edu.br"       to sha256("admin123"),
)

fun login(email: String, password: String): Boolean {
    val normalizedEmail = email.trim().lowercase()
    val expectedHash = hashedCredentials[normalizedEmail]
    return if (expectedHash != null && expectedHash == sha256(password)) { ... }
}
```

SHA-256 é implementado via `java.security.MessageDigest`:

```kotlin
private fun sha256(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
```

> **Atenção**: o modo de fallback local usa credenciais hardcoded adequadas apenas para
> desenvolvimento. Em produção, apenas o Firebase Auth deve ser utilizado.

### Perfil de Usuário via Firestore

Após autenticação Firebase, o perfil do usuário é buscado na coleção `users` com
estratégia de fallback em três passos:

1. Busca por `document(authUid)` — caso padrão
2. Busca via `whereEqualTo("authUid", firebaseUid)` — documentos com campo authUid
3. Busca via `whereEqualTo("email", email)` — fallback por e-mail

Se nenhum perfil for encontrado, o login é **abortado** e a sessão Firebase é encerrada.

---

## 2. Autorização (RBAC)

### Papéis de Usuário

```
PROFESSOR       → acesso mínimo (leitura de turmas/cursos/alunos, calendário, certificados)
COORDENADOR     → acesso intermediário (criar/editar alunos, cursos, turmas, instrutores)
ADMINISTRADOR   → acesso completo (inclui gestão de usuários, configurações avançadas)
```

### AccessPolicy — Regras Centralizadas

Todas as decisões de autorização passam pelo `AccessPolicy.can()`:

```kotlin
fun can(role: UserRole?, resource: ProtectedResource, action: ProtectedAction): Boolean {
    if (role == null) return false  // sem sessão = sem acesso
    return when (role) {
        UserRole.ADMINISTRADOR -> true
        UserRole.COORDENADOR   -> coordinatorRules(resource, action)
        UserRole.PROFESSOR     -> professorRules(resource, action)
    }
}
```

### Matriz de Permissões

| Recurso | Ação | PROFESSOR | COORDENADOR | ADMINISTRADOR |
|---|---|---|---|---|
| Students | View | ✅ | ✅ | ✅ |
| Students | Create / Update | ❌ | ✅ | ✅ |
| Students | Deactivate / Reactivate | ❌ | ✅ | ✅ |
| Students | LaunchStudentRecord | ✅ | ✅ | ✅ |
| Students | Delete | ❌ | ❌ | ✅ |
| Courses / Classes | View | ✅ | ✅ | ✅ |
| Courses / Classes | Create / Update / Deactivate | ❌ | ✅ | ✅ |
| Teachers | View | ❌ | ✅ | ✅ |
| Teachers | Create / Update / Deactivate | ❌ | ✅ | ✅ |
| Calendar | View | ✅ | ✅ | ✅ |
| Calendar | Create | ❌ | ✅ | ✅ |
| Settings | View / ChangeOwnPassword | ✅ | ✅ | ✅ |
| Settings | ClearStorage | ❌ | ❌ | ✅ |
| Users | Qualquer ação | ❌ | ❌ | ✅ |

### Aplicação na UI e no Serviço Remoto

O `AccessPolicy` é invocado em dois pontos:

1. **UI**: botões, FABs e ações are conditionally renderizados com base no papel do usuário —
   evitar exibir ações que o usuário não pode executar
2. **`FirebaseDataConnectService`**: antes de executar mutações no Firestore, o serviço
   verifica a permissão:

```kotlin
if (!AccessPolicy.can(currentRole, ProtectedResource.Students, ProtectedAction.Create)) {
    return Result.Error(SecurityException("Permissão negada"), "Sem permissão para criar aluno.")
}
```

---

## 3. Armazenamento Seguro de Credenciais

### Android Keystore + AES/GCM (`FirebaseSeedCredentialStore`)

Credenciais de seed para auto-login do Firebase são cifradas com **AES-256-GCM** via
**Android Keystore**, garantindo que a chave criptográfica nunca saia do hardware seguro:

```kotlin
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "SeedCredentialKey"
```

**Fluxo de proteção:**

```
Credencial (plaintext)
        │
        ▼
AES/GCM encrypt  ←── chave no Android Keystore (hardware-backed)
        │
        ▼
Base64(ciphertext) + Base64(IV)
        │
        ▼
SharedPreferences (apenas dados cifrados, nunca plaintext)
```

**Para descriptografar:**
- A chave é recuperada do Keystore — não pode ser exportada
- IV é armazenado junto ao ciphertext (não é segredo)
- GCM autentica a integridade do dado, detectando adulteração

**Geração da chave:**
```kotlin
KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .build()
```

---

## 4. Permissões Android

O `AndroidManifest.xml` declara apenas a permissão estritamente necessária:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Nenhuma permissão de localização, câmera, contatos ou armazenamento externo é solicitada,
seguindo o **princípio do mínimo privilégio** para permissões Android.

---

## 5. Backup e Extração de Dados

O `AndroidManifest.xml` referencia regras customizadas de backup:

```xml
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```

Estas regras controlam quais dados do aplicativo podem ser incluídos em backups
Google/ADB, evitando que dados sensíveis (credenciais, dados de alunos) sejam
extraídos via backup.

---

## 6. Gerenciamento de Sessão

- O `AuthManager` mantém `_currentUser` como `MutableStateFlow<AppUser?>` — `null` indica
  sessão encerrada
- Logout limpa a sessão local E chama `firebaseAuth.signOut()`:

```kotlin
fun logout() {
    _currentUser.value = null
    firebaseAuth.signOut()
}
```

- Após logout, o `AppNavHost` navega para login removendo o `HOME` do back-stack
  (`popUpTo(HOME) { inclusive = true }`), impedindo acesso via botão voltar

---

## 7. Segurança no Transporte

- Toda comunicação com Firebase (Firestore e Auth) usa **TLS/HTTPS** por padrão via SDK
- Nenhuma URL HTTP insegura é utilizada no código
- Nenhuma chave de API ou segredo está hardcoded no código-fonte; apenas o
  `google-services.json` (não versionado em produção) contém identificadores de projeto

---

## 8. Proteção contra Injeção

- Room usa **parâmetros vinculados** em todas as queries (`@Query` com `:param`),
  protegendo contra SQL injection
- Nenhuma query SQL é construída por concatenação de strings

---

## 9. Hashing de Senhas no Banco Local

A tabela `app_users` armazena `passwordHash` (SHA-256 hex), nunca a senha em texto plano.
O mesmo algoritmo é usado na verificação local:

```kotlin
val expectedHash = hashedCredentials[normalizedEmail]
expectedHash != null && expectedHash == sha256(password)
```

---

## 10. Mínimo de Dados Sincronizados

O `FirebaseScreenSyncPlanner` aplica **least-privilege de dados**: cada tela sincroniza
apenas as coleções que precisa, reduzindo exposição de dados desnecessários em trânsito
e limitando o que fica em cache local.

---

## Checklist de Melhorias de Segurança

- [ ] **Remover credenciais hardcoded do fallback local**: as senhas `prof123`, `coord123`,
  `admin123` nunca devem existir em código de produção — desabilitar completamente o
  `AuthManager.login()` local em builds de release com `BuildConfig.DEBUG`
- [ ] **Adicionar salt ao hash SHA-256**: o hash atual sem salt é vulnerável a ataques de
  rainbow table — usar PBKDF2 ou bcrypt com salt aleatório para o hash do `passwordHash`
  local
- [ ] **Implementar Certificate Pinning**: fixar o certificado TLS do Firebase para prevenir
  ataques MITM em redes comprometidas, usando `OkHttp CertificatePinner` ou
  Network Security Config
- [ ] **Habilitar ProGuard/R8 em release**: o `proguard-rules.pro` existe mas a configuração
  de ofuscação deve ser revisada e ativada para builds de produção, dificultando engenharia
  reversa
- [ ] **Criptografar banco de dados local com SQLCipher**: dados sensíveis de alunos
  (registros psicológicos, necessidades pedagógicas) armazenados no Room devem ser
  criptografados em repouso usando SQLCipher para Android
- [ ] **Implementar detecção de root / emulador**: bloquear ou alertar o usuário se o
  dispositivo estiver com root (use SafetyNet/Integrity API), reduzindo risco de extração
  de dados pelo sistema de arquivos
- [ ] **Adicionar timeout de sessão**: implementar auto-logout após período de inatividade,
  especialmente relevante dado que o app gerencia dados sensíveis de alunos
- [ ] **Revisar regras do Firestore Security Rules**: as `Security Rules` do Firestore devem
  refletir exatamente a matriz de permissões do `AccessPolicy`, evitando que requisições
  diretas à API do Firestore (fora do app) contornem a autorização
- [ ] **Ocultar senha no `google-services.json`**: garantir que `google-services.json` esteja
  no `.gitignore` do repositório de produção e usar CI/CD secrets para injetá-lo no build
- [ ] **Adicionar verificação de integridade do APK**: habilitar Play Integrity API para
  verificar que o app não foi modificado ou distribuído fora do Play Store
