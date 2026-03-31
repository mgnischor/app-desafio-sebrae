# Design — Telas e Interface Visual

## Sistema de Design

O aplicativo adota o **Material Design 3 (Material You)** via `androidx.compose.material3`,
com personalização completa da paleta de cores e tipografia. O tema é gerenciado pelo
`AppDesafioSEBRAETheme`, que suporta alternância dinâmica entre modo **claro** e **escuro**
controlada pela configuração `AppSettings.darkMode`.

---

## Paleta de Cores

### Primária — Deep Blue

| Token | Hex | Uso |
|---|---|---|
| `Primary10` | `#001D36` | Container escuro (dark) |
| `Primary20` | `#003258` | onPrimary (dark) |
| `Primary30` | `#00497D` | primaryContainer (dark) |
| `Primary40` | `#0061A4` | Cor primária (light) |
| `Primary80` | `#9ECAFF` | Cor primária (dark) |
| `Primary90` | `#D1E4FF` | primaryContainer (light) |

### Secundária — Teal

| Token | Hex | Uso |
|---|---|---|
| `Secondary40` | `#516474` | Cor secundária (light) |
| `Secondary80` | `#B5C9D8` | Cor secundária (dark) |
| `Secondary90` | `#D1E5F5` | secondaryContainer (light) |

### Terciária — Amber Accent

| Token | Hex | Uso |
|---|---|---|
| `Tertiary40` | `#8B5800` | Cor terciária (light) — destaque quente |
| `Tertiary80` | `#FFBA4D` | Cor terciária (dark) — destaque quente |
| `Tertiary90` | `#FFDEA8` | tertiaryContainer (light) |

### Erro

| Token | Hex | Uso |
|---|---|---|
| `Error40` | `#BA1A1A` | Erro (light) |
| `Error80` | `#FFB4AB` | Erro (dark) |

### Neutros (Background / Surface)

| Token | Hex | Uso |
|---|---|---|
| `Neutral98` | `#F8FBFF` | Background / Surface (light) |
| `Neutral6` | `#121316` | Background / Surface (dark) |
| `Neutral10` | `#1A1C1E` | onBackground (light) |
| `Neutral87` | `#DDE3EA` | onBackground (dark) |

---

## Esquemas de Cores — Mapeamento Material 3

### Modo Claro (`LightColorScheme`)

```
primary            → #0061A4  (Deep Blue)
primaryContainer   → #D1E4FF
secondary          → #516474  (Teal)
secondaryContainer → #D1E5F5
tertiary           → #8B5800  (Amber)
tertiaryContainer  → #FFDEA8
background         → #F8FBFF
surface            → #F8FBFF
error              → #BA1A1A
```

### Modo Escuro (`DarkColorScheme`)

```
primary            → #9ECAFF  (Blue claro)
primaryContainer   → #00497D
secondary          → #B5C9D8
background         → #121316
surface            → #121316
error              → #FFB4AB
```

---

## Tipografia

A tipografia usa `FontFamily.SansSerif` como fonte base local, evitando dependência de
Google Play Services em runtime.

| Escala | Peso | Tamanho | Uso |
|---|---|---|---|
| `displayLarge` | Light | 57sp | Títulos hero |
| `headlineLarge` | SemiBold | 32sp | Cabeçalhos de seção |
| `headlineMedium` | SemiBold | 28sp | Títulos de tela |
| `titleLarge` | Bold | 22sp | Títulos de card |
| `titleMedium` | Medium | 16sp | TopAppBar, headers |
| `titleSmall` | Medium | 14sp | Subtítulos |
| `bodyLarge` | Normal | 16sp | Texto principal |
| `bodyMedium` | Normal | 14sp | Texto de suporte |
| `labelLarge` | Medium | 14sp | Botões |
| `labelSmall` | Medium | 11sp | Chips, badges |

---

## Telas do Aplicativo

### Login (`LoginScreen`)

