# ⚡ Topic FD-P2-1: Quick Capture & Natural Language Task Parsing (Smart Recognition) Specification

**Document ID:** `FD_P2_QUICK_CAPTURE_NLP_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.quickcapture` & `com.flux.done.core.nlp`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + SpeechRecognizer + Offline Tokenizer)  
**Screen References:** SCR-03 (Quick Task Creation Sheet), SCR-04 (Calendar Timeline Tap-to-Create)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s intensive academic and Olympiad study routine, friction during task entry is the primary reason to abandon task management. Having to manually tap separate date pickers, time scrollers, priority flags, and list dropdowns breaks concentration.

**Topic FD-P2-1 establishes the Smart Recognition & In-App Quick Capture Engine for FluxDone.**  
It enables instant task entry with natural language parsing:
* **100% In-App Surface:** Capture is focused exclusively within the app via the Quick Creation bottom sheet (SCR-03) and Calendar tap-to-create (SCR-04) without extraneous background notification spam or Quick Settings tiles.
* **Offline Real-Time NLP Tokenizer:** Runs locally with pure Kotlin regex rules (zero external cloud dependencies, zero latency).
* **Calendar Slot Priority Invariant:** When summoned from a tapped slot on the Calendar timeline (SCR-04), the tapped time slot has the **absolute highest priority**; typed times in the title do not override the user's explicit calendar slot.
* **List Routing & Tag Fallback:** Typing `#ListName` routes the task to that list. If the `#tag` does not match any list, an inline `[ + Create list "tag" ]` pill is presented; if ignored, the task remains in the active list and preserves the tag text in notes/title.
* **Native Voice Input:** Instant speech-to-text via Android `SpeechRecognizer` injecting transcribed tokens directly into the parser.
* **Visual Parsing Feedback & Chips:** Interactive colored underline on matched text, instant attribute chips below the title with 1-tap `×` dismiss, and automated title cleanup on submit.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Hierarchy Baseline** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Parses `#ListName` against active Room `task_lists` table; provides list IDs and domain colors. |
| **Task Management** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Supplies the `TaskEntity` Room schema, priority constants, and subtask generation. |
| **Calendar View** | [`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md) | Timeline tap passes pre-filled `startTime` and `endTime`. Enforces Calendar slot priority invariant. |
| **Recurrence Engine** | [`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md) | Parses natural recurrence phrases (`every day`, `every mon, wed`) directly into RFC 5545 RRULE strings. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Quick Task Creation Sheet (`QuickCreationSheet.kt` — SCR-03)

Summoned via the global Floating Action Button (+) or list-level quick add:

```
+-----------------------------------------------------------------+
|  QUICK ADD TASK                                         [ 🎤 ]  |
+-----------------------------------------------------------------+
|  Solve 5 Geometry problems tomorrow 4pm #Geometry !high         |
|  ~~~~~~~~~~~~~~~~~~~~~~~~~ ~~~~~~~~ ~~~ ~~~~~~~~~ ~~~~~         |
|                                                                 |
|  PARSED ATTRIBUTES:                                             |
|  [ 📅 Tomorrow × ]  [ ⏰ 4:00 PM × ]  [ 📁 Geometry × ]  [ 🚩 High × ] |
|                                                                 |
|  -------------------------------------------------------------  |
|  List: [ Geometry #1565C0 ▾ ]             Due: [ Oct 14, 4:00 PM ]|
|                                                     [ Add Task → ]
+-----------------------------------------------------------------+
```

#### Visual Elements & Interaction Rules:
1. **Title Input Field:**
   * Single-line or compact multi-line text input with auto-focus upon opening.
   * Right-aligned microphone icon `[ 🎤 ]` (40dp touch target). Tapping opens Android native `SpeechRecognizer`.
2. **Real-Time Token Underlining:**
   * Parsed tokens receive a subtle 2dp colored underline tinted with `MaterialTheme.colorScheme.primary`.
3. **Autofill Attribute Chips:**
   * Appear directly below the text field with smooth enter animation.
   * Each chip has a trailing `×` icon:
     * `[ 📅 Tomorrow × ]`: Clearing removes date autofill.
     * `[ ⏰ 4:00 PM × ]`: Clearing removes time autofill.
     * `[ 📁 Geometry × ]`: Clearing reverts list back to active/default list.
     * `[ 🚩 High × ]`: Clearing resets priority to None.
