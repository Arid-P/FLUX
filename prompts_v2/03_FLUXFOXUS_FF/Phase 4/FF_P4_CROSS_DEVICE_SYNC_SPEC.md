# 🔄 Topic FF-P4-5: Cross-Device Sync Engine & Linux Focus Daemon Specification

**Document ID:** `FF_P4_CROSS_DEVICE_SYNC_SPEC`  
**Phase:** Phase 4 (Cross-Platform Daemon, Group Limits & Habit Breaking)  
**Target Packages:**  
- Android Client: `com.flux.focus.features.sync`  
- Linux Desktop Client: `com.flux.focus.desktop` (Compose Multiplatform)  
- Linux System Daemon: `com.flux.focus.desktop.daemon` (`fluxfoxus-daemon.service`)  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Compose Multiplatform for Desktop + Room DAO + systemd Daemon + Google OAuth / Firebase Realtime Sync)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Modern deep work rarely happens on a single screen. A student or knowledge worker typically keeps an **Android smartphone** beside an **Ubuntu Linux workstation or laptop**. 

Fragmented discipline creates immediate failure modes:
1. **Desktop Distraction Leakage:** The phone is locked in an intense 90-minute focus session, but the user opens Reddit, YouTube, or Discord on their Linux browser.
2. **Mobile Interruption Leakage:** The user enters deep coding or writing on Linux, but picks up the phone to scroll notifications.
3. **Manual Setup Friction:** Needing to manually configure and start focus sessions on two separate devices creates cognitive friction that discourages sessions altogether.

**Topic FF-P4-5 establishes the Flux Cross-Device Sync Engine and Linux Focus Daemon.**  
When a focus session starts on **either** device, all paired devices immediately lock into unified focus state. Breaks, timer ticks, and completions sync in real-time.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Phase 1 Timer Engine** | [`FF_P1_TIMER_AND_LOCK_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_TIMER_AND_LOCK_SPEC.md) | Synchronizes state transitions (`ACTIVE`, `PAUSED_BREAK`, `COMPLETED`, `QUIT_PREMATURELY`). |
| **Phase 2 Break Protocol** | [`FF_P2_BREAK_NEGOTIATION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%202/FF_P2_BREAK_NEGOTIATION_SPEC.md) | Break granted on one device immediately propagates break mode to all paired devices. |
| **Phase 3 Lock Screen & Monotonic Clock** | [`FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md) | Uses elapsed monotonic timestamps to prevent clock drift during cross-device sync. |
| **Phase 4 Pre-Open Friction** | [`FF_P4_PRE_OPEN_FRICTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_PRE_OPEN_FRICTION_SPEC.md) | Extends habit friction principles to desktop browser tabs and applications. |

---

## 3. Ubuntu Linux Architecture & Daemon Implementation

The Linux implementation consists of two coordinated layers:
1. **User Desktop Client (`flux-focus-desktop`):** A Compose Multiplatform UI app running in the user’s desktop session (displays the timer HUD, presets, reflection modal, and pairing UI).
2. **System Daemon (`fluxfoxus-daemon`):** A lightweight background service managed by `systemd` running with elevated privileges to enforce distraction blocking.

```
┌────────────────────────────────────────────────────────┐
│               Ubuntu Linux Desktop                     │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Compose Desktop GUI (User Session)                │  │
│  │ - Holst Dark UI HUD                              │  │
│  │ - Google Sign-In & Device Pairing Manager        │  │
│  │ - Session Controller & Local IPC Client          │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │ UNIX Domain Socket / DBus  │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │ fluxfoxus-daemon (systemd root service)           │  │
│  │ 1. Backup & Manipulate /etc/hosts (0.0.0.0)      │  │
│  │ 2. inotify Watcher on /etc/hosts (Anti-Tamper)   │  │
│  │ 3. D-Bus Window Inspector (Block Steam, Discord) │  │
│  │ 4. Crash-Safe State Machine & Auto-Restoration   │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

### 3.1 Website Blocking: `/etc/hosts` Sinkhole with Mandatory Backups

#### A. Strict Backup Protocol:
Before any file modification occurs, the daemon enforces a strict two-tier backup policy:
1. **Pristine Golden Baseline (`/etc/hosts.flux.orig`):**
   * Created on daemon installation / first run if it does not already exist.
   * Never overwritten or altered; represents the machine’s clean, original state.
2. **Pre-Session Dynamic Backup (`/etc/hosts.flux.bak`):**
   * Copied immediately prior to any modification when a focus session begins:
     ```bash
     cp -p /etc/hosts /etc/hosts.flux.bak
     ```
   * Preserves all file permissions (`0644`), ownership (`root:root`), and timestamps.

#### B. Demarcated Injection Engine:
Blocked domains (from the active Preset) are redirected to `0.0.0.0` inside explicit markers:
```
# Pristine system entries remain untouched
127.0.0.1   localhost
127.0.1.1   ubuntu-workstation

