# FluxFoxus (FF) — Phase 1: Preset Editor, Multi-State Content Filtering & Whitelists Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Preset Editor (`/presets/create`, `/presets/edit`), Full Emoji Catalog Picker, Multi-State Content Filtering (YouTube & Web Browsers), Dedicated App Selector (`/presets/select-apps`), and Global Content Whitelist Hub with Discipline Quote Verification  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`, `#E07A5F`)

---

## 1. Architectural Overview & Design Philosophy

Focus sessions require nuanced control over digital environments. Binary app-blocking (either an app is completely blocked or fully open) breaks down for critical academic workflows:
- **YouTube:** Hosts pure academic Olympiad lectures (3Blue1Brown, MIT OCW, Physics Galaxy) alongside highly addictive algorithmic video rabbit-holes.
- **Web Browsers (Chrome, Firefox, Brave):** Provide access to essential calculators, graphing engines (Desmos, GeoGebra), and problem archives (AoPS, Wikipedia), but also unrestricted access to social media web clients and feeds.

FluxFoxus Phase 1 introduces **Multi-State Content Filtering**:
1. **Binary Apps:** Standard apps are either `BLOCKED` or `ALLOWED` during a preset.
2. **Multi-State Filtered Apps:** YouTube and Web Browsers support a 3rd state: **`STUDY_MODE`**, where the application launches freely but internal consumption is strictly confined to a **Global Academic Whitelist** enforced by the native Android `FocusAccessibilityService`.
3. **Intentional Friction on Additions:** Adding a channel or domain to the whitelist requires typing a randomly selected discipline sentence with 100% character accuracy, preventing impulsive whitelisting during moments of study fatigue.
4. **Full Emoji Expression:** Seamless integration of Google's official Android/Gboard catalog (`androidx.emoji2:emoji2-emojipicker`) paired with a quick academic favorites row.

---

## 2. Screen: Preset Editor (`/presets/create` & `/presets/edit`)

A full-screen push screen (`Scaffold`) with a scrollable form and sticky bottom action bar.

```
+-------------------------------------------------------+
|  [←]  New Preset                             [Save]   |
+-------------------------------------------------------+
|  PRESET IDENTITY                                      |
|  +-------------------------------------------------+  |
|  | [ 📐 ▾ ]  Name: [Olympiad Math Deep Work       ]|  |
|  +-------------------------------------------------+  |
|                                                       |
|  BREAK CONFIGURATION                                  |
|  +-----------------------+ +-----------------------+  |
|  | NUMBER OF BREAKS      | | BREAK DURATION        |  |
|  |   [ - ]  2  [ + ]     | |   [ - ]  5m  [ + ]    |  |
|  |       breaks          | |       minutes         |  |
|  +-----------------------+ +-----------------------+  |
|                                                       |
|  CONTENT-FILTERED APPS                                |
|  +-------------------------------------------------+  |
|  | ▶️ YouTube                                       |  |
|  | [ Block ]    |    [ Allow ]    |  [ Study Mode 🛡️]|  |
|  | Status: 4 Study Channels Whitelisted            |  |
|  |-------------------------------------------------|  |
|  | 🌐 Web Browsers (Chrome, Firefox, Brave)        |  |
|  | [ Block ]    |    [ Allow ]    |  [ Study Mode 🛡️]|  |
|  | Status: 12 Study Websites Whitelisted (Desmos)  |  |
|  |-------------------------------------------------|  |
|  | [ ⚙️ Manage Global Content Whitelists → ]        |  |
|  +-------------------------------------------------+  |
|                                                       |
|  APP RESTRICTIONS                                     |
|  +-------------------------------------------------+  |
|  | Blocked Apps: 5 Distracting, 1 Semi-Productive  |  |
|  | [ Configure Blocked Apps (6) → ]                |  |
|  +-------------------------------------------------+  |
|                                                       |
|  DESCRIPTION / SYLLABUS NOTES (OPTIONAL)              |
|  +-------------------------------------------------+  |
|  | "Mock exam practice only. No solutions until 6" |  |
|  +-------------------------------------------------+  |
|                                                       |
|  [ 🗑️ Delete Preset (Only visible when editing) ]    |
+-------------------------------------------------------+
|  [         Save Preset (Warm Sand #D4C4A8)         ]  |
+-------------------------------------------------------+
```

### 2.1 Top App Bar
- Left: Back arrow `←` (discards changes with unsaved confirmation dialog if dirty).
- Title: *"New Preset"* (when creating) or *"Edit Preset"* (when modifying).
- Right: Text button *"Save"* in Warm Sand `#D4C4A8` (disabled if name is blank).

