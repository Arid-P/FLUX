# GMP — FluxDone (FD) Build Instructions for Antigravity

**Read this entire document before writing a single line of code.**

---

## 0. Before You Start

You have been provided the following files. Read ALL of them before planning anything:

- `FLUX_CONTEXT.md` — project context, app family overview, FD↔FF integration
- `FluxDone_PRD_v2.docx` — full product requirements
- `FluxDone_TRD_v2.docx` — full technical requirements, database schema, architecture
- `ui_SCR-01_list-view.md` through `ui_SCR-13_google-drive-backup.md` — 13 locked UI specs
- `ui_ICONS_icon-system.md` — icon system

**Every UI spec file is locked. The app must match the specs exactly. No visual deviations without explicit approval from Ari.**

---

## 1. Environment Setup

### 1.1 Install Flutter

Run in terminal:
```bash
# Check if Flutter is already installed
flutter --version

# If not installed, download Flutter SDK
# Use web search to find the latest stable Flutter SDK download for Linux
# Extract to ~/flutter and add to PATH:
export PATH="$PATH:$HOME/flutter/bin"

# Verify
flutter doctor
```

### 1.2 Install Android SDK

```bash
# Install Android command-line tools via Flutter doctor
flutter doctor --android-licenses

# If Android SDK is missing, install via sdkmanager:
# First install Java (required):
sudo apt-get install -y openjdk-17-jdk

# Download Android command-line tools from:
# https://developer.android.com/studio#command-line-tools-only
# Extract to ~/android-sdk/cmdline-tools/latest/
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
export PATH="$PATH:$ANDROID_HOME/platform-tools"

# Install required SDK packages:
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
flutter doctor --android-licenses
```

### 1.3 Verify Setup

```bash
flutter doctor -v
# All items should be green except iOS (not needed for Phase 1)
```

---

## 2. Project Initialization

```bash
# Create the Flutter project
flutter create --org com.fluxdone --project-name fluxdone fluxdone
cd fluxdone

# Set minimum Android SDK to API 26 in android/app/build.gradle:
# minSdkVersion 26
# targetSdkVersion 34
```

---

## 3. Install All Dependencies

Run this single command to install all required packages:

```bash
flutter pub add \
  flutter_bloc \
  bloc \
  equatable \
  get_it \
  injectable \
  sqflite \
  path \
  path_provider \
  shared_preferences \
  go_router \
  flutter_local_notifications \
  google_sign_in \
  googleapis \
  intl \
  flutter_slidable \
  confetti \
  rxdart \
  uuid \
  timezone \
  freezed_annotation \
  json_annotation

flutter pub add --dev \
  build_runner \
  injectable_generator \
  freezed \
  json_serializable \
  mocktail \
  flutter_test

# Run code generation after adding packages:
dart run build_runner build --delete-conflicting-outputs
```

---

## 4. Architecture Rules (Non-Negotiable)

Before writing any code, internalize these rules. Violating them will require full rewrites:

### 4.1 Folder Structure
Follow the exact structure in TRD v2.0 Section 3.4. Every feature lives in `lib/features/<feature_name>/` with three subdirectories: `data/`, `domain/`, `presentation/`.

### 4.2 No Hardcoded Colors — Ever
Every single color value in the widget tree must come from `ThemeTokens`. Never write `Color(0xFF2E7D32)` or `Colors.blue` directly in a widget. Always write `context.tokens.primary` or `context.tokens.numberTheory`. This is mandatory for Phase 3 JSON theming.

### 4.3 No Cross-Module Imports
Feature modules must never import from another module's `data/` or `domain/` internals. Cross-module communication only via the shared event bus (rxdart) or shared repository interfaces in `lib/shared/`.

### 4.4 Repository Pattern
Every data access goes through a repository interface defined in the `domain/` layer. The `data/` layer implements the interface. The `presentation/` layer never touches sqflite directly.

### 4.5 BLoC/Cubit for All State
No `setState()` outside of trivial animations. All screen state is managed by a BLoC or Cubit. Events and States are immutable (use freezed).

---

## 5. Build Order (Follow Strictly)

Build in this exact order. Do not skip ahead. Each phase must be tested on a physical Android device before proceeding.

### Phase 1-A: Foundation (Build First)

