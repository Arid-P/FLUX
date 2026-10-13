# 🛡️ Topic SEC-02: Cryptographic Security, Data Encryption at Rest & Tamper Resistance Specification

**Document ID:** `SEC_02_CRYPTOGRAPHIC_SECURITY_AND_DATA_ENCRYPTION_SPEC`  
**Phase:** Security, Authentication & System Validation Suite  
**Target Packages:** `com.flux.core.security.crypto` (Android) & `com.flux.desktop.security.crypto` (Ubuntu Linux Desktop)  
**Architecture:** Military-Grade Cryptography (256-Bit AES SQLCipher + Hardware Keystore + TLS 1.3 + Android Signature-Level IPC + Anti-Hooking Defense)  
**Applicability:** Universal across both **FluxDone (FD)** and **FluxFoxus (FF)**  

---

## 1. Threat Modeling & Core Defense Objectives

The Flux Family operates in an adversarial environment:
1. **Adversary 1 (Rogue Third-Party Apps):** Malicious apps on the user's device could attempt to forge IPC broadcasts (e.g. sending fake `FOCUS_SESSION_COMPLETED` broadcasts to artificially cheat focus streaks, or forging `FOCUS_BLOCK_SYNC` to lock out device access).
2. **Adversary 2 (Local Physical Attacker / Device Theft):** If Ari's phone or laptop is lost or inspected, physical extraction of SQLite database files must yield **zero plain text notes, tasks, or credentials**.
3. **Adversary 3 (Procrastinating Self / Hooking Bypasses):** A student under academic stress might attempt to use root tools (Frida, Xposed, Magisk) to bypass FluxFoxus lock-screens and app blockades.

---

## 2. Encryption at Rest: Transparent SQLCipher 256-Bit AES

All local Room databases (`fluxdone.db` and `fluxfoxus.db`) are encrypted using **SQLCipher 4.x** with 256-bit AES encryption.

```
+─────────────────────────────────────────────────────────────────────────────+
|                     DATABASE ENCRYPTION AT REST ARCHITECTURE                 |
+─────────────────────────────────────────────────────────────────────────────+
|                                                                             |
|   1. Hardware Keystore Generates 256-bit High-Entropy Master Seed           |
|      (Stored in Android Keystore / Linux GNOME Keyring)                     |
|                                │                                            |
|                                ▼                                            |
|   2. PBKDF2 Key Derivation (64,000 Iterations + SHA-512)                    |
|                                │                                            |
|                                ▼                                            |
|   3. Transparent Page-Level Cipher (256-bit AES-CBC / HMAC-SHA512)           |
|      ┌──────────────────────────────────────────────────────────────────┐   |
|      │ Physical Storage on Disk: /data/.../databases/fluxdone.db        │   |
|      │ 100% Ciphertext. Zero plain-text titles, notes, or timestamps.   │   |
|      └──────────────────────────────────────────────────────────────────┘   |
+─────────────────────────────────────────────────────────────────────────────+
```

### 2.1 Android Database Encryption Configuration

```kotlin
package com.flux.core.security.crypto

import android.content.Context
import androidx.room.Room
import com.flux.done.database.FluxDoneDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

object EncryptedDatabaseFactory {
    fun buildEncryptedRoomDatabase(context: Context, dbName: String): FluxDoneDatabase {
        val passphrase = DatabaseKeyProvider.getOrCreateDatabasePassphrase(context)
        val supportFactory = SupportFactory(passphrase)

        return Room.databaseBuilder(context, FluxDoneDatabase::class.java, dbName)
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration()
            .build()
    }
}

object DatabaseKeyProvider {
    private const val KEY_ALIAS = "flux_sqlite_master_key"

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = SecureTokenStorageAndroid.getSecurePreferences(context)
        val existingKeyHex = prefs.getString(KEY_ALIAS, null)
        
        if (existingKeyHex != null) {
            return hexStringToByteArray(existingKeyHex)
        }

        // Generate cryptographically secure 256-bit (32 bytes) random seed
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        
        prefs.edit().putString(KEY_ALIAS, byteArrayToHex(randomBytes)).apply()
        return randomBytes
    }

    private fun byteArrayToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun hexStringToByteArray(s: String): ByteArray = ByteArray(s.length / 2) { i ->
        s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
```

---

## 3. Encryption in Transit: Strict TLS 1.3 & Certificate Transparency

All network communication (Google Drive `appDataFolder` sync, Google Calendar overlay REST API) enforces **strict TLS 1.3**:

