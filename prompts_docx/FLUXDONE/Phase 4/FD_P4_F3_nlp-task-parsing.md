# FD Phase 3 — Feature 3: Natural Language Task Parsing

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Natural Language Task Parsing (NLP) allows the user to type or speak structured date, time, repeat, and reminder information directly into the task title field. FD parses the input in real time using a pattern-based rule engine (offline, no dependencies) and autofills the corresponding task fields. An AI API key (from FF P2 F3 Settings) can optionally enhance parsing for ambiguous or natural-language inputs.

This feature is called **Smart Recognition** in the UI, matching familiar TickTick terminology.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Task Creation Sheet (SCR-03) | `ui_SCR-03_task-creation-sheet.md` |
| Calendar tap-to-create flow | PRD v2 §7.1 |
| Task fields — date, time, reminder, recurrence | PRD v2 §5, FD-01 |
| Settings screen structure | `ui_SCR-09_settings.md` |

---

## 3. Entry Points

Smart Recognition is active wherever a task title can be typed:
- SCR-03 (Task Creation Sheet) — title field
- Calendar View (SCR-04) — task creation via timeline tap (title field in the creation sheet that opens)

Smart Recognition is **not** active in:
- Task editing (SCR-02 Task Detail Sheet) — existing tasks only, no re-parsing on edit
- Habit creation
- Any field other than the task title

---

## 4. Parsing Behaviour

### 4.1 Trigger
Parsing runs inline as the user types, with a **300ms debounce** after the last keystroke. This prevents parsing on every character and avoids jarring mid-word autofills.

### 4.2 Parse Result Display
When a pattern is matched:
1. The matched portion of the title text is highlighted with a colored underline (app primary color, 2dp)
2. The corresponding task field (date, time, reminder, recurrence) autofills immediately
3. A small dismiss chip appears below the title field for each autofilled field: *"📅 Mar 21 ×"* / *"⏰ 3:00 PM ×"* etc.
4. Tapping the × on a chip: removes that specific autofill and clears the field. The highlighted text in the title remains (user can manually delete it)

### 4.3 Title Cleanup
By default, the parsed text segment is **removed from the task title** on submit. Example: typing *"Math revision tomorrow 5pm"* results in title = *"Math revision"*, date = tomorrow, time = 5:00 PM.

User can disable this in Settings → General → Smart Recognition → "Remove parsed text from title" (default: ON).

### 4.4 Voice Input
A microphone icon button appears in the title field when the field is focused (right side of the title text field, 40dp touch target).

Tap microphone → Android `SpeechRecognizer` activates → user speaks → transcribed text is inserted into the title field → normal inline parsing runs on the transcribed text.

No special voice-specific parsing rules — voice transcription feeds into the same pattern engine as typed input.

### 4.5 AI Enhancement
If the user has provided an AI API key (Settings → AI):
- A toggle in Settings → General → Smart Recognition → "Use AI for ambiguous inputs" (default: OFF)
- When ON: inputs that fail all pattern matches are sent to the AI with a structured prompt asking for field extraction
- AI response is treated identically to a pattern match result (same chip display, same undo mechanism)
- AI parsing has a 5-second timeout. On timeout or error: silent fail, no autofill

---

## 5. Pattern Library

All patterns are case-insensitive. Matched against the full title string. Multiple patterns can match simultaneously (e.g. date + time + repeat).

### 5.1 Date Patterns

| Input | Parsed As | Notes |
|---|---|---|
| `today` / `tod` / `td` | Today's date | |
| `tomorrow` / `tmr` | Tomorrow's date | |
| `Monday` / `mon` | Nearest future Monday | Same for all weekday names |
| `next Monday` / `next mon` | The Monday after the nearest | |
| `January` / `Jan` | Next January 1st | Same for all month names |
| `6 January` / `Jan 6` | Next effective 6 January | |
| `3/6` / `03/06` / `3-6` / `03-06` | Next effective 6 March | DD/MM and MM/DD both supported — locale-aware |
| `morning` | 9:00 AM today (or tomorrow if past 9 AM) | |
| `noon` | 12:00 PM today (or tomorrow if past noon) | |
| `afternoon` | 1:00 PM today (or tomorrow if past 1 PM) | |
| `evening` | 5:00 PM today (or tomorrow if past 5 PM) | |

**Ambiguity rule:** When a date is ambiguous (e.g. `Monday` when today is Monday), FD resolves to the **next future occurrence** — never today unless `today` / `tod` / `td` is used explicitly.

### 5.2 Time Patterns

| Input | Parsed As | Notes |
|---|---|---|
| `9am` / `9 am` | 9:00 AM | |
| `9pm` / `9 pm` | 9:00 PM | |
| `09:00` / `9:00` | 9:00 AM (24h format) | |
| `21:00` | 9:00 PM | |
| `9am - 10am` / `9am to 10am` | Start: 9:00 AM, End: 10:00 AM | Sets both start_time and end_time |
| `9:00 - 10:30` | Start: 9:00 AM, End: 10:30 AM | 24h range |
| `3 to 4 pm` / `3-4pm` | Start: 3:00 PM, End: 4:00 PM | |

### 5.3 Combined Date + Time Patterns