4. **Title Cleanup on Submit:**
   * When user confirms (`Enter` or `[ Add Task → ]`), all matched token substrings are stripped from the final saved task title by default:
     * *Raw input:* `"Solve 5 Geometry problems tomorrow 4pm #Geometry !high"`
     * *Saved Task Title:* `"Solve 5 Geometry problems"`
     * *Saved Properties:* `due_date = Oct 14`, `start_time = 16:00`, `list_id = geometry_id`, `priority = 3`.
   * Configurable via DataStore setting: `remove_parsed_text_from_title` (default: `true`).

---

### 3.2 List Routing & Tag Fallback Workflow

When Ari types `#` followed by text:

```
+-----------------------------------------------------------------+
|  Read Organic Chemistry Ch 4 tomorrow #chemistry_revision       |
|                                                                 |
|  [ + Create list "chemistry_revision" ] <-- Inline Prompt Pill  |
|                                                                 |
|  [ 📅 Tomorrow × ]                                              |
|                                                     [ Add Task → ]
+-----------------------------------------------------------------+
```

* **Matching Existing List:** If `#text` matches an existing list name (e.g. `#Geometry` matching list *"Geometry"*), the parser assigns `targetListId = list.id`, colors the pill in the list's hex swatch, and strips `#Geometry` on submit.
* **Unrecognized Tag (`Option B` with `Option A` Fallback):**
  1. If `#tag` does **not** match any existing list, an inline prompt pill appears above the keyboard:
     `[ + Create list "tag_name" ]`.
  2. **If tapped:** Immediately creates a new list with default blue color and assigns the task to it.
  3. **If ignored:** The user simply taps `[ Add Task → ]`. The task stays safely in the currently active list (or default `Inbox`), and `#tag_name` is **preserved as a tag keyword** inside the task notes and title. Zero data is dropped.

---

### 3.3 Calendar Slot Priority Invariant (SCR-04)

When Quick Capture is summoned via **Calendar Timeline Tap**:

```
+-----------------------------------------------------------------+
|  NEW STUDY BLOCK (Tapped: 10:00 AM - 11:30 AM)                  |
+-----------------------------------------------------------------+
|  Combinatorics Problem Set at 4pm                               |
|                                                                 |
|  Locked to Calendar Slot: [ 10:00 AM - 11:30 AM 🔒 ]            |
|  (Calendar slot has highest priority; typed time ignored)       |
+-----------------------------------------------------------------+
```

* **Strict Invariant Rule:** If a time slot was explicitly chosen by tapping the timeline canvas, **that calendar slot has the absolute highest priority**.
* Any incidental time phrases typed in the title (e.g. *"at 4pm"*) are **strictly ignored** by the scheduler and will not move the task away from the tapped 10:00 AM slot.
* Prevents accidental jarring timeline block jumps caused by numbers in task names (e.g., *"Chapter 4 2pm review"* or *"Problem 10 at 4pm"*).

---

## 4. NLP Tokenizer & Pattern Engine (Pure Kotlin)

The parser is implemented in `SmartRecognitionParser.kt` as an offline, deterministic regex tokenizer with zero external third-party NLP libraries:

### 4.1 Pattern Specification Matrix