- Fundo com gradiente diagonal usando `Primary40` → `Primary30` → `Primary20`
- Card centralizado com bordas arredondadas contendo os campos de entrada
- Ícone da aplicação (shield/logo) no topo do card
- Campo e-mail com ícone de envelope (`MailOutline`)
- Campo senha com alternância de visibilidade (`Visibility` / `VisibilityOff`)
- Botão primário "Entrar" em largura total; exibe estado de loading com texto "Entrando…"
- Erros de autenticação exibidos via `Snackbar` na borda inferior

---

### Home (`HomeScreen`)

- `TopAppBar` com comportamento `exitUntilCollapsed` (colapsa ao rolar)
- Saudação dinâmica por período do dia (bom dia / boa tarde / boa noite)
- Nome e cargo do usuário autenticado no cabeçalho
- Ícone de notificações com `Badge` de contagem pendente
- Campo de busca (`OutlinedTextField`) para filtrar módulos, alunos e cursos
- **Seção Visão Geral**: linha com 4 `QuickStat` cards (Alunos, Cursos, Turmas, Conclusão)
  com indicador de fonte de dados (`stat_data_source`)
- **Seção Módulos**: `LazyVerticalGrid` com 2 colunas; cards com ícone + título + descrição +
  badge opcional. Visibilidade filtrada por `UserRole`
- **Seção Atividade Recente**: `LazyColumn` com os últimos eventos; botão "Ver tudo" navega
  para `RecentActivitiesScreen`

#### Módulos visíveis por papel

| Módulo | PROFESSOR | COORDENADOR | ADMINISTRADOR |
|---|---|---|---|
| Alunos | ✅ | ✅ | ✅ |
| Cursos | ✅ | ✅ | ✅ |
| Turmas | ✅ | ✅ | ✅ |
| Instrutores | ❌ | ✅ | ✅ |
| Relatórios | ❌ | ✅ | ✅ |
| Certificados | ✅ | ✅ | ✅ |
| Calendário | ✅ | ✅ | ✅ |
| Configurações | ✅ | ✅ | ✅ |

---

### Padrão de Telas de Lista (Cursos, Alunos, Turmas, Instrutores)

Todas as telas de lista compartilham o mesmo layout base via `DetailScaffold`:

- `TopAppBar` com botão voltar e botão de ação "+" (criar novo registro) — condicionado por
  `AccessPolicy`
- `ListSearchHeader` fixo abaixo da barra com campo de busca textual
- `LazyColumn` com cards de cada entidade
- `EmptyState` quando a lista filtrada está vazia
- Navegação para tela de detalhe ao tocar no card
- FAB ou ação na toolbar para criação (quando o papel permite)

---

### Acompanhamento de Aluno (`StudentMonitoringScreen`)

Tela com abas ou seções scrolláveis cobrindo:

- **Frequência**: histórico de presença com `AttendanceStatus` (Presente, Ausente, Justificado, Atrasado)
- **Comportamento**: participação (1–5), entrega de atividade, nota, observações do professor
- **Necessidades pedagógicas**: tipo, descrição, prazo, acomodações ativas
- **Necessidades psicológicas**: resumo com nível de confidencialidade (`ConfidentialityLevel`)
- **Contatos com responsáveis**: canal, resultado, responsável, observações

---

### Detalhe de Curso (`CourseDetailScreen`) e Turma (`ClassDetailScreen`)

- Cabeçalho com título, categoria/curso vinculado, instrutor
- Indicadores numéricos: total de alunos, carga horária / capacidade máxima
- Barra de progresso de conclusão (`LinearProgressIndicator`)
- Seção de ações condicionada por `AccessPolicy`: editar, desativar, reativar

---

### Calendário (`CalendarScreen`)

- Listagem de eventos agrupados por data
- `StatusChip` colorido por tipo de evento (`Class`, `Exam`, `Meeting`, `Other`)
- Filtro por tipo visível no topo

---

### Relatórios (`ReportsScreen`)

- Cards de métricas agregadas: alunos ativos, cursos ativos, total de turmas,
  taxa média de conclusão, certificados emitidos, avaliação média dos instrutores
