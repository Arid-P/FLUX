# FD Phase 4 — Feature 4: Task Templates

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked — Moved from P3  
**Author:** Ari  

---

## 1. Note

This feature was originally specced as FD P3 F4. It was moved to P4 to make room for Data Import (FD P3 F3) and Data Export (FD P3 F4), which were prioritised for Phase 3 to enable TickTick migration.

**The full specification is identical to the original P3 spec.** No decisions have changed.

---

## 2. Full Specification

Refer to the original spec document: `FD_P3_F4_task-templates.md`

All decisions, data model, database schema, routing, and module boundary defined there apply without modification in Phase 4.

---

## 3. P4 Context

By the time Task Templates ships in P4, the following features are already live that integrate naturally:
- **NLP Task Parsing (P4 F3):** When creating a task from a template, Smart Recognition still runs on the title field. The user can type time/date info into the title and it will be parsed as normal.
- **Notes (P3 F6):** Templates are task-only. Notes do not have templates — notes are intentionally lightweight.
- **Import/Export (P3 F3/F4):** Templates are not included in the Import/Export JSON schema in P3. If template portability is needed, it is a P5+ consideration.

No additional decisions required before implementation begins.