### FLUXFOXUS_SESSION_BLOCK_START - DO NOT EDIT MANUALLY ###
0.0.0.0 reddit.com
0.0.0.0 www.reddit.com
0.0.0.0 old.reddit.com
0.0.0.0 twitter.com
0.0.0.0 x.com
0.0.0.0 youtube.com
0.0.0.0 www.youtube.com
0.0.0.0 instagram.com
0.0.0.0 tiktok.com
0.0.0.0 news.ycombinator.com
### FLUXFOXUS_SESSION_BLOCK_END ###
```

#### C. Flush and Cleanup:
* When the session completes or enters a break:
  1. The daemon reads `/etc/hosts` and removes everything between `FLUXFOXUS_SESSION_BLOCK_START` and `FLUXFOXUS_SESSION_BLOCK_END`.
  2. If any discrepancy or corruption is detected, it immediately restores from `/etc/hosts.flux.bak`.
  3. Executes DNS cache flush:
     ```bash
     systemd-resolve --flush-caches 2>/dev/null || resolvectl flush-caches 2>/dev/null
     ```

#### D. Crash & Power Failure Self-Healing:
Upon system startup or daemon restart, `fluxfoxus-daemon` checks:
* If the block marker exists in `/etc/hosts`:
  * Evaluates current time against the recorded `session_end_time` in `/var/lib/fluxfoxus/state.json`.
  * If the session is expired or invalid, **instantly reverts `/etc/hosts` from `/etc/hosts.flux.bak`** (or `/etc/hosts.flux.orig`), ensuring the user is never permanently locked out due to a crash.

---

### 3.2 Desktop Application Blocking (D-Bus & Process Guard)

Desktop apps (e.g. Discord, Steam, Spotify, Minecraft) cannot be blocked via `/etc/hosts`. The daemon guards them via window inspection:

1. **Window State Monitoring:**  
   Listens to window creation events via standard D-Bus / X11 / Wayland protocols (`_NET_ACTIVE_WINDOW` on X11, `org.freedesktop.portal` or desktop compositor signals on Wayland).
2. **Blocked App Detection:**  
   If the active window’s process or `WM_CLASS` matches a blocked application (e.g. `steam`, `discord`, `com.discordapp.Discord`):
   * **Action:** The daemon immediately raises the Compose Desktop **Focus Shield Window** over the screen:
     ```
     +-------------------------------------------------------+
     │                                                       │
     │                   ⚡ FLUX FOCUS                       │
     │                                                       │
     │             Discord is Blocked on Desktop             │
     │                                                       │
     │            Session: Olympiad Math Marathon            │
     │            Remaining: 42m 18s                         │
     │                                                       │
     │            [ ↩️ Return to Work ]                     │
     │                                                       │
     +-------------------------------------------------------+
     ```
   * Tapping `[ ↩️ Return to Work ]` minimizes the offending application.
   * If the user attempts to bypass the shield, the daemon sends `SIGSTOP` (freeze process) until the session ends, whereupon it sends `SIGCONT`.

---

## 4. Google Account Authentication & Device Pairing

Device discovery and trust are anchored to the user's **Google Account**, providing zero-friction pairing with no manual IP or port entry.

```
┌────────────────────────┐                   ┌────────────────────────┐
│     Android Phone      │                   │  Ubuntu Linux Desktop  │
│  [ Sign in with Google]│                   │ [ Sign in with Google] │
└───────────┬────────────┘                   └───────────┬────────────┘
            │                                            │
            ▼                                            ▼
   OAuth 2.0 / Firebase                         OAuth 2.0 / Firebase
   ID: user_1092847123                          ID: user_1092847123
            │                                            │
            └─────────────────────┬──────────────────────┘
                                  │
                                  ▼
                     ┌─────────────────────────┐
                     │ Google / Firebase Cloud │
                     │ Device Registry Table   │
                     └─────────────────────────┘
