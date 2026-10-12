# FD Phase 4 — Feature 3: Natural Language Task Parsing (Smart Recognition)

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked — Moved from P3  
**Author:** Ari  

---

## 1. Note

This feature was originally specced as FD P3 F3. It was moved to P4 to make room for Data Import (FD P3 F3) and Data Export (FD P3 F4), which were prioritised for Phase 3 to enable TickTick migration.

**The full specification is identical to the original P3 spec.** No decisions have changed.

---

## 2. Full Specification

Refer to the original spec document: `FD_P3_F3_nlp-task-parsing.md`

All decisions, pattern library, implementation details, module boundary, and routing defined there apply without modification in Phase 4.

---

## 3. P4 Context

By the time NLP ships in P4, the following P3 features are already live:
- Smart Recognition pattern library can be extended with patterns learned from user import data (FD P3 F3 Import)
- Voice input (`SpeechRecognizer`) integrates naturally with Quick Capture if that feature is ever revisited
- AI API key infrastructure (from FF P2 F3) is available for the AI enhancement fallback

No additional decisions required before implementation begins.