| Category | Regex / Keyword Pattern | Parsed Extraction | Normalized Output |
|---|---|---|---|
| **Relative Date** | `\b(today\|tod\|td)\b` | Today's local date | `LocalDate.now()` |
| **Relative Date** | `\b(tomorrow\|tmr)\b` | Tomorrow's local date | `LocalDate.now().plusDays(1)` |
| **Weekday** | `\b(next\s+)?(mon|tue|wed|thu|fri|sat|sun|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b` | Nearest future weekday | Next matching `DayOfWeek` |
| **Specific Date** | `\b(\d{1,2})(?:st\|nd\|rd\|th)?\s+(jan\|feb\|mar\|apr\|may\|jun\|jul\|aug\|sep\|oct\|nov\|dec)[a-z]*\b` | Day + Month name | `LocalDate.of(year, month, day)` |
| **Specific Date** | `\b(jan\|feb\|mar\|apr\|may\|jun\|jul\|aug\|sep\|oct\|nov\|dec)[a-z]*\s+(\d{1,2})(?:st\|nd\|rd\|th)?\b` | Month name + Day | `LocalDate.of(year, month, day)` |
| **Numeric Date** | `\b(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{2,4}))?\b` | DD/MM or MM/DD | Locale-aware calendar date |
| **Day Time** | `\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b` | 12-hour clock | `LocalTime.of(hour, minute)` |
| **24h Time** | `\b([01]?\d\|2[0-3]):([0-5]\d)\b` | 24-hour clock | `LocalTime.of(hour, minute)` |
| **Time Span** | `\b(\d{1,2}(?::\d{2})?\s*(?:am\|pm)?)\s*(?:-|to)\s*(\d{1,2}(?::\d{2})?\s*(?:am\|pm))\b` | Range start & end | `startTime`, `endTime`, `duration` |
| **Named Period**| `\b(morning\|afternoon\|evening\|night)\b` | Standard study periods | Morning = 09:00, Afternoon = 14:00, Evening = 18:00, Night = 21:00 |
| **Priority** | `!(high\|h\|3)` | High Priority | `Priority.HIGH (3)` |
| **Priority** | `!(med\|medium\|m\|2)` | Medium Priority | `Priority.MEDIUM (2)` |
| **Priority** | `!(low\|l\|1)` | Low Priority | `Priority.LOW (1)` |
| **List Routing**| `#[a-zA-Z0-9_\-]+` | Match against list names | `targetListId` or fallback tag |
| **Recurrence** | `\bevery\s+(day\|weekday\|weekend\|week\|month)\b` | Standard intervals | `RRULE:FREQ=...` |
| **Recurrence** | `\bevery\s+(\d+)\s+(days\|weeks\|months)\b` | Custom interval $N$ | `RRULE:FREQ=...;INTERVAL=N` |
| **Recurrence** | `\bevery\s+((?:mon\|tue\|wed\|thu\|fri\|sat\|sun)(?:\s*(?:and\|,)\s*(?:mon\|tue\|wed\|thu\|fri\|sat\|sun))*)\b` | Specific weekdays | `RRULE:FREQ=WEEKLY;BYDAY=...` |

---

## 5. Technical Implementation & Data Models

### 5.1 Parsed Token Data Classes

```kotlin
package com.flux.done.core.nlp

import java.time.LocalDate
import java.time.LocalTime

enum class ParsedFieldType {
    DUE_DATE,
    START_TIME,
    END_TIME,
    PRIORITY,
    LIST_ASSIGNMENT,
    RECURRENCE_RULE,
    UNRECOGNIZED_TAG
}

data class ParsedToken(
    val fieldType: ParsedFieldType,
    val rawText: String,
    val startIndex: Int,
    val endIndex: Int,
    val dateValue: LocalDate? = null,
    val startTimeValue: LocalTime? = null,
    val endTimeValue: LocalTime? = null,
    val priorityValue: Int? = null,
    val targetListId: String? = null,
    val targetListName: String? = null,
    val rruleValue: String? = null,
    val unrecognizedTag: String? = null
)

data class NlpParseResult(
    val cleanedTitle: String,
    val originalTitle: String,
    val tokens: List<ParsedToken>,
    val effectiveDate: LocalDate?,
    val effectiveStartTime: LocalTime?,
    val effectiveEndTime: LocalTime?,
    val effectivePriority: Int,
    val effectiveListId: String?,
    val effectiveRrule: String?,
    val pendingNewListPrompt: String? = null
)
```

---

### 5.2 Pure Kotlin Tokenizer Engine (`SmartRecognitionParser.kt`)