---

### 2.2 Preset Identity & Emoji Picker

- Container card: Oxford Slate `#1B263B`, border `#415A77`, padding 12dp.
- **Emoji Selector Pill:** 48×48dp button on the left showing current emoji (default: `⏳`).
  - Tapping opens the **Emoji Bottom Sheet** combining two layers:
    1. **Top Quick Academic Row:** 10 one-tap focus icons: `📐` (Math), `🔬` (Physics), `📚` (Study), `💻` (Code), `🎯` (Target), `⚡` (Speed), `🧠` (Monk), `⏳` (Hourglass), `✍️` (Writing), `♟️` (Strategy).
    2. **Full Catalog Engine:** Powered by `androidx.emoji2:emoji2-emojipicker:EmojiPickerView`. Provides the full 3,000+ Unicode emoji standard matching Gboard, with categorized tabs (Smileys, Animals, Food, Activities, Objects, Symbols) and instant search.
- **Preset Name TextField:** Outlined text input in Eggshell Cream `#F4F1DE` with placeholder *"Enter preset name (e.g. Olympiad Math)"*.

---

### 2.3 Break Configuration Section

Two side-by-side metric cards inside a row:
1. **Number of Breaks Card:**
   - Label: *"NUMBER OF BREAKS"* (`#8E9CAE`, 11sp uppercase).
   - Control: `[ - ]` `2 breaks` `[ + ]`.
   - Range: `0` to `6`.
   - When set to `0`: Disables the adjacent Break Duration card, rendering text *"Breaks disabled"*.
2. **Break Duration Card:**
   - Label: *"BREAK DURATION"* (`#8E9CAE`, 11sp uppercase).
   - Control: `[ - ]` `5 mins` `[ + ]`.
   - Range: `1` to `15` minutes in 1-minute steps.
- Stepper buttons: 36×36dp rounded squares in `#415A77` with high-contrast symbols in `#F4F1DE`.

---

### 2.4 Content-Filtered Apps Section (Multi-State Rules)

Provides granular, sub-app content protection for media and web platforms.

#### A. Multi-State Controls
Each supported platform displays a 3-way segmented pill control:
1. **▶️ YouTube:**
   - `[ Block ]`: Fully blocks the YouTube app (`com.google.android.youtube`).
   - `[ Allow ]`: Grants unrestricted access.
   - `[ Study Mode 🛡️ ]`: Launches YouTube, but accessibility service inspects active channels/videos against the whitelist.
   - Subtext: Displays *"X Study Channels Whitelisted"*.
2. **🌐 Web Browsers (Chrome, Firefox, Brave, Samsung Internet):**
   - `[ Block ]`: Prohibits opening browser packages.
   - `[ Allow ]`: Grants unrestricted web browsing.
   - `[ Study Mode 🛡️ ]`: Allows browsing, but Accessibility Service monitors address bars and redirects non-whitelisted domains.
   - Subtext: Displays *"X Study Websites Whitelisted"*.

#### B. Whitelist Hub Navigation
At the bottom of the section:
- Button: `[ ⚙️ Manage Global Content Whitelists → ]` in Warm Sand `#D4C4A8`.
- Opens the **Global Content Whitelist Hub**.

---

### 2.5 App Restrictions Card
- Summarizes standard application blocks:
  - Displays: *"Blocked Apps: 5 Distracting, 1 Semi-Productive"*.
  - Action pill: `[ Configure Blocked Apps (6) → ]`.
- Tapping opens the dedicated **`/presets/select-apps`** screen.

---

### 2.6 Description / Syllabus Notes (Optional)
- Textarea in Oxford Slate `#1B263B` with border `#415A77`.
- Placeholder: *"Add notes or focus syllabus for this preset..."*.
- Max characters: 250.

---

### 2.7 Sticky Bottom Actions
- Sticky footer pinned to bottom of screen:
  - Full-width pill button: **`Save Preset`** in Warm Sand Khaki `#D4C4A8` with dark navy text `#0D1B2A` (16sp bold).
- If editing an existing preset:
  - Above the Save button, a muted Crimson text button: **`🗑️ Delete Preset`** (prompts confirmation dialog: *"Delete Preset? This will not affect past session records."*).

---

## 3. Dedicated App Selection Screen (`/presets/select-apps`)

A dedicated push screen to review, search, and configure installed app rules for the preset.

