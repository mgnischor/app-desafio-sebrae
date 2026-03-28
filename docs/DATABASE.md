# Banco de Dados — App Desafio SEBRAE

## Visão Geral

O aplicativo usa uma estratégia **local-first**: o Room SQLite atua como cache principal
de todos os dados, e o Firebase Firestore é a fonte remota que alimenta esse cache sob
demanda. A UI nunca lê do Firestore diretamente — toda leitura é feita via Room.

| Componente | Tecnologia | Função |
|---|---|---|
| Cache local | Room 2.8.4 + SQLite | Fonte de dados da UI |
| Backend remoto | Firebase Firestore | Sincronização remota |
| Autenticação de usuários | Firebase Auth | Identity de login |
| Credenciais de seed | SharedPreferences + Android Keystore | Armazenamento seguro |

---

## Banco de Dados Local (Room)

- **Arquivo**: `sebrae_local.db`
- **Criação**: `Room.databaseBuilder` com `fallbackToDestructiveMigration(true)`
- **Classe**: `AppDatabase` (extends `RoomDatabase`)
- **Versão atual**: implícita via `@Database`

### Type Converters (`AppConverters`)

Como o Room não suporta campos de tipo complexo nativamente, os seguintes conversores
são registrados em `AppConverters`:

| Tipo Kotlin | Armazenado como | Converter |
|---|---|---|
| `LocalDate` | `String` (ISO-8601) | `localDateToString` / `stringToLocalDate` |
| `List<String>` | `String` (JSON array) | `accommodationsToString` / `stringToAccommodations` |
| `StudentStatus` | `String` | enum→string / string→enum |
| `ClassStatus` | `String` | enum→string / string→enum |
| `AttendanceStatus` | `String` | enum→string / string→enum |
| `ActivityDeliveryStatus` | `String` | enum→string / string→enum |
| `PedagogicalNeedType` | `String` | enum→string / string→enum |
| `PsychologicalNeedType` | `String` | enum→string / string→enum |
| `ConfidentialityLevel` | `String` | enum→string / string→enum |
| `ParentContactChannel` | `String` | enum→string / string→enum |
| `ParentFollowUpStatus` | `String` | enum→string / string→enum |
| `EventType` | `String` | enum→string / string→enum |
| `UserRole` | `String` | enum→string / string→enum |

---

## Tabelas Locais (Room Entities)

### `courses`

Armazena os cursos oferecidos pela instituição.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `title` | `String` | TEXT | NOT NULL |
| `category` | `String` | TEXT | NOT NULL |
| `instructor` | `String` | TEXT | NOT NULL |
| `totalStudents` | `Int` | INTEGER | NOT NULL |
| `durationHours` | `Int` | INTEGER | NOT NULL |
| `completionRate` | `Float` | REAL | NOT NULL (0.0–1.0) |
| `isPublished` | `Boolean` | INTEGER | NOT NULL |

---

### `school_classes`

Representa turmas vinculadas a cursos.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `name` | `String` | TEXT | NOT NULL |
| `course` | `String` | TEXT | NOT NULL |
| `instructor` | `String` | TEXT | NOT NULL |
| `studentsCount` | `Int` | INTEGER | NOT NULL |
| `maxCapacity` | `Int` | INTEGER | NOT NULL |
| `schedule` | `String` | TEXT | NOT NULL |
| `status` | `ClassStatus` | TEXT | NOT NULL — via TypeConverter |

`ClassStatus`: `Open`, `InProgress`, `Closed`

---

### `teachers`

Gerencia o cadastro de instrutores.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `name` | `String` | TEXT | NOT NULL |
| `email` | `String` | TEXT | NOT NULL |
| `specialty` | `String` | TEXT | NOT NULL |
| `activeCourses` | `Int` | INTEGER | NOT NULL |
| `totalStudents` | `Int` | INTEGER | NOT NULL |
| `rating` | `Float` | REAL | NOT NULL (0.0–5.0) |
| `isActive` | `Boolean` | INTEGER | NOT NULL |

---

### `students`

Alunos matriculados no sistema.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `name` | `String` | TEXT | NOT NULL |
| `email` | `String` | TEXT | NOT NULL |
| `course` | `String` | TEXT | NOT NULL |
| `enrolledClass` | `String` | TEXT | NOT NULL |
| `progress` | `Float` | REAL | NOT NULL (0.0–1.0) |
| `status` | `StudentStatus` | TEXT | NOT NULL — via TypeConverter |

`StudentStatus`: `Active`, `Inactive`, `Graduated`

---

### `certificates`

Certificados emitidos para conclusão de cursos.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `studentName` | `String` | TEXT | NOT NULL |
| `courseName` | `String` | TEXT | NOT NULL |
| `issuedDate` | `String` | TEXT | NOT NULL |
| `hours` | `Int` | INTEGER | NOT NULL |
| `code` | `String` | TEXT | NOT NULL — código único de validação |

---

### `calendar_events`