```kotlin
package com.flux.done.core.nlp

import com.flux.done.core.database.TaskListEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern

class SmartRecognitionParser(
    private val activeListsProvider: () -> List<TaskListEntity>
) {

    private val priorityRegex = Pattern.compile("!(high|h|3|med|medium|m|2|low|l|1)", Pattern.CASE_INSENSITIVE)
    private val listTagRegex = Pattern.compile("#([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
    private val relativeDateRegex = Pattern.compile("\\b(today|tod|td|tomorrow|tmr)\\b", Pattern.CASE_INSENSITIVE)
    private val weekdayRegex = Pattern.compile("\\b(next\\s+)?(monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\\b", Pattern.CASE_INSENSITIVE)
    private val timeSpanRegex = Pattern.compile("\\b(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\s*(?:-|to)\\s*(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm))\\b", Pattern.CASE_INSENSITIVE)
    private val singleTimeRegex = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b|\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b", Pattern.CASE_INSENSITIVE)
    private val namedPeriodRegex = Pattern.compile("\\b(morning|afternoon|evening|night)\\b", Pattern.CASE_INSENSITIVE)
    private val recurrenceRegex = Pattern.compile("\\bevery\\s+(day|weekday|weekend|week|month|\\d+\\s+days|\\d+\\s+weeks)\\b", Pattern.CASE_INSENSITIVE)

    fun parse(
        input: String,
        isCalendarSlotLocked: Boolean = false,
        lockedStartTime: LocalTime? = null,
        lockedEndTime: LocalTime? = null,
        stripTokensFromTitle: Boolean = true
    ): NlpParseResult {
        val tokens = mutableListOf<ParsedToken>()
        val existingLists = activeListsProvider()

        // 1. Priority Parser
        val priorityMatcher = priorityRegex.matcher(input)
        var parsedPriority = 0
        while (priorityMatcher.find()) {
            val matched = priorityMatcher.group(1).lowercase()
            val prioValue = when (matched) {
                "high", "h", "3" -> 3
                "med", "medium", "m", "2" -> 2
                "low", "l", "1" -> 1
                else -> 0
            }
            parsedPriority = prioValue
            tokens.add(
                ParsedToken(
                    fieldType = ParsedFieldType.PRIORITY,
                    rawText = priorityMatcher.group(0),
                    startIndex = priorityMatcher.start(),
                    endIndex = priorityMatcher.end(),
                    priorityValue = prioValue
                )
            )
        }

        // 2. List & Tag Parser (#List or #tag)
        val tagMatcher = listTagRegex.matcher(input)
        var parsedListId: String? = null
        var pendingPrompt: String? = null
        while (tagMatcher.find()) {
            val tagName = tagMatcher.group(1)
            val matchedList = existingLists.firstOrNull { it.name.equals(tagName, ignoreCase = true) }
            if (matchedList != null) {
                parsedListId = matchedList.id
                tokens.add(
                    ParsedToken(
                        fieldType = ParsedFieldType.LIST_ASSIGNMENT,
                        rawText = tagMatcher.group(0),
                        startIndex = tagMatcher.start(),
                        endIndex = tagMatcher.end(),
                        targetListId = matchedList.id,
                        targetListName = matchedList.name
                    )
                )
            } else {
                pendingPrompt = tagName
                tokens.add(
                    ParsedToken(
                        fieldType = ParsedFieldType.UNRECOGNIZED_TAG,
                        rawText = tagMatcher.group(0),
                        startIndex = tagMatcher.start(),
                        endIndex = tagMatcher.end(),
                        unrecognizedTag = tagName
                    )
                )
            }
        }

        // 3. Date Parser
        var parsedDate: LocalDate? = null
        val relMatcher = relativeDateRegex.matcher(input)
        if (relMatcher.find()) {
            val text = relMatcher.group(1).lowercase()
            parsedDate = if (text in listOf("today", "tod", "td")) LocalDate.now() else LocalDate.now().plusDays(1)
            tokens.add(
                ParsedToken(
                    fieldType = ParsedFieldType.DUE_DATE,
                    rawText = relMatcher.group(0),
                    startIndex = relMatcher.start(),
                    endIndex = relMatcher.end(),
                    dateValue = parsedDate
                )
            )
        }

        val weekdayMatcher = weekdayRegex.matcher(input)
        if (parsedDate == null && weekdayMatcher.find()) {
            val isNext = weekdayMatcher.group(1) != null
            val dayName = weekdayMatcher.group(2).lowercase()
            val dayOfWeek = parseDayOfWeek(dayName)
            var target = LocalDate.now().with(TemporalAdjusters.next(dayOfWeek))
            if (isNext) target = target.plusWeeks(1)
            parsedDate = target
            tokens.add(
                ParsedToken(
                    fieldType = ParsedFieldType.DUE_DATE,
                    rawText = weekdayMatcher.group(0),
                    startIndex = weekdayMatcher.start(),
                    endIndex = weekdayMatcher.end(),
                    dateValue = parsedDate
                )
            )
        }

        // 4. Time Parser (Enforces Calendar Slot Priority Invariant)
        var parsedStartTime: LocalTime? = lockedStartTime
        var parsedEndTime: LocalTime? = lockedEndTime

        if (!isCalendarSlotLocked) {
            val spanMatcher = timeSpanRegex.matcher(input)
            if (spanMatcher.find()) {
                val start = parseTime(spanMatcher.group(1))
                val end = parseTime(spanMatcher.group(2))
                if (start != null && end != null) {
                    parsedStartTime = start
                    parsedEndTime = end
                    tokens.add(
                        ParsedToken(
                            fieldType = ParsedFieldType.START_TIME,
                            rawText = spanMatcher.group(0),
                            startIndex = spanMatcher.start(),
                            endIndex = spanMatcher.end(),
                            startTimeValue = start,
                            endTimeValue = end
                        )
                    )
                }
            } else {
                val singleMatcher = singleTimeRegex.matcher(input)
                if (singleMatcher.find()) {
                    val time = parseTime(singleMatcher.group(0))
                    if (time != null) {
                        parsedStartTime = time
                        parsedEndTime = time.plusMinutes(60) // default 1 hour duration
                        tokens.add(
                            ParsedToken(
                                fieldType = ParsedFieldType.START_TIME,
                                rawText = singleMatcher.group(0),
                                startIndex = singleMatcher.start(),
                                endIndex = singleMatcher.end(),
                                startTimeValue = time,
                                endTimeValue = parsedEndTime
                            )
                        )
                    }
                }
            }
        }

        // 5. Recurrence Parser
        var parsedRrule: String? = null
        val recurMatcher = recurrenceRegex.matcher(input)
        if (recurMatcher.find()) {
            val phrase = recurMatcher.group(1).lowercase()
            parsedRrule = when {
                phrase == "day" -> "RRULE:FREQ=DAILY;INTERVAL=1"
                phrase == "weekday" -> "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
                phrase == "weekend" -> "RRULE:FREQ=WEEKLY;BYDAY=SA,SU"
                phrase == "week" -> "RRULE:FREQ=WEEKLY;INTERVAL=1"
                phrase == "month" -> "RRULE:FREQ=MONTHLY;INTERVAL=1"
                phrase.endsWith("days") -> "RRULE:FREQ=DAILY;INTERVAL=${phrase.split(" ")[0]}"
                phrase.endsWith("weeks") -> "RRULE:FREQ=WEEKLY;INTERVAL=${phrase.split(" ")[0]}"
                else -> null
            }
            if (parsedRrule != null) {
                tokens.add(
                    ParsedToken(
                        fieldType = ParsedFieldType.RECURRENCE_RULE,
                        rawText = recurMatcher.group(0),
                        startIndex = recurMatcher.start(),
                        endIndex = recurMatcher.end(),
                        rruleValue = parsedRrule
                    )
                )
            }
        }

        // 6. Clean Title Construction
        val cleanedTitle = if (stripTokensFromTitle) {
            cleanInputString(input, tokens.filter { it.fieldType != ParsedFieldType.UNRECOGNIZED_TAG })
        } else {
            input.trim()
        }

        return NlpParseResult(
            cleanedTitle = cleanedTitle,
            originalTitle = input,
            tokens = tokens,
            effectiveDate = parsedDate,
            effectiveStartTime = parsedStartTime,
            effectiveEndTime = parsedEndTime,
            effectivePriority = parsedPriority,
            effectiveListId = parsedListId,
            effectiveRrule = parsedRrule,
            pendingNewListPrompt = pendingPrompt
        )
    }

    private fun cleanInputString(input: String, tokensToRemove: List<ParsedToken>): String {
        var result = input
        // Sort descending by start index to avoid altering offsets of preceding tokens
        val sortedTokens = tokensToRemove.sortedByDescending { it.startIndex }
        for (token in sortedTokens) {
            result = result.removeRange(token.startIndex, token.endIndex)
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun parseDayOfWeek(str: String): DayOfWeek {
        return when {
            str.startsWith("mon") -> DayOfWeek.MONDAY
            str.startsWith("tue") -> DayOfWeek.TUESDAY
            str.startsWith("wed") -> DayOfWeek.WEDNESDAY
            str.startsWith("thu") -> DayOfWeek.THURSDAY
            str.startsWith("fri") -> DayOfWeek.FRIDAY
            str.startsWith("sat") -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
    }

    private fun parseTime(str: String): LocalTime? {
        // Normalizes "4pm", "04:00 pm", "16:00"
        return runCatching {
            val cleaned = str.trim().lowercase()
            if (cleaned.contains("am") || cleaned.contains("pm")) {
                val isPm = cleaned.contains("pm")
                val numericPart = cleaned.replace("am", "").replace("pm", "").trim()
                val parts = numericPart.split(":")
                var hour = parts[0].toInt()
                val minute = if (parts.size > 1) parts[1].toInt() else 0
                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                LocalTime.of(hour, minute)
            } else {
                val parts = cleaned.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            }
        }.getOrNull()
    }
}
```

