# 🛡️ Topic SEC-03: Comprehensive Input Validation, Data Sanitization & Schema Defense Specification

**Document ID:** `SEC_03_INPUT_VALIDATION_AND_DATA_DEFENSE_SPEC`  
**Phase:** Security, Authentication & System Validation Suite  
**Target Packages:** `com.flux.core.validation` (Shared Multiplatform Module)  
**Architecture:** Zero-Trust Boundary Validation / Fail-Fast Defensive Ingestion (Kotlin 2.x + Room 2.7+ KMP + Kotlinx Serialization)  
**Applicability:** Universal across both **FluxDone (FD)** and **FluxFoxus (FF)**  

---

## 1. Overview & Defensive Ingestion Philosophy

Applications that accept freeform text, complex recurrence rules (RRULE), mathematical LaTeX scripts, and external file backups are vulnerable to boundary overflow, injection attacks, infinite loop denial-of-service (DoS), and database corruption.

**Topic SEC-03 establishes a universal validation gate for all incoming data across FluxDone and FluxFoxus.**  
All data entering the database—whether from user typing, voice NLP, cloud sync, or external backup restore—must pass through strict, deterministic validation pipelines.

---

## 2. Field Boundary & Type Validation Matrix

```
[ Incoming Raw Input ]
           │
           ▼
[ Domain Validation Gate ] ──► FAILS ──► Reject / Cleanse with ValidationError
           │ PASS
           ▼
[ Room DAO Parameterized Query ] ──► Database Insertion
```

| Field / Attribute | Allowed Type & Format | Strict Boundary Constraint | Sanitization / Rejection Rule |
|---|---|---|---|
| **Task Title** | `String` (UTF-8) | $1 \le \text{length} \le 255$ characters | Strips null bytes (`\0`) and non-printable control characters ($0x00 - 0x1F$). Auto-trims leading/trailing whitespace. |
| **Task Notes Body**| `String` (Markdown) | Max $5\text{ MB}$ ($5,242,880$ characters) | Strips raw `<script>`, `<iframe>`, `javascript:` tags. Rejects payloads $>5\text{ MB}$ with `PayloadTooLargeException`. |
| **Domain Tag** | `String` | $2 \le \text{length} \le 50$, regex: `^[a-zA-Z0-9_-]+$` | Rejects punctuation, special characters, and path traversals (`../`). |
| **Color Hex** | `String` | Exact 6-character hex code: `^#([A-Fa-f0-9]{6})$` | Validates against curated 12-swatch academic palette; falls back to default `#1565C0` if invalid. |
| **Priority** | `Int` (Enum) | Strict values: `0` (None), `1` (High), `2` (Med), `3` (Low) | Rejects out-of-bounds integers with `InvalidPriorityException`. |
| **Timestamps** | `Long` (Epoch ms) | $1704067200000 \le t \le 4102444800000$ (Years 2024 to 2100) | Rejects corrupted negative timestamps or distant century values. |
| **Time Spans** | `Pair<Long, Long>`| `end_time_epoch >= start_time_epoch` | Rejects inverted time ranges (`end_time < start_time`). |
| **Timezone Offset**| `Int` (Minutes) | $-840 \le \text{offset} \le +840$ ($-14\text{h}$ to $+14\text{h}$) | Rejects non-physical astronomical timezone values. |

---

## 3. LaTeX Math & Markdown Sanitization Engine

To prevent Cross-Site Scripting (XSS) or parser crashes in KaTeX and Markdown renderers:

```kotlin
package com.flux.core.validation

object MarkdownSanitizer {
    private val FORBIDDEN_HTML_TAGS = listOf(
        "<script", "</script>",
        "<iframe", "</iframe>",
        "javascript:",
        "onload=", "onerror=", "onclick="
    )

    fun sanitizeNoteContent(rawMarkdown: String): String {
        var clean = rawMarkdown.replace("\u0000", "") // Strip null bytes
        
        // Strip dangerous HTML/Script tags while preserving LaTeX math ($...$, $$...$$)
        for (tag in FORBIDDEN_HTML_TAGS) {
            clean = clean.replace(Regex(tag, RegexOption.IGNORE_CASE), "")
        }
        return clean
    }
}
```