- Gráfico de matrículas mensais (`MonthlyEnrollmentMetric`)
- Ranking de cursos por taxa de conclusão (`CourseCompletionMetric`)

---

### Configurações (`SettingsScreen`)

- Toggle para modo escuro (`darkMode`)
- Toggle para notificações push (`pushEnabled`)
- Toggle para notificações por e-mail (`emailEnabled`)
- Seleção de idioma (pt / en / es)
- Botão "Gestão de Usuários" visível apenas para `ADMINISTRADOR`
- Botão "Limpar armazenamento local" para `ADMINISTRADOR`

---

### Gestão de Usuários (`UserManagementScreen`)

- Lista de usuários cadastrados com nome, e-mail e papel (chip colorido por role)
- Opção de criar novo usuário (admin-only)
- Opção de editar papel de usuário existente

---

## Componentes Reutilizáveis

### `DetailScaffold`

```
┌────────────────────────────────┐
│ ← Título da Tela    [ações]    │  ← TopAppBar (scroll: exitUntilCollapsed)
├────────────────────────────────┤
│                                │
│         content(padding)       │
│                                │
└────────────────────────────────┘
```

### `StatusChip`

Chip com ícone e cor semântica baseada no status da entidade:

| Status | Cor |
|---|---|
| Active / Open / Published | `tertiary` (Amber) |
| InProgress | `primary` (Blue) |
| Inactive / Closed | `error` |
| Graduated | `secondary` (Teal) |

### `EmptyState`

Ícone centralizado + título + subtítulo descritivo. Exibido quando listas filtradas retornam
zero resultados.

### `ListSearchHeader`

`OutlinedTextField` em largura total com ícone de busca e placeholder localizado.

---

## Edge-to-Edge e Status Bar

`MainActivity` chama `enableEdgeToEdge()` para suporte a conteúdo sob a status bar.
O `AppDesafioSEBRAETheme` ajusta a aparência da status bar com `WindowCompat`:

```kotlin
val view = LocalView.current
SideEffect {
    val window = (view.context as Activity).window
    window.statusBarColor = colorScheme.primary.toArgb()
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
}
```

---

## Checklist de Melhorias de Design

- [ ] **Criar tela de onboarding/splash**: exibir logo e animação de carregamento enquanto o
  grafo de dependências aquece, evitando tela em branco no cold start
- [ ] **Implementar Dynamic Color (Material You)**: usar `dynamicLightColorScheme` /
  `dynamicDarkColorScheme` em Android 12+, com fallback para a paleta fixa atual
- [ ] **Padronizar o `StatusChip`**: definir mapeamento official de cor por status para todas
  as entidades e documentar no design system interno
- [ ] **Adicionar estado de loading nas telas de lista**: exibir `CircularProgressIndicator` ou
  skeleton loader enquanto o `Flow` emite o primeiro valor
- [ ] **Criar tela de erro genérica**: exibir mensagem amigável + botão de retry quando
  a sincronização Firebase falha
- [ ] **Revisar acessibilidade (a11y)**: adicionar `contentDescription` em todos os ícones
  interativos e verificar contraste WCAG AA na paleta de cores
- [x] **Implementar gráficos interativos em Relatórios**: substituir listagem de métricas
  por gráfico de barras (matriculas mensais) e gráfico de pizza (distribuição de status)
  com biblioteca como Vico ou MPAndroidChart para Compose
- [ ] **Adicionar animações de transição entre telas**: usar `AnimatedNavHost` com
  `EnterTransition` / `ExitTransition` para suavizar a navegação
- [ ] **Suporte a tablets e telas grandes**: adaptar o layout para usar `NavigationRail`
  ou `NavigationDrawer` em janelas com largura > 600dp
- [ ] **Padronizar espaçamentos em sistema de tokens**: criar objeto `AppSpacing` com valores
  em `dp` padronizados (4, 8, 12, 16, 24, 32) para substituir `dp` hardcoded espalhado
  nas telas
