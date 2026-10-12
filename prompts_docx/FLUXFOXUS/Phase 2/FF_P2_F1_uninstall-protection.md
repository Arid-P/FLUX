# FF Phase 2 — Feature 1: Uninstall Protection

**Version:** 1.0  
**Phase:** 2  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Uninstall Protection prevents the user from uninstalling FluxFoxus impulsively — particularly during a moment of frustration with a focus session or an app limit enforcement. The feature adds deliberate friction to the uninstall flow without making removal truly impossible.

**This is not a security feature.** It is a friction mechanic consistent with FF's core philosophy (PRD §1.2: "Discipline over motivation. Friction makes stopping expensive."). A factory reset or ADB command will always bypass it — that is acceptable.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Core philosophy — friction mechanics | PRD §1.2 |
| Streak system — reset on quit | PRD §3.5.4 |
| Stop Focusing modal — friction before quitting | PRD §3.4 |
| App Limits turn-off flow — 3-second countdown | PRD §3.6.5 |

The uninstall protection pattern follows the same friction escalation model used in the Stop Focusing modal (§3.4) and the App Limits turn-off flow (§3.6.5).

---

## 3. Implementation Approach

**Android Device Admin API** (`DevicePolicyManager`).

FF registers as a Device Administrator. Android blocks uninstallation of any app that holds active Device Admin privileges. To uninstall, the user must first manually revoke Device Admin status — which FF intercepts and handles with a friction flow.

### 3.1 Why Device Admin (not alternatives)

| Approach | Verdict |
|---|---|
| Device Admin API | ✅ Chosen. Play Store compliant, well-documented, clean revocation hook |
| Accessibility Service blocking | ❌ Fragile, breaks on Android updates, not designed for this |
| Root-level protection | ❌ Out of scope entirely |

---

## 4. Feature Specification

### 4.1 Activation

- On first launch (or during onboarding), FF requests Device Administrator privileges
- Plain-language explanation screen shown before opening Android's Device Admin grant screen
- Explanation text: *"FluxFoxus needs Device Administrator access to prevent impulsive uninstalls. You can always remove this in Settings if you genuinely want to uninstall."*
- If user denies: Uninstall Protection is inactive. A persistent but dismissible banner appears on the Home screen Settings section noting it is off
- User can enable it later via Settings → Security → Uninstall Protection

### 4.2 Revocation Intercept Flow

When the user navigates to Android Settings → Apps → FluxFoxus → Uninstall, Android requires Device Admin to be revoked first. FF's `DeviceAdminReceiver.onDisableRequested()` is called — FF uses this hook to show the friction modal.

**Friction Modal — Step 1:**
- Full-screen overlay (not dismissible by back tap)
- Icon: flame (streak) in Electric Indigo
- Primary text: *"Remove uninstall protection?"*
- Secondary text: *"This will allow FluxFoxus to be uninstalled. Your streak and all session history will be permanently lost."*
- Streak display: *"[X] days → 0 days"* (same pattern as Stop Focusing modal, PRD §3.4.2)
- Two buttons, both disabled on appear:
  - **"Keep Protection"** (Electric Indigo, primary)
  - **"Remove Protection"** (danger red, secondary)
- Countdown timer: **10 seconds** before both buttons activate (shorter than Stop Focusing modal's 15–25s because this is a settings action, not a mid-session action)
- Countdown label: *"Please wait [N]s"* below buttons

**On "Keep Protection":** modal closes, Device Admin revocation is cancelled, user returns to Android Settings

**On "Remove Protection":**
- Device Admin is revoked
- FF shows a final confirmation snackbar: *"Uninstall protection removed. You can re-enable it in FF Settings."*
- User can now uninstall normally

### 4.3 Settings Entry Point

**Location:** Settings screen → new section: **Security**

| Control | Type | Behaviour |
|---|---|---|
| Uninstall Protection | Toggle (on/off) | ON = Device Admin active. OFF = triggers same friction modal as revocation flow |
| Status label | Static text | *"Active"* (Soft Cyan) / *"Inactive"* (secondary text colour) |

**P1 reference:** Settings screen structure is defined in `ui_SCR-09_settings.md` (FD). FF's settings UI spec is `ui_home.md` and `ui_navigation.md`. The Security section is a new addition to FF's Settings in P2 — it does not exist in P1.

### 4.4 Onboarding Integration

- Device Admin request is added to the P1 permission request order (TRD §7.1) as step 5 (after all existing permissions)
- It is the only optional permission in the onboarding flow — all others are required for core function
- If skipped during onboarding: banner on Home screen (dismissible, reappears on next launch until enabled or explicitly dismissed permanently)

---

## 5. Data & Storage

No new SQLite tables required.

| Key | Store | Value |
|---|---|---|
| `uninstall_protection_enabled` | Hive (user preferences) | `bool` |
| `uninstall_protection_dismissed_banner` | Hive | `bool` |

---

## 6. New Permissions

```xml
<receiver android:name=".FluxFoxusDeviceAdminReceiver"
    android:permission="android.permission.BIND_DEVICE_ADMIN">
  <meta-data android:name="android.app.device_admin"
      android:resource="@xml/device_admin_config" />
  <intent-filter>
    <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
    <action android:name="android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED" />
  </intent-filter>
</receiver>
```

```xml
<!-- device_admin_config.xml -->
<device-admin>
  <uses-policies>
    <!-- No destructive policies needed — Device Admin is used only for uninstall block -->
  </uses-policies>
</device-admin>
```

**Important:** FF must not request any destructive Device Admin policies (wipe data, lock screen password enforcement, etc.). The sole purpose is uninstall blocking. Requesting extra policies would be misleading to the user and a Play Store risk.

---

## 7. Limitations (Document Explicitly)

- **Factory reset bypasses this entirely.** This is expected and acceptable.
- **ADB `pm uninstall` bypasses this.** Acceptable — this is a friction mechanic, not a security lock.
- **Android 12+ Device Admin deprecation:** Google is progressively deprecating certain Device Admin policies but the basic Device Admin registration (used here) remains supported. Monitor with each Android release.
- **Play Store policy:** Using Device Admin solely for uninstall protection is compliant as long as FF does not request destructive policies. Must be re-verified at Play Store submission time.

---

## 8. Module Boundary

**New module:** `security/`

```
features/
└── security/
    ├── data/
    │   └── device_admin_repository_impl.dart
    ├── domain/
    │   ├── i_device_admin_repository.dart
    │   └── use_cases/
    │       ├── enable_device_admin.dart
    │       └── disable_device_admin.dart
    └── presentation/
        ├── uninstall_protection_modal.dart
        └── security_settings_section.dart
```

No existing P1 modules are modified except:
- `settings/` — new Security section added to settings screen
- `core/onboarding/` — Device Admin request added as optional final step
