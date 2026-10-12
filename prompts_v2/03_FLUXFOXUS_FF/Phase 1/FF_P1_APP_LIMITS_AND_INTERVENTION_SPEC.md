# FluxFoxus (FF) — Phase 1: App Limits, Distracted Intervention & YouTube Study Mode Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Daily App Limits (`Tab.Limits`), 5/10/20/40 min Intervention Overlay, and YouTube Study Mode Edge Grabber  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`)

---

## 1. Architectural Strategy

FluxFoxus enforces screen discipline at two distinct levels:
1. **Macro Level (Focus Sessions):** Full-screen flip clock and strict blocklists during active study blocks.
2. **Micro Level (Daily Habit Friction):** When Ari is *not* in an active focus session, FF prevents casual digital distraction through **Persistent App Limits** and the **System Intervention Overlay Window**.

---

## 2. App Limits Screen (`Tab.Limits`)

Accessible via the 4th bottom navigation tab.

```
+-------------------------------------------------------+
|  LIMITS                                       [+ Add] |
+-------------------------------------------------------+
|  DISTRACTING APPS (3)                                 |
|                                                       |
|  +-------------------------------------------------+  |
|  | [📸 Instagram]                     [Active 🟢]   |  |
|  | 42m spent / 45m limit                           |  |
|  +-------------------------------------------------+  |
|                                                       |
|  +-------------------------------------------------+  |
|  | [▶️ YouTube]                       [Study Mode 🛡️]|  |
|  | 1h 10m spent / 2h limit                         |  |
|  +-------------------------------------------------+  |
|                                                       |
|  SEMI-PRODUCTIVE APPS (2)                             |
|  +-------------------------------------------------+  |
|  | [💬 WhatsApp]                      [Active 🟢]   |  |
|  | 18m spent / 30m limit                           |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

### 2.1 Category Structure & App Cards
- Apps are categorized using Android `UsageStatsManager` into:
  - **Distracting** (High friction, strict limits)
  - **Semi-Productive** (Moderate limits)
  - **Others** (User-defined limits)
- **App Card Visuals:**
  - Card Container: Oxford Slate (`#1B263B`) with 1dp border `#415A77`, corner radius 12dp.
  - App Icon (40×40dp) + Title in Eggshell Cream (`#F4F1DE`).
  - Progress Sub-label: E.g., *"42m spent / 45m limit"* in `#8E9CAE`.
  - Status Pill: E.g., `Active` (Muted Sage Green `#778D7A`) or `Paused` (Grey).

### 2.2 App Limit Configuration Bottom Sheet
Tapping any app card opens the detailed configuration modal:
1. **Time Budget Picker:**
   - Dual-wheel scroll for Hours (`0–6h`) and Minutes (`0–55m` in 5-minute increments).
   - Sourced with a minimum selectable limit of 15 minutes.
2. **UsageStats Smart Suggestion Bar:**
   - Displays: *"💡 Suggested limit is 45m based on your 7-day average usage."*
   - Tapping the suggestion auto-fills the time picker.
3. **Extra Time Passes Allowance:**
   - Allows user to configure emergency passes: `0` to `3` passes per day.
   - Pass duration selectable via chips: `5 min` | `10 min` | `15 min` (Max 45 minutes total per day).
4. **Primary Actions:**
   - *"Save Changes"* pill in Warm Sand Khaki (`#D4C4A8`) with dark navy text.
   - *"Turn Off Limit"* text button styled in Crimson Alert (`#DC2626`).

### 2.3 Hard Turn-Off Protection Flow
To eliminate impulsive habit breaking:
1. Tapping *"Turn Off Limit"* launches the **Streak Warning Modal**:
   - Displays app streak: `"🔥 14 Days → 🌧️ 0 Days"`.
   - Warning: *"Turning off this limit forfeits your streak for this app."*
   - **3-Second Hold Button:** User must physically hold down the button for 3 continuous seconds. A radial countdown fills the button. Releasing early aborts the action.
2. **Turn-Off Duration Selection:**
   - Once the 3-second hold completes, user chooses duration:
     - *Rest of the Day*
     - *Till Tomorrow Morning (6:00 AM)*
     - *7 Days (Vacation / Sick Mode)*

---

## 3. The Distracted Intervention Overlay (5 / 10 / 20 / 40 min)

When a blacklisted app (e.g., Instagram, Reddit) is launched *outside* of a scheduled focus session, FluxFoxus immediately intercepts the window using Android `TYPE_APPLICATION_OVERLAY`.