Eventos da agenda acadêmica.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `title` | `String` | TEXT | NOT NULL |
| `course` | `String` | TEXT | NOT NULL |
| `date` | `String` | TEXT | NOT NULL |
| `time` | `String` | TEXT | NOT NULL |
| `location` | `String` | TEXT | NOT NULL |
| `type` | `EventType` | TEXT | NOT NULL — via TypeConverter |

`EventType`: `Class`, `Exam`, `Meeting`, `Other`

---

### `recent_activities`

Feed de atividades recentes exibido na Home.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `title` | `String` | TEXT | NOT NULL |
| `subtitle` | `String` | TEXT | NOT NULL |
| `iconKey` | `String` | TEXT | NOT NULL — chave para resolução de ícone |
| `timeLabel` | `String` | TEXT | NOT NULL |

---

### `monthly_enrollments`

Métricas de matrículas mensais para o gráfico de relatórios.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `month` | `String` | TEXT | PRIMARY KEY (ex: "2025-03") |
| `count` | `Int` | INTEGER | NOT NULL |

---

### `attendance_records`

Registros de frequência do diário de aula por aluno.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `studentId` | `Int` | INTEGER | NOT NULL — FK lógica para `students.id` |
| `date` | `LocalDate` | TEXT | NOT NULL — via TypeConverter |
| `status` | `AttendanceStatus` | TEXT | NOT NULL — via TypeConverter |
| `minutesLate` | `Int` | INTEGER | NOT NULL |
| `justification` | `String?` | TEXT | nullable |

`AttendanceStatus`: `Present`, `Absent`, `JustifiedAbsence`, `Late`

---

### `behavior_records`

Registros de comportamento e desempenho acadêmico por aluno por aula.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `studentId` | `Int` | INTEGER | NOT NULL |
| `date` | `LocalDate` | TEXT | NOT NULL — via TypeConverter |
| `participationScore` | `Int` | INTEGER | NOT NULL (1–5) |
| `activityDelivery` | `ActivityDeliveryStatus` | TEXT | NOT NULL — via TypeConverter |
| `delayMinutes` | `Int` | INTEGER | NOT NULL |
| `grade` | `Float?` | REAL | nullable (0.0–10.0) |
| `note` | `String` | TEXT | NOT NULL |

`ActivityDeliveryStatus`: `OnTime`, `Late`, `Missing`

---

### `pedagogical_needs`

Necessidades pedagógicas formalizadas por aluno.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `studentId` | `Int` | INTEGER | NOT NULL |
| `type` | `PedagogicalNeedType` | TEXT | NOT NULL — via TypeConverter |
| `description` | `String` | TEXT | NOT NULL |
| `expiresAt` | `LocalDate?` | TEXT | nullable — via TypeConverter |
| `accommodations` | `List<String>` | TEXT | NOT NULL — JSON via TypeConverter |

`PedagogicalNeedType`: `Report`, `MedicalCertificate`, ... (outros tipos definidos no enum)

---

### `psychological_needs`

Necessidades psicológicas com controle de confidencialidade.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `studentId` | `Int` | INTEGER | NOT NULL |
| `summary` | `String` | TEXT | NOT NULL |
| `confidentiality` | `ConfidentialityLevel` | TEXT | NOT NULL — via TypeConverter |
| `nextStep` | `String` | TEXT | NOT NULL |
| `reviewAt` | `LocalDate` | TEXT | NOT NULL — via TypeConverter |

`ConfidentialityLevel`: controla quais papéis podem visualizar o registro

---

### `parent_follow_ups`

Registros de contato com responsáveis pelo aluno.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `studentId` | `Int` | INTEGER | NOT NULL |
| `date` | `LocalDate` | TEXT | NOT NULL — via TypeConverter |
| `channel` | `ParentContactChannel` | TEXT | NOT NULL — via TypeConverter |
| `outcome` | `ParentFollowUpStatus` | TEXT | NOT NULL — via TypeConverter |
| `responsible` | `String` | TEXT | NOT NULL |
| `notes` | `String` | TEXT | NOT NULL |

`ParentContactChannel`: `Phone`, `Email`, `InPerson`, ... (canais de contato)
`ParentFollowUpStatus`: `Resolved`, `Pending`, `FollowUpNeeded`, ...

---

### `settings`

Configurações do aplicativo (single-row, id fixo = 1).

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY (valor fixo: 1) |
| `darkMode` | `Boolean` | INTEGER | NOT NULL |
| `pushEnabled` | `Boolean` | INTEGER | NOT NULL |
| `emailEnabled` | `Boolean` | INTEGER | NOT NULL |
| `language` | `String` | TEXT | NOT NULL (padrão: "pt") |

---

### `app_users`

Usuários da aplicação com hash de senha para autenticação local.

| Coluna | Tipo Kotlin | Tipo SQL | Restrições |
|---|---|---|---|
| `id` | `Int` | INTEGER | PRIMARY KEY |
| `name` | `String` | TEXT | NOT NULL |
| `email` | `String` | TEXT | NOT NULL |
| `role` | `UserRole` | TEXT | NOT NULL — via TypeConverter |
| `passwordHash` | `String` | TEXT | NOT NULL — SHA-256 hex |

`UserRole`: `PROFESSOR`, `COORDENADOR`, `ADMINISTRADOR`

---

