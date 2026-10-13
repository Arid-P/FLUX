# 🔐 Topic SEC-01: Unified Secure Authentication & Identity Lifecycle Specification

**Document ID:** `SEC_01_SECURE_AUTHENTICATION_AND_IDENTITY_SPEC`  
**Phase:** Security, Authentication & System Validation Suite  
**Target Packages:** `com.flux.core.security.auth` (Android) & `com.flux.desktop.security.auth` (Ubuntu Linux Desktop)  
**Architecture:** Clean Architecture / Zero-Trust Identity Model (Kotlin 2.x + Android Credential Manager + OAuth 2.0 PKCE + Hardware Keystore + BiometricPrompt)  
**Applicability:** Universal across both **FluxDone (FD)** and **FluxFoxus (FF)**  

---

## 1. Overview & Security Philosophy

A productivity and discipline suite must protect user privacy and identity without introducing unnecessary complexity. Because Flux adheres to the **$0 Free-Forever Architecture**, authentication serves two primary goals:
1. **Google Drive Cloud Sync & Google Calendar Binding:** Authorizing access to the hidden `appDataFolder` and calendar overlay without managing third-party backend servers.
2. **Device Identity & Zero-Trust Session Isolation:** Ensuring that each paired node (Phone, Tablet, Linux Workstation) communicates over authenticated channels with zero risk of credential theft.

---

## 2. Authentication Implementations by Platform

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                         UNIFIED FLUX AUTHENTICATION PIPELINE                                |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   ┌─────────────────────────────────────────┐     ┌─────────────────────────────────────┐   |
|   │         ANDROID (PHONE & TABLET)        │     │         UBUNTU LINUX DESKTOP        │   |
|   │  • androidx.credentials API             │     │  • OAuth 2.0 with PKCE (RFC 7636)   │   |
|   │  • GoogleIdTokenCredential              │     │  • Loopback HTTP: 127.0.0.1:<port>  │   |
|   │  • Scopes: drive.appdata, calendar      │     │  • System Browser (xdg-open)        │   |
|   └────────────────────┬────────────────────┘     └──────────────────┬──────────────────┘   |
|                        │                                             │                      |
|                        ▼                                             ▼                      |
|   ┌─────────────────────────────────────────┐     ┌─────────────────────────────────────┐   |
|   │       ANDROID HARDWARE KEYSTORE         │     │         LINUX SECRET SERVICE        │   |
|   │  • AES-256 GCM MasterKey                │     │  • GNOME Keyring / libsecret        │   |
|   │  • EncryptedSharedPreferences           │     │  • Secure D-Bus org.freedesktop     │   |
|   └─────────────────────────────────────────┘     └─────────────────────────────────────┘   |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

### 2.1 Android Platform: Android Credential Manager API

Flux standardizes on modern `androidx.credentials.CredentialManager` (deprecating legacy GoogleSignInClient):

```kotlin
package com.flux.core.security.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

class AndroidAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(serverClientId: String): Result<AuthTokens> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            // Extract Google ID Token and exchange for OAuth Authorization Code
            val tokens = TokenExchangeService.exchangeCodeForTokens(credential)
            Result.success(tokens)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        }
    }
}
```

### 2.2 Ubuntu Linux Desktop: OAuth 2.0 with PKCE (RFC 7636)

On Desktop Linux, where native Google Play Services are absent, FluxDesktop implements **Authorization Code Flow with Proof Key for Code Exchange (PKCE)**:
1. **Code Verifier & Challenge:** Generates a cryptographically random 128-byte `code_verifier` and SHA-256 `code_challenge`.
2. **Ephemeral Local Loopback:** Binds an ephemeral local socket on `http://127.0.0.1:<random-port>/oauth2callback`.
3. **Browser Launch:** Launches the default Linux browser via `xdg-open` targeting Google's OAuth consent screen:
   ```
   https://accounts.google.com/o/oauth2/v2/auth?
     client_id=...&
     redirect_uri=http://127.0.0.1:45678/oauth2callback&
     response_type=code&
     scope=https://www.googleapis.com/auth/drive.appdata%20https://www.googleapis.com/auth/calendar.readonly&
     code_challenge=...&
     code_challenge_method=S256
   ```
4. **Token Capture & Server Termination:** Captures the one-time authorization code, completes the exchange with Google OAuth token endpoints, and immediately closes the local socket.

---