**Step 1: ThemeTokens + AppTheme**
- Create `lib/core/theme/theme_tokens.dart` with ALL color tokens (light + dark sets)
- Create `lib/core/theme/app_theme.dart` constructing `ThemeData` from tokens
- Add a context extension: `context.tokens` to access current ThemeTokens
- Test: app launches in both light and dark mode, theme persists via shared_preferences

**Step 2: Database + DI**
- Create `lib/core/database/database_helper.dart` — sqflite database initialization with all v2 tables
- All table schemas exactly as specified in TRD v2.0 Sections 5.2 through 5.9
- Database version: 1. Include `onUpgrade` handler for future migrations.
- Set up get_it + injectable DI container
- Register all repositories

**Step 3: go_router + Navigation Shell**
- Implement all routes from TRD v2.0 Section 6.1
- Create the shell route with bottom navigation bar (portrait) and navigation rail (landscape)
- 4 tabs: Tasks, Calendar, Habits, Settings
- Test: navigation between all top-level tabs works

**Step 4: AppDrawer (SCR-08)**
- Build the side drawer per `ui_SCR-08_side-drawer.md`
- Portrait: modal drawer (80% width). Landscape: permanent 280dp rail.
- Folders with expand/collapse, nested lists with color swatches, smart lists
- Long-press context menus for folder and list management (inline — no separate routes)
- Color picker bottom sheet
- Rename AlertDialog
- Delete AlertDialog

### Phase 1-B: Core Task System

**Step 5: Lists Module**
- Folder CRUD, List CRUD, Section CRUD
- All operations inline in drawer (no navigation routes for SCR-10/SCR-11)
- List creation bottom sheet: name + color picker + folder selector

**Step 6: Task CRUD**
- Full tasks table implementation
- ITaskRepository interface + TaskRepositoryImpl
- All 19 columns from TRD v2.0 Section 5.5 (including is_pinned from day one)

**Step 7: Smart Lists**
- Today, Tomorrow, Upcoming (7 days), All, Completed, Trash queries
- SmartListQueryService in its own module

**Step 8: SCR-01 — List View**
- Full task card per `ui_SCR-01_list-view.md`
- Custom checkbox (CustomPainter — see `ui_ICONS_icon-system.md` Section 10)
- Priority flag icon
- Metadata row (date, time, subtask count, reminder, recurrence)
- Sections with headers
- flutter_slidable: swipe right = complete, swipe left = trash
- Sort options
- FAB

**Step 9: SCR-03 — Task Creation Sheet**
- DraggableScrollableSheet at 45% initial, 100% expanded
- Collapsed: title field + metadata chips + circular submit button
- Per `ui_SCR-03_task-creation-sheet.md` exactly
- Date picker (calendar grid bottom sheet)
- Time picker (clock face)
- List selector (folder hierarchy)
- Priority dropdown
- Validation + discard dialog

**Step 10: SCR-02 — Task Detail Sheet**
- DraggableScrollableSheet at 85% initial, 100% expanded
- Auto-save on every change (500ms debounce) — no save button
- Rich text description editor with inline markdown rendering
- Rich text toolbar docked above keyboard
- Subtasks (ReorderableListView)
- Reminders (multiple per task)
- Recurrence picker + custom recurrence screen
- Recurring task edit scope dialog
- Overflow menu (Duplicate, Share, Delete with undo snackbar)
- Per `ui_SCR-02_task-detail-sheet.md` exactly

**Step 11: Recurring Tasks**
- All 6 recurrence types (daily, weekly, monthly_date, monthly_weekday, yearly, interval)
- Recurrence instance generation
- Edit scope: this only / this and future / all

**Step 12: Trash (SCR-12)**
- Per `ui_SCR-12` in `ui_SCR-10-11-12.md`
- Restore via tap or swipe right. Delete via swipe left.
- Empty Trash with confirmation. Auto-purge on launch (30 days).

### Phase 1-C: Calendar

**Step 13: SCR-04 — Calendar View**
- This is the most complex screen. Read `ui_SCR-04_calendar-view.md` in full before starting.
- Custom timeline grid (CustomPainter): Y = time (60dp/hr × 24hrs = 1440dp total), X = dates
- 3 view modes: Day, 3-day, Week (toggle persisted to shared_preferences)
- Hour lines (solid 1dp) and half-hour lines (dashed)
- Current time indicator (red line + dot, Timer.periodic every 60 seconds)
- Task blocks: Positioned containers, list color fill, 3dp left accent, title + time range text
- Overlap detection and side-by-side column layout (max 3 columns)
- Google Calendar overlay blocks: outlined, muted, read-only (Phase 1 placeholder if not connected)
- Tap-to-create: ghost block appears, drag bottom edge to set end time, opens SCR-03
- Date header row: day abbreviation + date number, today filled circle, all-day task row
- Scroll: vertical (time axis, BouncingScrollPhysics), horizontal (date axis, PageScrollPhysics for Day/3-day, free for Week)
- Drag-to-reschedule (P1): long press block → drag to new time/date, 15-min snap, ghost at origin
- Drag-to-resize (P1): long press bottom 12dp zone of block → drag to resize duration