```

### 4.1 Authentication Lifecycle

1. **Android Client:** Uses Android Credential Manager (`GoogleIdTokenCredential`) to authenticate via the user's Google Account.
2. **Ubuntu Linux Client:** Uses Google OAuth 2.0 with PKCE (Proof Key for Code Exchange) via system browser callback:
   * Launches default browser to `https://accounts.google.com/o/oauth2/v2/auth`.
   * Captures loopback redirect on `http://127.0.0.1:41782/callback`.
   * Exchanges authorization code for secure refresh token saved in Linux Keyring (`SecretService` / `gnome-keyring`).
3. **Shared User ID:** Both devices now share an identical authenticated `user_uid`.

### 4.2 Device Registration

Upon authentication, each device registers its presence in the user's device graph:

```json
{
  "device_id": "uuid-v4-generated-on-install",
  "device_name": "Ubuntu Workstation (ThinkPad P14s)",
  "device_platform": "LINUX_DESKTOP",
  "app_version": "1.0.0",
  "sync_enabled": true,
  "last_seen_timestamp": 1791811200000
}
```

* **UI on `Tab.Settings` → `Connected Devices`:**  
  Lists all devices registered under the Google Account:
  ```
  CONNECTED DEVICES
  • Pixel 8 Pro (This Device)             [ Active ]
  • Ubuntu Workstation (Desktop)          [ Active • Synced ]
  
  [ + Pair Another Device ]   [ Unlink Google Account ]
  ```

---

## 5. Multi-Device Realtime Sync Protocol

### 5.1 Dual-Channel Hybrid Architecture

Sync operates simultaneously over two resilient channels:

```
┌────────────────────────────────────────────────────────┐
│ Channel 1: Local LAN (Same Wi-Fi)                      │
│ - mDNS Discovery: `_fluxfoxus._tcp`                    │
│ - Encrypted Local WebSocket / UDP Broadcast            │
│ - Latency: < 20ms • Works 100% Offline (No Internet)  │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│ Channel 2: Cloud Sync (Different Networks / Mobile)    │
│ - Google Firebase Realtime / Firestore Document        │
│ - Low-latency streaming listener                       │
│ - Latency: < 300ms • Works across 5G / Home Broadband  │
└────────────────────────────────────────────────────────┘
```

* **Seamless Failover:** When devices are on the same home or library Wi-Fi, local LAN packets arrive first and update the state in <20ms. If the phone is on 5G mobile data while the laptop is on Wi-Fi, the Cloud channel carries the state effortlessly.

---

### 5.2 Unified Session State Schema (`ActiveFocusSession`)

```kotlin
package com.flux.focus.features.sync.model

data class SyncedFocusState(
    val sessionId: String,                     // UUID
    val sourceDeviceId: String,                // ID of device that initiated session
    val presetId: String,                      // Active Preset ID
    val presetName: String,                    // Display name e.g. "Deep Work"
    val status: String,                        // 'ACTIVE', 'PAUSED_BREAK', 'COMPLETED', 'QUIT'
    val startTimestampUtc: Long,               // Wall clock UTC
    val durationMinutes: Int,                  // Planned total duration
    val elapsedRealtimeOrigin: Long,           // Hardware monotonic reference
    val breakAllowed: Int,                     // Total breaks allowed
    val breaksTaken: Int,                      // Breaks used so far
    val breakRemainingSeconds: Int = 0,        // Active break countdown
    val stateVersion: Long,                    // Monotonically increasing revision counter
    val updatedTimestampUtc: Long              // Last state transition UTC
)
```

---

### 5.3 State Transition Synchronization Matrix

| Event on Device A | Propagated State to Device B | Device B Immediate Response |
|---|---|---|
| **User Starts Session on Phone** | `status = 'ACTIVE'`, `preset = 'Deep Work'` | Linux daemon backs up `/etc/hosts`, writes block rules, launches Desktop Focus HUD. |
| **User Starts Session on Linux** | `status = 'ACTIVE'`, `preset = 'Deep Work'` | Android foreground service starts, locks accessibility overlay on blocked apps. |
| **Break Granted on Either Device** | `status = 'PAUSED_BREAK'`, `breakRemaining = 300` | Both devices temporarily unblock distraction filters for exactly 5 minutes with synchronized break HUD. |
| **Break Expires** | `status = 'ACTIVE'` | Both devices re-engage blocking rules simultaneously. |
| **Session Natural Completion** | `status = 'COMPLETED'` | Both devices restore clean state, display Reflection Sheet (`FF_P2_POST_SESSION_REFLECTION_SPEC.md`). |
| **Emergency Quit Initiated** | Requires asymmetric quit modal on active screen | Once 15–25s gate finishes on device A, `status = 'QUIT'` propagates; device B unlocks. |