## 3. Hardware-Backed Token Vault & Key Storage

Access tokens and refresh tokens must **never** reside in plain text JSON, unencrypted SQLite tables, or standard preferences.

### 3.1 Android Storage: Android Keystore + `EncryptedSharedPreferences`

Tokens are encrypted using hardware-backed AES-256 GCM keys generated inside the **Android Keystore Provider**:

```kotlin
package com.flux.core.security.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureTokenStorageAndroid {
    private const val PREFS_FILE = "flux_secure_tokens.enc"

    fun getSecurePreferences(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### 3.2 Linux Storage: GNOME Keyring via Secret Service D-Bus

On Ubuntu Linux, tokens are persisted in the user's encrypted **GNOME Keyring** using the FreeDesktop `org.freedesktop.secrets` interface:
* **Item Label:** `"Flux Done Cloud Credentials"`
* **Attributes:** `application = "fluxdone"`, `user_id = "<google-sub-id>"`
* **Permissions:** Accessible only when the user's Linux desktop session is unlocked.

---

## 4. Token Rotation & Silent Refresh Engine

To prevent authentication interruptions during deep study sessions, Flux implements an autonomous **Silent Token Refresh Worker**:

```
[ Active Access Token ]
          │
          ▼
[ Current Time >= (Expiry Epoch - 5 Minutes) ]
          │
     ┌────┴─────────────────────────────┐
     ▼ YES                              ▼ NO
[ Execute Token Refresh ]          [ Continue Normal Operations ]
     │
     ├─► Success: Persist new Access Token in Keystore; reset expiry timer
     └─► Failure: Exponential backoff (10s, 30s, 60s); fallback to cached offline mode
```

### 4.1 Token Revocation on Logout

When the user taps *"Disconnect Account"* in Settings:
1. Asynchronously invokes `https://oauth2.googleapis.com/revoke?token=<refresh_token>`.
2. Hardware Keystore / Keyring wipes all stored access and refresh tokens.
3. In-memory authentication state transitions to `AuthState.Unauthenticated`.
4. Local SQLite database remains fully accessible in local offline-only mode.

---

## 5. Biometric & Device PIN Step-Up Authentication

To protect sensitive actions from accidental execution or tampering, Flux enforces **Biometric Step-Up Authentication**:

### 5.1 Sensitive Protected Actions

1. **Permanent Database Reset / Wipe**
2. **Manual SQLite Snapshot Restoration (Overwriting Local Data)**
3. **Disabling FluxFoxus Uninstall Protection or Altering Emergency PINs**
4. **Exporting Raw Backup to External Storage**

### 5.2 Implementation Specification (`BiometricAuthGate.kt`)

```kotlin
package com.flux.core.security.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthGate {
    fun authenticateSensitiveAction(
        activity: FragmentActivity,
        actionTitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(actionTitle)
            .setSubtitle("Confirm your identity with fingerprint, face, or device PIN to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
```

---

## 6. Zero-Trust Multi-Device Session Isolation

1. **Cryptographic Device Identifier:** Upon first launch, each device generates a permanent `device_id = UUID.randomUUID().toString()`, cryptographically signed and stored in the Keystore.
2. **Manifest Device Binding:** All differential sync uploads to Google Drive `appDataFolder` include `device_id` and an incrementing monotonic sequence number (`client_sequence_id`).
3. **Ghost Session Rejection:** If a device detects a corrupted or unauthorized sequence skip in the manifest, it refuses to apply incoming mutations until a clean snapshot integrity check passes.

---

## 7. Verification & Security Testing Contracts

1. **Keystore Encryption Verification Test:** Inspect the physical XML preferences file `/data/data/com.fluxdone/shared_prefs/flux_secure_tokens.enc.xml`. Verify that all keys and values are ciphertext and neither the refresh token nor client secret appear as plain text strings.
2. **PKCE Verification Test (Desktop):** Execute desktop authentication. Intercept outgoing token request; verify `code_verifier` matches the SHA-256 hash of `code_challenge`.
3. **Silent Refresh 5-Minute Window Test:** Mock access token expiry at $T + 240\text{s}$. Verify that the background token refresh triggers automatically at $T = 0$ without displaying any UI consent prompts.
4. **Biometric Gate Test:** Attempt to restore a backup file. Verify that the file restore is blocked until `onAuthenticationSucceeded` is signaled by `BiometricPrompt`.