### Phase 1-D: Habits

**Step 14: SCR-05/06/07 — Habit Tracker**
- habits table: all columns from TRD v2.0 Section 5.8 (target_count, icon_identifier, reminder_time all present)
- SCR-05: date strip, weekly summary bar, habit cards with progress ring + streak badge + toggle
- Confetti animation on daily target reached (confetti package)
- SCR-06: stats row (current streak, longest, total completions), monthly calendar with completion dots, inline editable fields
- SCR-07: creation sheet with appearance preview, frequency picker, target stepper, reminder time picker
- StreakCalculator: correct streak logic for all frequency types

### Phase 1-E: Settings + Reminders

**Step 15: SCR-09 — Settings**
- Per `ui_SCR-09_settings.md`
- Appearance → Theme sub-screen (radio Light/Dark, immediate apply, 300ms fade)
- Account → Google sub-screen (Google Sign-In, Calendar, Drive Backup row)
- Calendar → Google Calendar connection
- Notifications → master switch + default reminder + sound picker + system settings shortcut
- SCR-13 Backup Screen: Phase 1 placeholder with orange banner, all controls disabled

**Step 16: Local Notifications (Reminders)**
- flutter_local_notifications with Android notification channels
- Reminder creation, cancellation on task delete/reschedule
- Multiple reminders per task
- Habit reminders (stored but notification scheduling Phase 2 — column exists)

### Phase 1-F: FluxFoxus Bridge

**Step 17: MethodChannel IPC**
- Register MethodChannel on `com.fluxfoxus/fd_integration`
- Send FocusBlockRequest on task create/update/delete (when start_time + end_time both set)
- Handle incoming createTask calls from FF → write "Focus Session" to FF section of default list
- Auto-create "FF" section in default list on first app launch, persist sectionId to shared_preferences
- Each recurring instance fires its own FocusBlockRequest independently
- Graceful handling when FF is not installed (silent failure, no crash)

---

## 6. Google Sign-In Setup

For Google Calendar and Google Drive Backup (Phase 2 features but Sign-In needed in Phase 1):

```bash
# Add Google services config
# 1. Create a project in Google Cloud Console
# 2. Enable: Google Calendar API, Google Drive API
# 3. Create OAuth 2.0 credentials (Android)
# 4. Download google-services.json → place in android/app/
# 5. Add to android/app/build.gradle:
#    apply plugin: 'com.google.gms.google-services'
# 6. Add to android/build.gradle:
#    classpath 'com.google.gms:google-services:4.3.15'
```

The google_sign_in package handles auth. Request scopes:
- Phase 1: `email`, `profile`
- Phase 2 (Calendar): `https://www.googleapis.com/auth/calendar.readonly`
- Phase 2 (Drive): `https://www.googleapis.com/auth/drive.file`

---

## 7. Testing Requirements

After every step in Section 5, verify on a physical Android device (API 26+):

- Feature works with **zero network connectivity**
- Back navigation works correctly on all sheets and sub-screens
- Portrait AND landscape orientations render correctly
- Light AND dark themes render correctly with no missing color tokens
- No hardcoded color values exist in any widget built in that step

---

## 8. Artifacts to Generate

After completing each major step, generate an Artifact containing:
1. Screenshot of the completed screen in light mode
2. Screenshot of the completed screen in dark mode
3. List of any deviations from the UI spec (should be none)
4. Any open questions or blockers

---

## 9. What NOT to Build in Phase 1

Do not build any of these — they are explicitly out of scope:
- Month view or List tab in Calendar View
- Folder colors (no color_hex on folders table)
- Pin task UI (column exists but no UI)
- Habit reminder notification scheduling (column exists but no scheduling)
- JSON custom theming UI
- Google Drive backup functionality (screen is placeholder only)
- iOS or Web targets
- Tags, filters, attachments, Pomodoro, collaboration