| Input | Parsed As |
|---|---|
| `6 March, 9pm` | 6 March at 9:00 PM |
| `March 6, 9pm` | 6 March at 9:00 PM |
| `March 6 at 9pm` | 6 March at 9:00 PM |
| `tomorrow 5pm` | Tomorrow at 5:00 PM |
| `Monday 3-4pm` | Nearest Monday, 3:00 PM – 4:00 PM |
| `Thursday 3 to 4 pm` | Nearest Thursday, 3:00 PM – 4:00 PM |

### 5.4 Repeat Patterns

| Input | Parsed As |
|---|---|
| `every day` | Daily recurrence |
| `every 2 days` | Every 2 days |
| `every week` | Weekly, every Monday from nearest Monday |
| `every 2 weeks` | Every 2 weeks |
| `every month` | Monthly on same day |
| `every 2 months` | Every 2 months |
| `every weekday` | Mon–Fri recurrence |
| `every weekend` / `repeated every weekend` | Sat + Sun recurrence |
| `every Monday` | Weekly on Monday |
| `every Monday and Wednesday` | Weekly on Mon + Wed |
| `March every year` | Yearly on 1 March |
| `6 March every year` | Yearly on 6 March |
| `1st day of the month` | Monthly on 1st |
| `last day of the month` | Monthly on last day |

### 5.5 Reminder Patterns

| Input | Parsed As |
|---|---|
| `remind 3 mins earlier` | Reminder 3 minutes before due time |
| `remind 1 hour earlier` | Reminder 1 hour before due time |
| `remind 3 days earlier` | Reminder 3 days before due date |
| `remind me in advance of 3pm today` | Reminder at 3 PM today (early reminder) |
| `5 minutes later` | Reminder 5 minutes from now (postponed) |
| `1 hour later` | Reminder 1 hour from now |
| `1 hour 30 minutes later` | Reminder 1h 30m from now |
| `2 days later` | Reminder 2 days from now |
| `next week later` | Reminder 1 week from now |

---

## 6. Settings

**Location:** Settings → General → Smart Recognition

| Setting | Type | Default | Description |
|---|---|---|---|
| Smart Recognition | Toggle | ON | Master toggle. Off = no inline parsing anywhere |
| Remove parsed text from title | Toggle | ON | When ON, matched text is stripped from title on submit |
| Use AI for ambiguous inputs | Toggle | OFF | Requires AI API key. Sends unmatched inputs to AI |
| Show examples | Link | — | Opens in-app examples screen (see §7) |

---

## 7. Examples Screen

Accessible from Settings → General → Smart Recognition → "Show examples".

A scrollable reference screen showing all supported patterns grouped by category (Date, Time, Repeat, Reminder), formatted as a two-column table: "Text you enter" | "FD will recognise as". Matches the TickTick Smart Recognition examples format.

---

## 8. Implementation

### 8.1 Parser Architecture

```dart
class SmartRecognitionParser {
  // Returns list of ParsedField — each field that was matched
  List<ParsedField> parse(String input) {
    final results = <ParsedField>[];
    
    for (final rule in _rules) {
      final match = rule.pattern.firstMatch(input);
      if (match != null) {
        results.add(ParsedField(
          field: rule.field,
          value: rule.extract(match),
          matchStart: match.start,
          matchEnd: match.end,
          matchedText: match.group(0)!,
        ));
      }
    }
    
    return results;
  }
}

class ParsedField {
  final TaskField field;   // date / startTime / endTime / reminder / recurrence
  final dynamic value;     // parsed value — DateTime, TimeOfDay, RecurrenceRule, etc.
  final int matchStart;
  final int matchEnd;
  final String matchedText;
}
```

### 8.2 Title Cleanup on Submit

```dart
String cleanTitle(String originalTitle, List<ParsedField> parsedFields) {
  if (!settings.removeMatchedTextFromTitle) return originalTitle;
  
  var result = originalTitle;
  // Remove matched segments in reverse order to preserve indices
  final sorted = parsedFields.sortedByDescending((f) => f.matchStart);
  for (final field in sorted) {
    result = result.replaceRange(field.matchStart, field.matchEnd, '').trim();
  }
  // Clean up double spaces
  return result.replaceAll(RegExp(r'\s+'), ' ').trim();
}
```

### 8.3 Voice Input

```dart
final speechRecognizer = SpeechRecognizer(); // android.speech.SpeechRecognizer via platform channel

Future<void> startVoiceInput() async {
  final transcription = await speechRecognizer.listen(
    locale: 'en-US',
    onPartialResult: (partial) {
      // Optionally show partial transcription in title field
    },
  );
  
  if (transcription != null) {
    titleController.text = transcription;
    // Parsing triggers automatically via debounced listener
  }
}
```

---

## 9. Module Boundary

**New module:** `smart_recognition/`

```
features/
└── smart_recognition/
    ├── domain/
    │   ├── smart_recognition_parser.dart    ← Pattern engine
    │   ├── parsed_field.dart                ← Result model
    │   └── recognition_rules.dart           ← All pattern definitions
    └── presentation/
        ├── smart_recognition_chip.dart      ← Dismiss chip widget
        └── smart_recognition_examples_screen.dart
```

Modifications to existing modules:
- `tasks/presentation/task_creation_sheet.dart` — title field: debounced listener, chip row, mic button
- `calendar/presentation/` — title field in calendar creation flow: same additions
- `settings/presentation/` — Smart Recognition section under General