---

## 4. RRULE Recurrence Syntax & Infinite Loop Defense

Recurrence rules (RFC 5545) can cause infinite loop denial-of-service if given malicious inputs (e.g. `INTERVAL=0` or unachievable `BYSETPOS` rules).

### 4.1 RRULE Validator Implementation (`RRuleSafetyValidator.kt`)

```kotlin
package com.flux.core.validation

object RRuleSafetyValidator {
    private val VALID_FREQUENCIES = setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
    private val RRULE_REGEX = Regex("^(RRULE:)?(FREQ=[A-Z]+)(;[A-Z0-9=,]+)*$")

    fun validateRRule(rruleString: String): Result<String> {
        if (!RRULE_REGEX.matches(rruleString)) {
            return Result.failure(IllegalArgumentException("Malformed RRULE syntax: $rruleString"))
        }

        val parts = rruleString.removePrefix("RRULE:").split(";").associate {
            val tokens = it.split("=")
            tokens[0] to tokens.getOrElse(1) { "" }
        }

        val freq = parts["FREQ"] ?: return Result.failure(IllegalArgumentException("Missing FREQ"))
        if (freq !in VALID_FREQUENCIES) {
            return Result.failure(IllegalArgumentException("Unsupported FREQ: $freq"))
        }

        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
        if (interval < 1 || interval > 365) {
            return Result.failure(IllegalArgumentException("INTERVAL must be between 1 and 365"))
        }

        val count = parts["COUNT"]?.toIntOrNull()
        if (count != null && count < 1) {
            return Result.failure(IllegalArgumentException("COUNT must be positive"))
        }

        return Result.success(rruleString)
    }

    // Safety cap: Never expand more than 730 instances (2 years) in a single query
    const val MAX_EXPANSION_INSTANCES = 730
}
```

---

## 5. Backup & External JSON Ingestion Armor

When importing portable JSON v2.0 or restoring SQLite snapshots ([`FD_P2_BACKUP_EXPORT_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_BACKUP_EXPORT_SPEC.md)):

1. **Size Cap Enforcement:** Max file size strictly capped at **$100\text{ MB}$**. Files exceeding $100\text{ MB}$ are rejected immediately before reading into memory.
2. **Schema Version Check:** Inspects `manifest.schema_version`. If greater than the current app version, rejects with:
   > *"Backup was created by a newer version of Flux. Please update the app before restoring."*
3. **Foreign Key Integrity Check:** Validates that all imported `tasks` reference valid `list_id` and `folder_id` records in the backup payload. Orphaned tasks are routed to the default Inbox.
4. **100% Parameterized DAO Queries:** All database mutations execute via Room `@Insert`, `@Update`, `@Delete`, and parameterized `@Query`. Raw SQL string concatenation (`"SELECT * FROM " + input`) is **strictly prohibited** in the codebase.

---

## 6. Verification & Validation Test Suite

1. **Title Boundary Test:** Attempt to insert a task with an empty string `""` or title with 300 characters. Verify that `ValidationError` is thrown and database remains unmodified.
2. **Control Character Cleansing Test:** Input title `"Geometry\u0000Exam\u0007Preparation"`. Verify that the sanitized title is `"GeometryExamPreparation"`.
3. **RRULE DoS Protection Test:** Pass `FREQ=DAILY;INTERVAL=0` to `RRuleSafetyValidator`. Verify that validation fails with `IllegalArgumentException`.
4. **Time Range Sanity Test:** Create a task with `start_time = 16:00` and `end_time = 14:00`. Verify that the validator rejects the inverted span before Room insertion.
5. **SQL Injection Resistance Test:** Input task title `' OR 1=1; DROP TABLE tasks; --`. Verify that the title is safely inserted as literal string characters with zero DDL execution.