1. **Cleartext Traffic Disabled:** In `AndroidManifest.xml`, cleartext is explicitly blocked:
   ```xml
   <application
       android:usesCleartextTraffic="false"
       android:networkSecurityConfig="@xml/network_security_config" ... />
   ```
2. **Network Security Config (`network_security_config.xml`):**
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
       <domain-config>
           <domain includeSubdomains="true">googleapis.com</domain>
           <pin-set expiration="2027-01-01">
               <!-- Backup Google Root CA Pins -->
               <pin digest="SHA-256">WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=</pin>
           </pin-set>
       </domain-config>
   </network-security-config>
   ```

---

## 4. Signature-Protected Inter-Process Communication (IPC) Defense

To prevent rogue applications from forging inter-app broadcasts (such as sending a spoofed `FOCUS_SESSION_COMPLETED` intent to prematurely unblock phone apps), all IPC broadcasts and receivers enforce **Android Signature-Level Permissions**.

### 4.1 Custom Signature Permission Definition

Both `FluxDone` and `FluxFoxus` declare and share custom signature-level permissions in their respective manifests:

```xml
<!-- Defined in both AndroidManifest.xml files -->
<permission
    android:name="com.flux.permission.INTER_APP_DISCIPLINE_SYNC"
    android:protectionLevel="signature" />

<uses-permission android:name="com.flux.permission.INTER_APP_DISCIPLINE_SYNC" />
```

### 4.2 Protected Receiver Configuration

```xml
<receiver
    android:name="com.flux.done.integration.ff.FocusSessionCompletedReceiver"
    android:permission="com.flux.permission.INTER_APP_DISCIPLINE_SYNC"
    android:exported="true">
    <intent-filter>
        <action android:name="com.flux.action.FOCUS_SESSION_COMPLETED" />
        <action android:name="com.flux.action.FOCUS_SESSION_ABORTED" />
    </intent-filter>
</receiver>
```

### 4.3 Runtime Certificate Verification Gate (Defense-in-Depth)

Even within registered receivers, Flux validates the sender's cryptographic signing certificate at runtime before processing payloads:

```kotlin
package com.flux.core.security.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build

object SignatureVerificationGate {
    fun verifyCallingAppSignature(context: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        val packageManager = context.packageManager
        val packages = packageManager.getPackagesForUid(callingUid) ?: return false

        val ownSignatures = getPackageSignatures(context, context.packageName)

        for (pkg in packages) {
            val callerSignatures = getPackageSignatures(context, pkg)
            if (callerSignatures.intersect(ownSignatures).isNotEmpty()) {
                return true // Caller signed with identical developer certificate
            }
        }
        return false // Unauthorized untrusted caller!
    }

    @Suppress("DEPRECATION")
    private fun getPackageSignatures(context: Context, packageName: String): Set<String> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo.apkContentsSigners.map { it.toCharsString() }.toSet()
        } else {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            info.signatures.map { it.toCharsString() }.toSet()
        }
    }
}
```

---

## 5. Anti-Tamper & Root/Hook Resistance (FluxFoxus Enforcer)

To ensure that scheduled focus blocks cannot be circumvented via debugging hooks during deep study periods:

1. **Frida & Xposed In-Memory Detection:** Scans memory maps (`/proc/self/maps`) for suspicious libraries (`frida-agent.so`, `libxposed.so`).
2. **Debugging Port Check:** Detects active `ptrace` or listening debugger ports on standard debugging ports ($27042$).
3. **Quarantine Defense Policy:**
   * If memory tampering is detected while a focus block is active:
   * The app **locks down in Emergency Quarantine**: writes a security incident to encrypted logs, maintains distraction shielding, and notifies the user upon session completion.

---

## 6. Verification & Security Testing Contracts

1. **Physical Ciphertext Test:** Extract `fluxdone.db` via adb root / backup. Inspect raw bytes using `strings fluxdone.db | grep -i "Geometry"`. Verify that zero plain text task titles or notes appear in raw database pages.
2. **Spoofed Intent Rejection Test:** Create a mock untrusted test app with a different signature key. Attempt to send `com.flux.action.FOCUS_SESSION_COMPLETED`. Verify that the Android OS blocks the intent with a `SecurityException`, and `FocusSessionCompletedReceiver` is never invoked.
3. **TLS Cleartext Block Test:** Attempt an HTTP connection to `http://example.com`. Verify that Android's NetworkSecurityPolicy terminates the socket immediately with `IOException: Cleartext HTTP traffic not permitted`.
4. **Signature Gate Runtime Test:** Verify that `SignatureVerificationGate.verifyCallingAppSignature()` returns `true` exclusively for official Flux builds and returns `false` for third-party mock packages.
