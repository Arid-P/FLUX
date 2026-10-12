# Flux Apps — Antigravity Skills Setup Guide

**Version:** 1.0 | **Date:** March 2026

---

## What Are Skills?

Skills are structured folders of instructions, scripts, and resources that extend Antigravity's agent capabilities. When you install a skill, the agent automatically loads it when a relevant task is detected — you don't need to mention it manually. Each skill lives in a `SKILL.md` file inside a named directory.

**Two install locations:**
- **Project-level:** `<project-root>/.agent/skills/` — available only in that workspace
- **Global:** `~/.gemini/antigravity/skills/` — available across all workspaces on your machine

For the Flux apps, install skills at the **project level inside each app's folder**.

---

## Method 1: Automatic Install via the `skills` CLI (Recommended)

The `skills` Dart package scans your `pubspec.yaml` and installs skills for every package that ships one.

### Step 1: Activate the CLI globally

```bash
dart pub global activate skills
```

Make sure `~/.pub-cache/bin` is on your PATH. If not, add it:

```bash
# Add to ~/.bashrc or ~/.zshrc:
export PATH="$PATH:$HOME/.pub-cache/bin"

# Then reload:
source ~/.bashrc
```

### Step 2: Navigate to your project root

```bash
cd ~/path/to/fluxdone   # or fluxfoxus
```

### Step 3: Install skills for all dependencies

```bash
skills get
```

This scans every package in your `pubspec.yaml`, finds any that ship a `skills/` directory, and installs them into `.agent/skills/`. It also fetches skills from the official Flutter/Dart skills registries on GitHub.

### Step 4: Verify what was installed

```bash
skills list
```

### Step 5: Prune stale skills after removing packages

```bash
skills prune
```

---

## Method 2: Manual Install from GitHub

For skills that aren't in your pubspec (community skills, framework-specific skills):

```bash
# General pattern:
mkdir -p .agent/skills/<skill-name>
# Then copy or download SKILL.md into that directory

# Example using git:
git clone --depth 1 <repo-url> temp_skills
cp -r temp_skills/skills/<skill-name> .agent/skills/
rm -rf temp_skills
```

---

## Method 3: Install from skills.sh Registry

The community skills registry at skills.sh lists skills by technology. Search and install from terminal:

```bash
# Search for Flutter skills
gemini skills find flutter

# Install a specific skill
gemini skills install <skill-name>

# Install globally (across all projects)
gemini skills install <skill-name> --global
```

---

## Skills to Install for FluxDone

Install these inside the `fluxdone/` project root:

| Skill Name | Source | Why You Need It |
|---|---|---|
| `flutter-development` | skills.sh / pub.dev `skills get` | Core Flutter best practices, widget patterns, state management |
| `flutter_bloc` | Auto-installed via `skills get` if package ships it | BLoC/Cubit patterns, event/state definitions |
| `go_router` | Auto-installed via `skills get` if package ships it | Declarative routing, nested routes, shell routes |
| `sqflite` | Auto-installed via `skills get` if package ships it | SQLite query patterns, migrations, DAOs |
| `injectable` | Auto-installed via `skills get` if package ships it | Dependency injection code generation |
| `flutter_local_notifications` | Auto-installed via `skills get` if package ships it | Notification channel setup, scheduling, cancellation |
| `google_sign_in` | Auto-installed via `skills get` if package ships it | OAuth 2.0 flow, token management |
| `flutter_slidable` | Auto-installed via `skills get` if package ships it | Swipe action patterns |
| `android-native-kotlin` | Community registry | MethodChannel platform channel implementation in Kotlin |

### Install command for FluxDone:

```bash
cd fluxdone

# Step 1: Auto-install from pubspec dependencies
skills get

# Step 2: Install flutter-development from skills.sh
gemini skills install flutter-development

# Step 3: Install Android/Kotlin skill for MethodChannel work
gemini skills install android-native-kotlin
```

---

## Skills to Install for FluxFoxus

Install these inside the `fluxfoxus/` project root:

| Skill Name | Source | Why You Need It |
|---|---|---|
| `flutter-development` | skills.sh / pub.dev | Core Flutter patterns |
| `riverpod` | Auto-installed via `skills get` if package ships it | Riverpod v2+ provider patterns, code generation |
| `go_router` | Auto-installed via `skills get` | Routing |
| `sqflite` | Auto-installed via `skills get` | SQLite |
| `hive` | Auto-installed via `skills get` | Hive box setup, type adapters |
| `fl_chart` | Auto-installed via `skills get` | Chart widget patterns (area, bar, donut ring) |
| `workmanager` | Auto-installed via `skills get` | Background task scheduling |
| `home_widget` | Auto-installed via `skills get` | Android widget data contract, WorkManager refresh |
| `android-native-kotlin` | Community registry | Accessibility Service, UsageStats, WindowManager overlay |
| `android-accessibility-service` | Community registry | Accessibility Service event handling, node inspection |

### Install command for FluxFoxus:

```bash
cd fluxfoxus

# Step 1: Auto-install from pubspec dependencies
skills get

# Step 2: Community skills
gemini skills install flutter-development
gemini skills install android-native-kotlin
gemini skills install android-accessibility-service
```

---

## After Installing Skills

Skills are loaded automatically by Antigravity when a task matches the skill's description. You don't need to invoke them manually. However, you can reference them explicitly in a prompt when needed:

```
Read @.agent/skills/flutter-development/SKILL.md and scaffold the TaskDetailSheet widget.
```

or in Antigravity's agent chat:

```
@flutter-development scaffold the TaskDetailSheet per the spec in ui_SCR-02_task-detail-sheet.md
```

---

## Keeping Skills Up to Date

```bash
# Update all installed skills to latest versions
skills get   # re-running this updates existing skills

# Update a specific skill via npx (for community registries):
npx @rmyndharis/antigravity-skills update <skill-name>
```

---

## Useful Community Skill Registries

| Registry | URL | What's There |
|---|---|---|
| Official Flutter/Dart | `flutter/skills` (auto via `skills get`) | Flutter, Dart, core packages |
| skills.sh | https://skills.sh | Community skills for frameworks and tools |
| evanca/flutter-ai-rules | https://github.com/evanca/flutter-ai-rules | Flutter rules for BLoC, Riverpod, routing |
| kevmoo/dash_skills | https://github.com/kevmoo/dash_skills | Dart ecosystem skills |
| rmyndharis/antigravity-skills | https://github.com/rmyndharis/antigravity-skills | 300+ skills ported from Claude Code |
| guanyang/antigravity-skills | https://github.com/guanyang/antigravity-skills | Full-stack dev, UI/UX, frontend design skills |

---

## Troubleshooting

**`skills` command not found:**
```bash
dart pub global activate skills
export PATH="$PATH:$HOME/.pub-cache/bin"
```

**Skills installed but agent not using them:**
- Check they are in `.agent/skills/` inside the project root (not in a subdirectory)
- Each skill must have a `SKILL.md` at the top of its directory
- Restart Antigravity after installing new skills

**`skills get` installs nothing:**
- Make sure `pubspec.yaml` has the actual packages listed and `flutter pub get` has been run
- Some packages do not ship skills yet — use Method 2 or 3 for those

**Git required for registry installs:**
```bash
# Install git if missing:
sudo apt-get install -y git
```