---

### 5.4 Conflict Resolution: "Active Discipline Dominance"

When concurrent actions occur:
1. **Focus Overrides Idle:** If Device A starts a focus session while Device B is idle, Device B **must** enter focus mode.
2. **Strict Quit Gate:** A session cannot be bypassed on Linux by quitting casually; quitting on either device requires completing the 15–25s asymmetric quit modal.
3. **State Version Clock (LWW):** State transitions increment `stateVersion`. Higher version numbers supersede lower versions, protected by monotonic clock timestamps to prevent NTP tampering.

---

## 6. Cross-Device Aggregated App Limits & Global Screen Time Pooling

### 6.1 The Siloed Device Trap & The Flux Solution
In digital wellbeing tools like Regain, application limits are siloed per device:
* A user with a phone, tablet, and laptop sets 30m on each device.
* **Vulnerability 1: Device Hopping.** When the phone hits 30m, the user simply hops to the tablet for 30m, then the laptop for 30m, accumulating 90 minutes of aggregate doomscrolling.
* **Vulnerability 2: The Travel Penalty.** When traveling with only a smartphone for a week, the user only has access to 30m of their intended 1-hour daily budget. Increasing the limit artificially breaks their streak or distorts analytics.

**FluxFoxus solves this with Cross-Device Aggregated App Limits:**
* **Single Global Ceiling:** A daily limit (e.g. 60m Instagram) applies to the **sum of usage across all paired devices**.
* **Automatic Travel Rebalancing:** If tablet and laptop have 0 minutes logged today, the smartphone automatically has access to the full 60-minute pool without requiring any limit adjustments or risking streak breakage.

---

### 6.2 Mathematical Model & Realtime Aggregation Pipeline

1. **Per-Device Logging:**
   * Android (Phone & Tablet): Logged via `UsageStatsManager` in `UsageStatsDataHub`.
   * Ubuntu Linux: Logged via `fluxfoxus-daemon` active window / browser domain monitor.
2. **Sync Frequency:**
   * Whenever an app in `app_limits` or a category in `category_limits` is in the foreground, the device broadcasts a lightweight delta ping every 60 seconds (or immediately on app close/switch) over LAN mDNS socket (<20ms) and Firebase Realtime (<300ms).
3. **Global Usage Formula:**
   $$\text{GlobalUsageToday}(p) = \text{LocalUsage}(p) + \sum_{d \in \text{RemoteDevices}} \text{DeviceUsage}(p, d)$$
   $$\text{RemainingGlobalBudget}(p) = B_{\text{global}}(p) - \text{GlobalUsageToday}(p)$$
4. **Universal Multi-Device Lock:**
   * When $\text{GlobalUsageToday}(p) \ge B_{\text{global}}(p)$, the 5/10/20/40m intervention overlay or desktop shield engages on **all paired devices simultaneously**.
   * If a user consumes their 59th and 60th minute of Instagram on their laptop, picking up their phone immediately triggers the intervention overlay on launch.

---

### 6.3 Room Database Schema: `device_usage_records`

```kotlin
package com.flux.focus.features.sync.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "device_usage_records",
    primaryKeys = ["date", "target_identifier", "device_id"],
    indices = [
        Index("date"),
        Index("target_identifier")
    ]
)
data class DeviceUsageRecordEntity(
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "target_identifier")
    val targetIdentifier: String, // package_name (e.g. "com.instagram.android") or category (e.g. "DISTRACTING")

    @ColumnInfo(name = "device_id")
    val deviceId: String, // UUID of hardware device

    @ColumnInfo(name = "device_name")
    val deviceName: String, // e.g. "Pixel 8 Pro", "ThinkPad P14s", "Galaxy Tab S9"

    @ColumnInfo(name = "device_type")
    val deviceType: String, // "PHONE", "TABLET", "DESKTOP_LINUX"

    @ColumnInfo(name = "usage_minutes")
    val usageMinutes: Int,

    @ColumnInfo(name = "last_updated_utc")
    val lastUpdatedUtc: Long
)
```

---

