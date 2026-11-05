# HealthTrack Agent & Engineering Guidelines

This document outlines architectural rules, UI conventions, and mandatory patterns that **all AI agents and developers must follow** when developing, updating, or maintaining screens and components in HealthTrack.

---

## 1. Soft Keyboard & Input Experience (MANDATORY FOR ALL SCREENS)

Whenever creating or modifying any screen, dialog, sheet, or form that includes text input fields (`TextField`, `OutlinedTextField`, `BasicTextField`), the following rules **MUST ALWAYS** be satisfied:

### Rule 1.1: Tap-Outside-to-Dismiss (`clearFocusOnTap`)
- When a user taps anywhere outside of an active text field (e.g. background, non-interactive cards, empty spaces), the soft keyboard must immediately dismiss and focus must be cleared.
- Every root layout container (`Box`, `Column`, `Scaffold` inner content) **must** apply `Modifier.clearFocusOnTap()`.
  ```kotlin
  import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

  Column(
      modifier = modifier
          .fillMaxSize()
          .clearFocusOnTap()
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
  ) {
      // Screen contents & input fields
  }
  ```
- *Implementation Note*: `clearFocusOnTap()` uses `PointerEventPass.Initial` and `PointerEventPass.Final` with touch-slop detection. This ensures:
  1. Scrolling / dragging does **not** dismiss the keyboard accidentally.
  2. Clicking child buttons, cards, or other text fields is **not** intercepted.
  3. Clicking unconsumed / empty areas immediately calls `focusManager.clearFocus(force = true)` and `keyboardController?.hide()`.

---

### Rule 1.2: Screen Resizing & IME Visibility (`imePadding` & `adjustResize`)
- When the soft keyboard opens, the active input field and action buttons **must never be obscured** by the keyboard. The screen must adjust and scroll up so the user can clearly see what they are typing.
- **AndroidManifest**: In `app/androidApp/src/main/AndroidManifest.xml`, `MainActivity` must always have:
  ```xml
  android:windowSoftInputMode="adjustResize"
  ```
- **Scaffold & Inner Padding**:
  When using `Scaffold(bottomBar = { ... })`:
  ```kotlin
  Scaffold(
      topBar = { ... },
      bottomBar = {
          // Hide bottom navigation or bottom bar while typing to maximize viewport
          AnimatedVisibility(
              visible = !WindowInsets.isImeVisible,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut()
          ) {
              NavigationBar(...) { ... }
          }
      }
  ) { innerPadding ->
      Box(
          modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .consumeWindowInsets(innerPadding)
              .imePadding()
              .clearFocusOnTap()
      ) {
          // Content
      }
  }
  ```
- Always chain `.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()` so keyboard insets are not double-counted with the navigation bar height.
- Screens with input fields **must** have a scrollable parent (e.g., `Modifier.verticalScroll(rememberScrollState())` or `LazyColumn`) so Compose can bring focused text fields into the visible viewport.

---

### Rule 1.3: Dialogs & Modal Bottom Sheets
- **Every `AlertDialog`** with text input fields **must** include `modifier = Modifier.imePadding()` on the dialog, and its content Column must include `Modifier.verticalScroll(rememberScrollState())`:
  ```kotlin
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.imePadding(),
      title = { Text(...) },
      text = {
          Column(
              modifier = Modifier.verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
              OutlinedTextField(...)
          }
      },
      confirmButton = { ... }
  )
  ```
- **Modal Bottom Sheets** with input fields (such as comment bars or chat composers):
  - Must apply `.navigationBarsPadding().imePadding()` to the composer container row/surface so that it floats directly above the keyboard when opened.

---

## 2. Architecture & Design Alignment

### 2.1: Planora Architectural Consistency
HealthTrack and Planora share the same visual and engineering design language:
- **Brand Identity**: Use the glassmorphic brand pod (`"H"` in HealthTrack, `"P"` in Planora) with `HealthColors.GradientPrimary`.
- **TopBar**: Keep consistent header styling with title, subtitle/handle, theme toggle (Dark/Light mode), sync trigger, and logout.
- **Dark Mode**: Dark mode is mandatory and default-supported using `HealthColors` dark palette and frosted glass cards (`GlassCard`).
- **MVI Pattern**: Screens are organized around MVI (`State`, `Intent`, `ViewModel`, `Screen`, `ScreenRoot`). Keep business logic strictly inside ViewModels and repository layers.

### 2.2: Safe API & Error Handling
- Never hardcode generic offline errors when remote network calls fail. Always check HTTP status codes and response bodies from `HealthApiClient` (e.g., 404 Render sleeping/deploying, 500 missing API key, 401 expired session) to present helpful, actionable messages to the user.