```
+-------------------------------------------------------+
|  [←]  Select Blocked Apps                      [Done] |
+-------------------------------------------------------+
|  [ ⚡ Block All Distracting ]    [ Reset to Defaults ] |
+-------------------------------------------------------+
|  [🔍 Search all installed apps...                    ] |
+-------------------------------------------------------+
|  ▼ 🔴 DISTRACTING APPS (5/5 Blocked)                   |
|  +-------------------------------------------------+  |
|  | [📸] Instagram                      [ ☑ Blocked ]|  |
|  | [🐦] X / Twitter                    [ ☑ Blocked ]|  |
|  | [🎮] Chess.com                      [ ☑ Blocked ]|  |
|  | [🎵] Spotify                        [ ☐ Allowed ]|  |
|  +-------------------------------------------------+  |
|                                                       |
|  ► 🟢 PRODUCTIVE APPS (0/8 Blocked)                   |
|  ► 🟡 SEMI-PRODUCTIVE APPS (1/6 Blocked)              |
|  ► ⚪ OTHERS (0/14 Blocked)                           |
+-------------------------------------------------------+
|  [            Save Selection (6 Blocked)           ]  |
+-------------------------------------------------------+
```

### 3.1 Smart Presets & Quick Actions
- **Default State for New Presets:**
  - All apps categorized as **Distracting** are pre-checked as `Blocked`.
  - All **Productive**, **Semi-Productive**, and **Others** are pre-checked as `Allowed`.
- **Quick Action Row:**
  - `[ ⚡ Block All Distracting ]`: Instantly selects all distracting apps.
  - `[ Reset to Defaults ]`: Reverts to recommended baseline.

### 3.2 Search & Category Accordions
- Search field dynamically filters apps across all categories.
- Collapsible category headers with category color indicator dots (🔴 Distracting, 🟢 Productive, 🟡 Semi-Productive, ⚪ Others) and blocked count tally (`5/5 Blocked`).
- Each app row contains: App Icon (36×36dp), App Title (`#F4F1DE`), and a Holst styled Lock Checkbox (`[ ☑ Blocked ]` in `#E07A5F` / `[ ☐ Allowed ]` in `#778D7A`).

---

## 4. Global Content Whitelist Hub (`/presets/content-whitelists`)

Academic whitelists are **global** across all presets. Ari defines his allowed educational resources once, and any preset set to `Study Mode` enforces them.

```
+-------------------------------------------------------+
|  [←]  Study Whitelists                                |
+-------------------------------------------------------+
|  SELECT PLATFORM TO MANAGE                            |
|  +-------------------------------------------------+  |
|  |  ▶️ YouTube Channels                4 Allowed →  |  |
|  |     3Blue1Brown, MIT OCW, Physics Galaxy...     |  |
|  +-------------------------------------------------+  |
|  |  🌐 Web Domains & Sites            12 Allowed →  |  |
|  |     desmos.com, artofproblemsolving.com...      |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

### 4.1 Channel / Domain Management Screen
Navigating into either platform displays the active whitelisted items:

```
+-------------------------------------------------------+
|  [←]  YouTube Study Channels                  [+ Add] |
+-------------------------------------------------------+
|  [🔍 Search whitelisted channels...                 ] |
|                                                       |
|  +-------------------------------------------------+  |
|  |  [▶️] 3Blue1Brown                     [@3blue1brown]|
|  |       Added 14 Sep                       [✕ Remove]|
|  +-------------------------------------------------+  |
|  |  [▶️] MIT OpenCourseWare              [@mitocw]     |
|  |       Added 22 Sep                       [✕ Remove]|
|  +-------------------------------------------------+  |
|  |  [▶️] Physics Galaxy                  [@physicsgal] |
|  |       Added 02 Oct                       [✕ Remove]|
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

- Each row displays: Channel/Domain icon, Primary Title, Handle/Domain URL, and a `✕ Remove` button.
- Removing an item prompts an instant undo snackbar.

---

### 4.2 Add Whitelist Item Flow & Discipline Quote Verification

To protect Ari from impulse-whitelisting entertainment media while studying, adding a new item enforces **Intentional Typing Friction**:

```
+-------------------------------------------------------+
|  ➕ Add Study Channel / Website                        |
+-------------------------------------------------------+
|  Platform: [ ▶️ YouTube Channel  ▾ ]                   |
|  Identifier: [@numberphile                         ]  |
|  Display Name: [Numberphile                        ]  |
|                                                       |
|  DISCIPLINE VERIFICATION                              |
|  Type the following quote exactly to confirm:         |
|  +-------------------------------------------------+  |
|  | "Discipline is choosing between what you want   |  |
|  |  now and what you want most."                   |  |
|  +-------------------------------------------------+  |
|  [Type sentence here...                            ]  |
|                                                       |
|  [ Cancel ]                 [ Confirm & Whitelist 🔒 ]|
+-------------------------------------------------------+
```