### 6.4 UI Specifications: Aggregated Cards & Device Breakdowns

#### On `Tab.Limits`:
```
+-------------------------------------------------------+
|  Instagram                              50m / 60m     |
|  [=====================================....] 10m left |
|                                                       |
|  DEVICE CONTRIBUTIONS:                                |
|  📱 Pixel 8 Pro (This Device):  25m                   |
|  💻 ThinkPad P14s (Desktop):    15m                   |
|  📱 Galaxy Tab S9:              10m                   |
+-------------------------------------------------------+
```

#### On `Tab.Stats`:
* **Device Filter Segmented Control:**  
  `[ All Devices (Aggregated) ]` | `[ Pixel 8 Pro ]` | `[ ThinkPad P14s ]` | `[ Galaxy Tab S9 ]`
* Selecting a specific device filters the charts to that device's local contributions; default is always **All Devices (Aggregated)**.

---

### 6.5 Offline Disconnection & Midnight Streak Reconciliation
1. **Offline Operation:** If a device loses internet and LAN connectivity, it enforces limits against $\text{LastKnownRemoteUsage} + \text{LocalOfflineUsage}$.
2. **Reconnection Catch-Up:** Upon reconnection, the device sends its accumulated offline usage minutes. If the sum exceeds the budget, the app locks immediately on all synced devices.
3. **Streak Integrity:** Streaks evaluate against the final aggregated midnight total. If global usage remained within budget, the streak advances without regard to how minutes were distributed across devices.

---

## 7. Linux Desktop UI Specifications (Compose Multiplatform)

The desktop companion UI adheres strictly to the Holst Dark Palette tokens.

### 7.1 Desktop Focus HUD (Minimalist Floating Pill)

A lightweight, non-intrusive HUD positioned at the top-right of the desktop screen:

```
+-------------------------------------------------------+
|  ⚡ DEEP WORK   42:15   [ ☕ Take Break ]   [ ⏹ Quit ] |
+-------------------------------------------------------+
```
* **Background:** `HolstTheme.colors.surfaceCard` (`#1B263B`).
* **Border:** `HolstTheme.colors.borderSubtle` (`#415A77`).
* **Timer Text:** `HolstTheme.colors.textPrimary` (`#F4F1DE`), bold monospace font.
* **Accent:** `HolstTheme.colors.accentSage` (`#778D7A`).
* **Draggable & Always-On-Top:** Glides cleanly on GNOME / KDE / Wayland desktop sessions.

---

---

## 8. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class CrossDeviceSyncEngineTest {

    private lateinit var syncRepository: FakeSyncRepository
    private lateinit var syncEngine: CrossDeviceSyncEngine
    private lateinit var deviceUsageDao: DeviceUsageRecordDao
    private lateinit var appLimitDao: AppLimitDao

    @Before
    fun setup() {
        syncRepository = FakeSyncRepository()
        syncEngine = CrossDeviceSyncEngine(syncRepository)
    }

    @Test
    fun startingSessionOnMobile_broadcastsActiveStateToDesktop() = runTest {
        val session = createSampleSession("session-123", preset = "Olympiad Math", duration = 60)
        
        syncEngine.publishSessionStart(session)

        val publishedState = syncRepository.latestBroadcastState.first()
        assertEquals("session-123", publishedState.sessionId)
        assertEquals("ACTIVE", publishedState.status)
        assertEquals("Olympiad Math", publishedState.presetName)
    }

    @Test
    fun breakGrantedOnDesktop_propagatesToMobileWithinTolerance() = runTest {
        val breakUpdate = SyncedFocusState(
            sessionId = "session-123",
            sourceDeviceId = "linux-workstation",
            presetId = "preset-1",
            presetName = "Deep Work",
            status = "PAUSED_BREAK",
            startTimestampUtc = System.currentTimeMillis(),
            durationMinutes = 60,
            elapsedRealtimeOrigin = 1000L,
            breakAllowed = 2,
            breaksTaken = 1,
            breakRemainingSeconds = 300,
            stateVersion = 2L,
            updatedTimestampUtc = System.currentTimeMillis()
        )

        syncEngine.handleIncomingState(breakUpdate)

        assertEquals(SessionStatus.PAUSED_BREAK, syncEngine.currentLocalSessionState.value.status)
        assertEquals(300, syncEngine.currentLocalSessionState.value.breakRemainingSeconds)
    }

    @Test
    fun crossDeviceUsageAggregation_locksAppWhenSumExceedsGlobalBudget() = runTest {
        // Given: Instagram has 60m global daily budget
        // Phone used: 35m, Laptop used: 20m, Tablet used: 10m -> Total = 65m (> 60m)
        val today = "2026-10-13"
        val records = listOf(
            DeviceUsageRecordEntity(today, "com.instagram.android", "dev-phone", "Pixel 8 Pro", "PHONE", 35, System.currentTimeMillis()),
            DeviceUsageRecordEntity(today, "com.instagram.android", "dev-pc", "ThinkPad P14s", "DESKTOP_LINUX", 20, System.currentTimeMillis()),
            DeviceUsageRecordEntity(today, "com.instagram.android", "dev-tab", "Galaxy Tab S9", "TABLET", 10, System.currentTimeMillis())
        )
        
        val totalGlobalUsage = syncEngine.calculateGlobalUsage("com.instagram.android", records)
        assertEquals(65, totalGlobalUsage)
        
        val isBlocked = syncEngine.isAppBlockedGlobally("com.instagram.android", globalBudgetMinutes = 60, records = records)
        assertTrue(isBlocked)
    }
}
```

### 8.1 Linux Daemon Bash Self-Healing Unit Test
```bash
#!/usr/bin/env bash
# Test hosts backup and restoration integrity
set -euo pipefail