---

## 6. MVI Architecture & State Management

### 6.1 Quick Capture MVI State (`QuickCaptureViewModel.kt`)

```kotlin
package com.flux.done.features.quickcapture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.done.core.nlp.NlpParseResult
import com.flux.done.core.nlp.ParsedFieldType
import com.flux.done.core.nlp.ParsedToken
import com.flux.done.core.nlp.SmartRecognitionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class QuickCaptureUiState(
    val rawInputText: String = "",
    val parseResult: NlpParseResult? = null,
    val selectedListId: String = "inbox",
    val selectedPriority: Int = 0,
    val selectedDueDate: LocalDate? = null,
    val selectedStartTime: LocalTime? = null,
    val selectedEndTime: LocalTime? = null,
    val selectedRrule: String? = null,
    val isCalendarSlotLocked: Boolean = false,
    val dismissedFieldTypes: Set<ParsedFieldType> = emptySet(),
    val isListeningVoice: Boolean = false,
    val inlineNewListPrompt: String? = null
)

sealed interface QuickCaptureIntent {
    data class OnInputChanged(val text: String) : QuickCaptureIntent
    data class DismissAttributeChip(val fieldType: ParsedFieldType) : QuickCaptureIntent
    data class CreateNewListFromPrompt(val listName: String) : QuickCaptureIntent
    object ToggleVoiceInput : QuickCaptureIntent
    data class OnVoiceTranscriptionReceived(val text: String) : QuickCaptureIntent
    object SubmitTask : QuickCaptureIntent
}
```

---

## 7. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Calendar Tap-to-Create Time Conflict** | When Quick Capture opens from SCR-04 with locked calendar bounds, `isCalendarSlotLocked = true`. The parser skips time overrides; the tapped calendar slot is preserved with 100% fidelity. |
| **Ignored Unrecognized `#tag`** | If user types `#ioqm_geometry` and ignores the `[ + Create list "ioqm_geometry" ]` pill, task saves to the active list and `#ioqm_geometry` remains in the title/notes. No data is lost. |
| **Number Collision in Task Title** | For strings like *"Solve 10 problems at 5pm"*, the tokenizer recognizes `"10 problems"` as title content and `"at 5pm"` as the time token, keeping *"Solve 10 problems"* intact upon title stripping. |
| **SpeechRecognizer Permission Denied** | If microphone permission is not granted, tapping the mic icon displays standard Android permission rationale. On denial, Quick Capture falls back silently to keyboard typing without crashing. |
| **Ambiguous Relative Weekday** | Typing `"Friday"` when today is Friday always resolves to the **next future Friday** (+7 days), never retroactively today. |
| **Title Stripping Disabled** | If user disables "Remove parsed text from title" in Settings, the raw text is preserved verbatim in the title while still applying all parsed date, time, and priority properties. |