1. **Input Fields:** Platform picker, channel handle / URL (e.g. `@numberphile` or `artofproblemsolving.com`), and friendly label.
2. **Discipline Sentence Verification:**
   - Displays a randomly selected quote from a curated pool of academic discipline aphorisms.
   - Text input box directly beneath.
   - **Verification Gate:** The *"Confirm & Whitelist"* button remains strictly disabled, muted, and unclickable until the input matches the target sentence with **100% character accuracy** (case-sensitive, punctuation-checked).
3. **Sentence Pool:**
   - *"Discipline is choosing between what you want now and what you want most."*
   - *"The pain of discipline is far less than the pain of regret."*
   - *"Deep focus is a competitive advantage in a world of shallow distractions."*
   - *"Mastery requires the courage to say no to trivial novelties."*
   - *"Excellence is an art won by training and habituation."*

---

## 5. Technical Architecture & Database Schema

### 5.1 Room Database Schema (SQLite v2)

```sql
-- Updated presets table
CREATE TABLE presets (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL DEFAULT '⏳',
    break_count INTEGER NOT NULL DEFAULT 2,          -- 0 to 6
    break_duration_minutes INTEGER NOT NULL DEFAULT 5, -- 1 to 15
    youtube_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    browser_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    description TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Per-preset app rules
CREATE TABLE preset_app_rules (
    preset_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    rule_mode TEXT NOT NULL DEFAULT 'BLOCKED', -- 'BLOCKED', 'ALLOWED', 'STUDY_MODE'
    PRIMARY KEY(preset_id, package_name),
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);

-- Global Content Whitelist (Channels & Domains)
CREATE TABLE global_content_whitelists (
    id TEXT PRIMARY KEY NOT NULL,
    app_type TEXT NOT NULL,          -- 'YOUTUBE_CHANNEL' or 'BROWSER_DOMAIN'
    target_identifier TEXT NOT NULL,  -- e.g. '@3blue1brown' or 'desmos.com'
    display_label TEXT NOT NULL,      -- e.g. '3Blue1Brown' or 'Desmos Graphing'
    created_at INTEGER NOT NULL
);
```

### 5.2 Accessibility Service Enforcement Implementation
```kotlin
class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val currentPackage = event.packageName?.toString() ?: return

        val activeSession = sessionRepository.getActiveSessionSync() ?: return
        val preset = presetRepository.getPresetByIdSync(activeSession.presetId) ?: return

        when (currentPackage) {
            "com.google.android.youtube" -> {
                when (preset.youtubeMode) {
                    "BLOCK" -> blockAndRedirectToHome("YouTube is blocked during this focus session.")
                    "ALLOW" -> Unit // Permitted
                    "STUDY_MODE" -> inspectYouTubeContent(event.source)
                }
            }
            in BROWSER_PACKAGES -> {
                when (preset.browserMode) {
                    "BLOCK" -> blockAndRedirectToHome("Web browsers are blocked during this focus session.")
                    "ALLOW" -> Unit // Permitted
                    "STUDY_MODE" -> inspectBrowserUrl(event.source)
                }
            }
            else -> {
                val rule = presetRepository.getAppRuleSync(preset.id, currentPackage)
                if (rule?.ruleMode == "BLOCKED") {
                    blockAndRedirectToHome("$currentPackage is blocked during this focus session.")
                }
            }
        }
    }
}
```

---

## 6. Strict Holst UI Color Token Lock

| UI Component | Token Name | Hex Value |
| :--- | :--- | :--- |
| **Screen Background** | `color_background` | `#0D1B2A` |
| **Card Surfaces** | `color_surface` | `#1B263B` |
| **Borders & Dividers** | `color_border` | `#415A77` |
| **Primary Action CTA / Save** | `color_primary` (Warm Sand) | `#D4C4A8` |
| **Study Mode / Productive Accents** | `color_accent` (Muted Sage) | `#778D7A` |
| **Blocked Apps / Alerts** | `color_warning` (Terracotta Coral) | `#E07A5F` |
| **High-Contrast Text / Titles** | `color_text_primary` | `#F4F1DE` |
| **Muted Labels & Stepper Units** | `color_text_muted` | `#8E9CAE` |
