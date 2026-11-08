# HealthTrack Agent & Engineering Guidelines

This document outlines architectural rules, UI conventions, Compose Preview standards, and mandatory patterns that **all AI agents and developers must follow** when developing, updating, or maintaining screens and components in HealthTrack.

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
- **Modal Bottom Sheets** with input fields (such as comment bars, raw food text input, or quick add fields):
  - Must apply `.navigationBarsPadding().imePadding()` to the composer container row/surface so that it floats directly above the keyboard when opened.

---

## 2. Architecture & MVI Separation Pattern

HealthTrack strictly adheres to the Model-View-Intent (MVI) architecture across all modules:

### 2.1: ScreenRoot vs. Stateless Screen
- **`*ScreenRoot`**:
  - Handles Koin ViewModel injection (`koinViewModel<*ViewModel>()`).
  - Collects state via `collectAsStateWithLifecycle()`.
  - Collects one-off side-effects (`Effect`) via `LaunchedEffect`.
  - Dispatches intents to the ViewModel (`viewModel.onIntent(...)`).
  - **Does NOT** contain complex UI layout code.
- **`*Screen`**:
  - **100% Pure & Stateless**.
  - Accepts `state: FeatureState`, `onIntent: (FeatureIntent) -> Unit`, and optional navigation lambdas.
  - Never accesses ViewModels, repositories, or Singletons directly.
  - This stateless contract makes the screen directly testable and previewable.

```kotlin
// Example: Screen Root
@Composable
fun FoodScreenRoot(
    viewModel: FoodViewModel = koinViewModel(),
    onNavigateToWeight: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FoodScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToWeight = onNavigateToWeight
    )
}
```

---

## 3. Compose Previews & Modular Preview Architecture (MANDATORY)

To ensure rapid UI development, prevent visual regressions, and keep IDEs fast and responsive, all screens and major components **must have dedicated Compose Previews** following these rules:

### Rule 3.1: Modular Preview Files (No Bloat, No Monoliths)
- **Do not bloat large screen files** (e.g., `ProfileScreen.kt`, `FoodScreen.kt`) with hundreds of lines of preview mock data.
- **Do not create a single giant monolith file** for all previews (which causes IDE memory spikes and cascading compile failures).
- **Place previews into dedicated feature preview files**:
  - Screens: `feature/<feature>/presentation/<Feature>Previews.kt` (e.g. `FoodPreviews.kt`, `WeightPreviews.kt`, `ProfilePreviews.kt`, `WorkoutPreviews.kt`, `AuthPreviews.kt`).
  - Shared Components: `ui/components/preview/ComponentsPreviews.kt`.

### Rule 3.2: Dark & Light Theme Previews
- Every screen preview file **must provide both Light and Dark mode** previews wrapped in `HealthTrackTheme`:
  ```kotlin
  @Preview
  @Composable
  fun FoodScreenDarkPreview() {
      HealthTrackTheme(darkTheme = true) {
          FoodScreen(state = populatedFoodState, onIntent = {})
      }
  }

  @Preview
  @Composable
  fun FoodScreenLightPreview() {
      HealthTrackTheme(darkTheme = false) {
          FoodScreen(state = populatedFoodState, onIntent = {})
      }
  }
  ```

### Rule 3.3: Multi-State Coverage
- Provide preview states covering:
  1. **Populated State**: Rich, realistic mock data with actual Turkish labels, macro targets, sparkline entries, and badges.
  2. **Empty State**: Zero entries / first-time user state (e.g. `FoodScreenEmptyPreview`, `WeightScreenEmptyPreview`).
  3. **Action / Loading States**: Where applicable (e.g. `AuthScreenLoadingPreview`).

### Rule 3.4: Pure & Isolated Execution
- Use `androidx.compose.ui.tooling.preview.Preview`.
- Never inject ViewModels, Room databases, Ktor clients, or Napier loggers into preview functions.
- Preview functions must always supply pure mock states and no-op intent lambdas `{}`.

---

## 4. Design System: Stitch Luxury & Bio-Telemetry Aesthetic

HealthTrack and Planora share a unified luxury bio-telemetry design language:

### 4.1: Brand Identity & Palette
- **Obsidian Dark & Glassmorphism**:
  - Dark mode is default-supported using `HealthColors.BackgroundDark` (`0xFF0B0F19`), `HealthColors.SurfaceDark` (`0xFF121826`), and `GlassCard`.
  - Cards use frosted translucency (`alpha = 0.82f`) and glowing multi-stop gradient borders (`Color.White.copy(0.12f)` -> `HealthColors.Primary.copy(0.08f)`).
- **Brand Pod**:
  - Glassmorphic `"H"` mark using `HealthColors.GradientPrimary`.
- **Dynamic Accents**:
  - Supports dynamic accent switching (`AppThemeColor.TEAL`, `AppThemeColor.EMERALD`) via `LocalThemeAccent.current`.

### 4.2: Hero Gauges & Telemetry
- **Concentric Calorie Hero**: `CalorieProgressGauge` centered circular ring with embedded remaining calories, burn telemetry, and daily macro capsules (Protein / Karb / Yağ) adapting dynamically to daily body weight and fitness goal.
- **7-Day Strip Selector**: `DaysStripSelector` compact timeline selector fitting cleanly above the fold without vertical bloat.
- **Bento Rows**: `FoodBentoRow` multi-metric dashboard cards with glowing sparkline paths and micro-nutrient capsule meters.

---

## 5. Safe API & Error Handling & Database Rules

### 5.1: Actionable HTTP Error Handling
- Never hardcode generic offline errors when remote network calls fail.
- Always inspect HTTP status codes and response bodies from `HealthApiClient`:
  - `404`: Render backend sleeping or deploying (inform user with warm-up prompt).
  - `401`: Expired session / token invalid (trigger smooth re-auth).
  - `500`: Missing backend environment keys or unexpected server errors.

### 5.2: Database & Synchronization
- Local-first architecture: All user actions must save to Room immediately and queue for sync.
- Entity changes must update schema versions, provide migrations, and maintain `SyncStatus` (`PENDING`, `SYNCED`, `FAILED`).