```
+-------------------------------------------------------+
|              [📸 Instagram — 45m Daily Limit]         |
|                     🔥 14-Day Streak                  |
|                                                       |
|  [==========================........] 38m / 45m Spent |
|                                                       |
|             Intentional Usage Verification:           |
|           How long do you need this app for?          |
|                                                       |
|       +-------------------+   +-------------------+   |
|       |      5 mins       |   |      10 mins      |   |
|       +-------------------+   +-------------------+   |
|       |      20 mins      |   |      40 mins      |   |
|       +-------------------+   +-------------------+   |
|                                                       |
|               [ Exit Instagram (Home) ]               |
+-------------------------------------------------------+
```

### 3.1 Overlay UI Layout
- **Backdrop:** The target app content behind is visible but heavily blurred and non-interactive.
- **Card Container:** Oxford Slate (`#1B263B`) with 1.5dp Warm Sand border (`#D4C4A8`), docked at the screen center/bottom.
- **Usage Context:** Displays current daily consumption progress bar (`#778D7A` fill) and remaining daily budget.
- **The 2×2 Time Selection Grid:**
  - 4 capsule pills: **`5 mins`**, **`10 mins`**, **`20 mins`**, **`40 mins`**.
  - Background: Oxford Slate (`#1B263B`), 1dp border `#415A77`.
  - Tapping a duration dismisses the overlay and activates an intentional usage timer.
- **Exit Button:** Full-width pill in Warm Sand (`#D4C4A8`) reading: `"Exit [App Name]"` (immediately returns user to Android Home launcher).

### 3.2 Expiration & Mandatory Cooldown Friction
When the selected minutes (e.g., 10m) expire:
1. The target app is immediately intercepted and blurred again.
2. The overlay reappears, but **all time options are disabled and locked**.
3. A mandatory cooldown countdown runs:
   - For 5m session: 5-second wait
   - For 10m session: 10-second wait
   - For 20m session: 15-second wait
   - For 40m session: 25-second wait
4. The user cannot bypass the cooldown. Once 0 is reached, options become clickable again or the user can tap `"Exit [App Name]"`.
5. **Zero Streak Impact:** This flow operates as pure behavioral friction and does not reset focus session streaks.

---

## 4. YouTube Study Mode & Dynamic Edge Grabber

YouTube is treated as a unique hybrid application: it contains high-value academic lectures (Olympiad math, JEE foundation Neev Diamond) alongside extreme entertainment rabbit holes.

### 4.1 Accessibility Whitelist Inspection
- `FocusAccessibilityService` inspects YouTube window transitions (`com.google.android.youtube`).
- Reads the video title and channel name nodes via `AccessibilityNodeInfo`.
- **Whitelisted Channels:** Video plays unimpeded with full controls.
- **Non-Whitelisted Channels:** 
  - Playback is automatically paused.
  - An in-app dialog overlays the video: *"Channel '[Channel Name]' is not on your academic study whitelist."*
  - To watch, user must complete the **Typing Verification Friction Engine** (typing 5 short discipline sentences if $\le 25$ whitelist channels, or 50 longer sentences if $> 25$).

### 4.2 The Dynamic Edge Grabber (Floating Pill Handle)
A subtle, translucent floating pill handle rendered on the right edge of the screen inside YouTube:

```
+-------------------------------------------------------+
|  YouTube Player                                       |
|                                                       |
|                                                   (=) | <-- Edge Grabber Handle
|                                                       |
+-------------------------------------------------------+
```

#### Behavioral Rules for the Edge Grabber:
1. **When Study Mode is OFF:**
   - The Edge Grabber handle **must remain visible on screen**.
   - Tapping it displays: *"Study Mode is currently OFF. [Enable Study Mode]"*.
2. **Turning Study Mode OFF via Edge Grabber:**
   - If the user uses the Edge Grabber to toggle Study Mode from **ON → OFF**:
   - **Crucial Enforcement Trigger:** FluxFoxus **immediately summons the 2×2 Time Selection Grid (5 min | 10 min | 20 min | 40 min)**.
   - The user cannot simply disable Study Mode and binge indefinitely—they are forced to choose an intentional time window for unrestricted browsing!
3. **When Study Mode is ON:**
   - The Edge Grabber displays a discreet shield icon (`🛡️`).
   - Tapping it expands a quick menu:
     - Current Channel status (Whitelisted / Blocked)
     - Quick button: *"Add to Whitelist"* (requires passcode)
     - Button: *"Turn Off Study Mode"* (triggers the 2×2 grid enforcement)

---

## 5. Android System Service Implementation Rules

```kotlin
// Android WindowManager Overlay Setup
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
    PixelFormat.TRANSLUCENT
)
```

1. **Permission Check:** On first run or setup, prompt `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` and `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
2. **No Memory Leaks:** Overlay view must be cleanly removed via `windowManager.removeViewImmediate(view)` upon duration selection or app exit.
3. **Accessibility Event Throttling:** Throttle `TYPE_WINDOW_CONTENT_CHANGED` inside YouTube to evaluate channel changes at a maximum rate of once per 800ms.
