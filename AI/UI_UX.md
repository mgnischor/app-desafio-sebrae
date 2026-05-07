# UI/UX Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all user-facing interfaces in the Educalink Android application. The UI stack is **Jetpack Compose + Material 3**. All screens must be accessible, consistent, and provide clear feedback for every user action.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [Technology Stack](#2-technology-stack)
3. [Accessibility](#3-accessibility)
4. [Design System and Visual Consistency](#4-design-system-and-visual-consistency)
5. [Screen Structure and Shared Components](#5-screen-structure-and-shared-components)
6. [Feedback, States, and Loading](#6-feedback-states-and-loading)
7. [Forms and Input Handling](#7-forms-and-input-handling)
8. [Navigation](#8-navigation)
9. [Offline Handling](#9-offline-handling)
10. [RBAC-Driven UI](#10-rbac-driven-ui)
11. [Security and Privacy in the UI](#11-security-and-privacy-in-the-ui)
12. [Content and Writing Standards](#12-content-and-writing-standards)
13. [UI Testing Requirements](#13-ui-testing-requirements)
14. [UI/UX Definition of Done](#14-uiux-definition-of-done)

---

## 1. General Principles

| Principle                   | Mandatory Behavior                                                                                                                 |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **User Clarity First**      | Every screen must communicate what it shows, what state it is in, and what the user can do next.                                   |
| **Accessibility by Default**| All Composables must provide `contentDescription` for interactive elements and icon-only buttons.                                  |
| **Consistency**             | Use shared Composable components from `:core`. Never recreate visually equivalent components in feature modules.                   |
| **Feedback for Every Action** | Every user action must produce a visible, timely response (progress indicator, success message, or error feedback).              |
| **Fail Gracefully**         | Errors, empty states, and offline conditions must show clear, helpful messages — never blank screens or raw exceptions.            |
| **Domain Language**         | All labels, messages, and placeholders use domain terms as defined in [BUSINESS.md](./BUSINESS.md). Default language: PT-BR.      |
| **No Business Logic in UI** | Composable functions contain zero business logic. All logic lives in ViewModels. See [CODE.md](./CODE.md).                        |
| **Stateless Composables**   | Composables receive state as parameters and emit events via lambdas. `hiltViewModel()` is used at the screen level only.          |

---

## 2. Technology Stack

| Concern             | Technology                                       |
| ------------------- | ------------------------------------------------ |
| UI framework        | Jetpack Compose (via BOM 2026.03.01)             |
| Design system       | Material 3 (Material You)                        |
| Icons               | Material Icons Extended                          |
| Typography          | Google Fonts for Compose 1.10.6                  |
| Navigation          | Navigation Compose 2.9.7                         |
| Charts              | Vico (`compose-m3`) 3.1.0                        |
| State management    | `StateFlow` → `collectAsState()`                 |
| DI in Composables   | `hiltViewModel()` from Hilt Navigation Compose   |

---

## 3. Accessibility

### 3.1 ContentDescription Requirements

- Every `Icon` or `IconButton` that does not have visible label text **must** have a `contentDescription`.
- Images that convey meaning must have a descriptive `contentDescription`.
- Decorative images must have `contentDescription = null`.

```kotlin
// Correct
IconButton(onClick = onAddStudent) {
    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_student))
}

// FORBIDDEN
IconButton(onClick = onAddStudent) {
    Icon(Icons.Default.Add, contentDescription = null)  // ← no label
}
```

### 3.2 Semantics

- Use `Modifier.semantics {}` for custom components where role or state is not inferred by Compose.
- `StatusChip` must expose the status value via `semantics { stateDescription = status.label }`.
- Toggle states (active/inactive) must be communicated via `toggleableState` semantics.

### 3.3 Color and Contrast

- Follow Material 3 color system — use `MaterialTheme.colorScheme` tokens, never hardcoded hex values in Composables.
- Ensure minimum 4.5:1 contrast ratio for body text and 3:1 for large text/UI components.
- Status colors (active = green, inactive = gray, critical = red) must not rely solely on color — pair with text label or icon.
- The app supports dark mode (`darkMode` setting in `AppSettingsEntity`) — all components must look correct in both light and dark themes.

### 3.4 Touch Targets

- All interactive elements must have a minimum touch target of 48×48 dp.
- Use `Modifier.minimumInteractiveComponentSize()` for small interactive elements.

---

## 4. Design System and Visual Consistency

### 4.1 Theme

The app theme is defined in `:core/res/values/themes.xml` and the Compose `Theme.kt`:

- Use `MaterialTheme.colorScheme.*` for all colors.
- Use `MaterialTheme.typography.*` for all text styles.
- Use `MaterialTheme.shapes.*` for all corner radii.
- Custom colors (if any) must be declared as color tokens in `Color.kt` — never inline in Composables.

### 4.2 Typography

- App font is configured via Google Fonts for Compose.
- Body text: `MaterialTheme.typography.bodyMedium` / `bodyLarge`.
- Screen titles: `MaterialTheme.typography.headlineSmall`.
- Card headers: `MaterialTheme.typography.titleMedium`.
- Captions/labels: `MaterialTheme.typography.labelSmall`.

### 4.3 Spacing and Layout

- Use `Modifier.padding()` with `MaterialTheme.spacing` or standardized dp values (8, 16, 24).
- Do not use magic pixel values. Prefer `dp` units from `dimensionResource()` or inline dp literals.
- `LazyColumn` with `verticalArrangement = Arrangement.spacedBy(8.dp)` for list screens.

---

## 5. Screen Structure and Shared Components

Every screen must use the established shared Composable components from `:core`:

| Component           | Purpose                                                             | When to Use                              |
| ------------------- | ------------------------------------------------------------------- | ---------------------------------------- |
| `DetailScaffold`    | Scaffold with top bar, back button, and optional FAB                | All detail screens                       |
| `EmptyState`        | Illustrated empty state with optional CTA                           | When a list/feed has no items            |
| `ListSearchHeader`  | Search bar + filter toolbar for list screens                        | All list screens with search             |
| `LoadingOverlay`    | Full-screen loading indicator                                       | During initial data load                 |
| `OfflineBanner`     | Sticky banner indicating no network connection                      | All data screens when offline            |
| `SearchableDropdown`| Dropdown with search for selecting from long lists                  | Course/class/teacher selector fields     |
| `StatusChip`        | Color-coded chip displaying entity status                           | Student status, class status             |

**Rule:** Do not create new Composables that duplicate the responsibility of the above components.

### 5.1 Mandatory Screen States

Every data-driven screen must handle all three states:

```kotlin
@Composable
fun StudentsScreen(viewModel: StudentsViewModel = hiltViewModel()) {
    val students by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    OfflineBanner(visible = isOffline)

    when {
        isLoading -> LoadingOverlay()
        students.isEmpty() -> EmptyState(
            message = stringResource(R.string.no_students_found)
        )
        else -> StudentList(students = students)
    }
}
```

---

## 6. Feedback, States, and Loading

### 6.1 Loading

- `LoadingOverlay` is shown during the first data load (before content is available).
- Use `CircularProgressIndicator` inline for secondary loading within already-loaded screens (e.g., loading a student detail after list is shown).
- Do not block user interaction during background sync — show a non-blocking sync indicator.

### 6.2 Success

- Successful create/update operations show a `Snackbar` with a confirmation message.
- Successful deactivation shows `Snackbar` with undo action where applicable.

### 6.3 Error

- Network errors display a `Snackbar` or inline error with a "Retry" action.
- Validation errors on forms display inline below the offending field (not a toast or dialog).
- `SecurityException` (RBAC denial) must never surface as a raw exception — show a localized access-denied message.
- Critical errors (database corruption, unrecoverable sync failure) show a full-screen error state.

### 6.4 Empty States

- `EmptyState` must include:
  - An illustrative icon or graphic.
  - A domain-meaningful message (e.g., "Nenhum aluno cadastrado nesta empresa").
  - An optional primary action CTA (e.g., "Cadastrar aluno" button) if the user has permission.

---

## 7. Forms and Input Handling

### 7.1 Input Design Rules

- Use `OutlinedTextField` (Material 3) for all form inputs.
- Every input field must have a `label` and a `supportingText` for help or validation error.
- Required fields must be visually indicated (asterisk in label or supporting text).
- Password fields must use `KeyboardType.Password` and `PasswordVisualTransformation`.

### 7.2 Validation and Error Display

- Validation errors display inline, below the field (`isError = true` + `supportingText`).
- Validation runs on submit — do not validate on every keystroke unless UX explicitly requires it.
- The submit button is disabled (`enabled = isValid`) while required fields are empty.
- CSV import errors are shown in a scrollable validation summary before confirming the import.

### 7.3 Sensitive Data Inputs

- Password fields must use `VisualTransformation` — never show raw password text.
- Login screen credentials must not be preserved in `savedInstanceState` or `rememberSaveable`.

### 7.4 Dropdowns and Selectors

- Use `SearchableDropdown` from `:core` for course, class, teacher, and company selectors.
- All dropdowns show only items accessible to the current user's `companyId` and role.
- Deactivated entities (inactive teachers, closed classes) must not appear in selectors.

---

## 8. Navigation

- All route strings are defined in `AppRoutes` constants — never hardcoded in Composables.
- Back navigation is provided by `DetailScaffold` with a `NavigateUp` icon button.
- Logout navigates to `AppRoutes.LOGIN` with `popUpTo(AppRoutes.HOME) { inclusive = true }`.
- Bottom navigation or drawer (if added) must reflect the current user's accessible sections based on role.
- Deep links must not bypass authentication — the `AppNavHost` must validate session before rendering protected screens.

---

## 9. Offline Handling

- `ConnectivityObserver` provides a `StateFlow<ConnectivityObserver.Status>` to every ViewModel.
- `OfflineBanner` is shown on all data screens when status is not `Available`.
- Write operations (create, update, deactivate) when offline must:
  1. Write to Room (immediate, local-first).
  2. Show a "Saved locally — will sync when online" message.
  3. Queue the Firestore push for when connectivity is restored.
- Read operations always show the last cached Room data — never a blank screen due to offline state.

---

## 10. RBAC-Driven UI

- Buttons, menu items, and FABs that require elevated permissions must be hidden (not just disabled) for roles that lack access.
- Use `AccessPolicy.can(role, resource, action)` to determine UI element visibility — call this in ViewModel and expose as `StateFlow<Boolean>`.
- Do not show error messages for hidden elements — simply do not render them.
- Guardian (`RESPONSAVEL`) screens show only the linked student(s) — no list of all students.

```kotlin
// Correct: RBAC-driven visibility via ViewModel
val canCreateStudent by viewModel.canCreateStudent.collectAsState()
if (canCreateStudent) {
    FloatingActionButton(onClick = onAddStudent) { ... }
}
```

---

## 11. Security and Privacy in the UI

- Psychological records must not be displayed to users without the required role — filter in ViewModel using `AccessPolicy`.
- Student PII (name, email) must not be visible in logs or crash reports triggered from UI interactions.
- Confirmation dialogs must be shown for destructive actions (deactivate, delete, reset database).
- Session timeout UI: when Firebase Auth session expires, navigate to login screen immediately.
- Do not cache sensitive form data in `rememberSaveable`.

---

## 12. Content and Writing Standards

- **Default language:** Portuguese (PT-BR) — all string resources in `res/values/strings.xml`.
- **Additional languages:** English (`values-en/strings.xml`), Spanish (`values-es/strings.xml`).
- All user-facing strings must be externalized as string resources — never hardcoded in Kotlin.
- Messages must use domain language: "Aluno" (not "Registro"), "Turma" (not "Classe"), "Certificado" (not "Documento").
- Error messages must be actionable: "Erro ao carregar alunos. Toque para tentar novamente." (not "Error 500").
- Confirmation dialogs must state clearly what will happen: "Desativar João Silva? Esta ação poderá ser desfeita." (not "Tem certeza?").

---

## 13. UI Testing Requirements

- Every new screen must have at least one Compose UI test covering the content state.
- Test empty state and loading state for all data-driven screens.
- Test that RBAC-hidden elements are not rendered for unauthorized roles.
- Use `onNodeWithContentDescription()` to verify accessibility labels.
- Use `onNodeWithText(stringResource(...))` — never hardcode strings in test assertions.

---

## 14. UI/UX Definition of Done

A screen or component is UI-done when:

- [ ] `contentDescription` is set on all icon-only interactive elements.
- [ ] Loading, empty, and content states are all implemented and tested.
- [ ] `OfflineBanner` is shown when `ConnectivityObserver.Status != Available`.
- [ ] All user-facing strings are in `strings.xml` (PT-BR) with EN and ES translations.
- [ ] RBAC-sensitive UI elements are hidden (not disabled) for unauthorized roles.
- [ ] No business logic exists in the Composable function.
- [ ] Confirmation dialogs are shown for all destructive actions.
- [ ] The screen renders correctly in both light and dark mode.
- [ ] Psychological/sensitive data is hidden from unauthorized roles.
- [ ] At least one Compose UI test covers the screen's content state.