## Diagrama de Relacionamentos (Lógicos)

```
students (id) ──< attendance_records (studentId)
students (id) ──< behavior_records (studentId)
students (id) ──< pedagogical_needs (studentId)
students (id) ──< psychological_needs (studentId)
students (id) ──< parent_follow_ups (studentId)

students.course       ──> courses.title  (relação por nome — sem FK física)
students.enrolledClass ──> school_classes.name (relação por nome — sem FK física)
courses.instructor    ──> teachers.name  (relação por nome — sem FK física)
school_classes.course ──> courses.title  (relação por nome — sem FK física)
```

> **Nota**: os relacionamentos entre cursos, turmas e alunos são resolvidos em memória no
> `AppRepository` via `RelationshipRules`, usando o nome como chave de junção. Não há
> `FOREIGN KEY` declarada no schema Room.

---

## Coleções Firebase Firestore

O Firestore espelha as mesmas entidades do banco local. As coleções são sincronizadas pelo
`FirebaseDataConnectService` de acordo com o escopo de cada tela.

| Coleção | Equivalente Room | Chave de documento |
|---|---|---|
| `users` | `app_users` | `authUid` (Firebase Auth UID) ou auto-gerado |
| `students` | `students` | ID numérico como string |
| `courses` | `courses` | ID numérico como string |
| `school_classes` | `school_classes` | ID numérico como string |
| `teachers` | `teachers` | ID numérico como string |
| `certificates` | `certificates` | ID numérico como string |
| `calendar_events` | `calendar_events` | ID numérico como string |
| `recent_activities` | `recent_activities` | ID numérico como string |
| `monthly_enrollments` | `monthly_enrollments` | Mês como string (ex: `2025-03`) |
| `attendance` | `attendance_records` | auto-ID Firestore |
| `behaviors` | `behavior_records` | auto-ID Firestore |
| `pedagogical_needs` | `pedagogical_needs` | auto-ID Firestore |
| `psychological_needs` | `psychological_needs` | auto-ID Firestore |
| `parent_follow_ups` | `parent_follow_ups` | auto-ID Firestore |
| `settings` | `settings` | UID do usuário |

### Estrutura de documento `users` no Firestore

```json
{
  "authUid": "firebase-auth-uid",
  "name": "Coord. Ana Santos",
  "email": "coordenador@sebrae.edu.br",
  "role": "COORDENADOR",
  "id": 2
}
```

---

## Fluxo de Sincronização

```
Tela abre
    │
    ▼
FirebaseScreenSyncPlanner.tasksFor(scope)
    │  retorna Set<FirebaseSyncTask>
    ▼
FirebaseDataConnectService.syncScope(scope)
    │  para cada task:
    │    1. busca coleção no Firestore
    │    2. mapeia documentos → Entities
    │    3. dao.upsert(entities)
    ▼
Room emite Flow atualizado
    │
    ▼
UI recompõe com dados frescos
```

---

## Checklist de Melhorias de Banco de Dados

- [ ] **Adicionar FKs físicas no Room**: declarar `@ForeignKey` entre `students` e as tabelas
  de acompanhamento (`attendance_records`, `behavior_records`, etc.) para garantir
  integridade referencial e habilitar `CASCADE` em deleções
- [ ] **Migrar de `fallbackToDestructiveMigration`** para migrações versionadas com
  `Migration(from, to)`, evitando perda de dados locais em atualizações de schema
- [ ] **Criar índices explícitos**: adicionar `@Index` em `studentId` de todas as tabelas
  filhas (`attendance_records`, `behavior_records`, etc.) para otimizar consultas de
  acompanhamento
- [ ] **Separar IDs numéricos locais de IDs do Firestore**: usar `String` como tipo de ID
  nas entidades locais ou adicionar coluna `firestoreId` separada para facilitar o sync
  bidirecional sem colisão de chaves
- [ ] **Implementar conflito de sync correto**: a estratégia `OnConflictStrategy.REPLACE`
  atual pode apagar campos não sincronizados. Avaliar uso de `@Upsert` com mescla
  seletiva de campos
- [ ] **Adicionar campo `updatedAt` / `syncedAt`** nas entidades principais para suportar
  sync incremental (buscar apenas documentos modificados desde o último sync)
- [ ] **Normalizar relacionamentos de nome**: substituir as chaves `course` (string)
  em `students` e `school_classes` por `courseId` (Int) e criar FK explícita, eliminando
  a dependência de `RelationshipRules` por nome
- [ ] **Implementar paginação no DAO**: telas de lista sem paginação carregam todos os
  registros de uma vez — adicionar `@Query` com `LIMIT`/`OFFSET` ou usar `PagingSource`
  do Paging 3
- [ ] **Proteger dados de monitoramento psicológico**: registros de `psychological_needs`
  com `ConfidentialityLevel` alto deveriam ser cifrados em repouso (Room + SQLCipher) antes
  de ser armazenados localmente
- [ ] **Criar testes de integração do DAO**: usar Room in-memory database
  (`Room.inMemoryDatabaseBuilder`) para cobrir operações CRUD, consultas por `studentId`
  e comportamento dos TypeConverters