# 1. Setup mock environment
TMP_DIR=$(mktemp -d)
HOSTS_FILE="$TMP_DIR/hosts"
BAK_FILE="$TMP_DIR/hosts.flux.bak"

echo "127.0.0.1 localhost" > "$HOSTS_FILE"
echo "192.168.1.5 nas.local" >> "$HOSTS_FILE"

# 2. Simulate daemon backup & injection
cp -p "$HOSTS_FILE" "$BAK_FILE"
echo "### FLUXFOXUS_SESSION_BLOCK_START ###" >> "$HOSTS_FILE"
echo "0.0.0.0 reddit.com" >> "$HOSTS_FILE"
echo "### FLUXFOXUS_SESSION_BLOCK_END ###" >> "$HOSTS_FILE"

# Assert injection occurred
grep -q "reddit.com" "$HOSTS_FILE"

# 3. Simulate daemon restoration
sed -i '/### FLUXFOXUS_SESSION_BLOCK_START ###/,/### FLUXFOXUS_SESSION_BLOCK_END ###/d' "$HOSTS_FILE"

# Assert hosts matches backup perfectly
diff -u "$HOSTS_FILE" "$BAK_FILE"
echo "✓ Linux Hosts Backup & Restoration Test Passed"

rm -rf "$TMP_DIR"
```

---

## 9. Deliverables & Acceptance Checklist

- [ ] **Dual-Tier Hosts Backup:** Daemon creates pristine `/etc/hosts.flux.orig` on install and `/etc/hosts.flux.bak` immediately before every session modification.
- [ ] **Demarcated Block Injection:** Blocked domains cleanly inserted and stripped between explicit `# FLUXFOXUS_SESSION_BLOCK_...` markers.
- [ ] **Desktop App Guard:** D-Bus / window listener detects blocked desktop apps and deploys Focus Shield overlay.
- [ ] **Crash Self-Healing:** Systemd daemon checks for and cleanses orphaned block markers on boot/restart.
- [ ] **Google Account Pairing:** One-tap Google Sign-In on both Android and Linux clients with secure token storage in Android Keystore and Linux Keyring.
- [ ] **Hybrid Sync (mDNS LAN + Cloud):** Synchronizes focus state in $<20\text{ms}$ on local Wi-Fi and $<300\text{ms}$ over mobile broadband.
- [ ] **Active Discipline Dominance:** Starting a session on either device locks both; breaks and completions mirror seamlessly.
- [ ] **Cross-Device Aggregated App Limits:** Screen time across phone, tablet, and laptop pools into a unified daily budget; breaching the shared budget locks the app on all devices.
- [ ] **Per-Device Contribution Breakdown:** `Tab.Limits` and `Tab.Stats` display multi-device usage breakdown chips (`Pixel 8 Pro: 25m • ThinkPad: 15m`).
- [ ] **Travel Freedom:** 0-usage devices leave the entire budget intact for the traveling device with zero manual limit edits and zero streak penalties.
- [ ] **Holst Desktop UI:** Compose Multiplatform Desktop HUD styled with Holst dark tokens (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#F4F1DE`).
