package com.fluxfoxus.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Focus Accessibility Service
 *
 * Enforces discipline during active focus sessions:
 * - Detects foreground window changes (`TYPE_WINDOW_STATE_CHANGED`)
 * - Redirects blocked apps back to Android Home launcher
 * - Multi-state content inspection for YouTube channels and browser domains against whitelists
 */
class FocusAccessibilityService : AccessibilityService() {

    companion object {
        val SYSTEM_PACKAGES = setOf(
            "com.fluxfoxus",
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oppo.launcher"
        )

        const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser"
        )

        /**
         * Helper to verify if any extracted on-screen text matches whitelisted content.
         */
        fun isContentWhitelisted(
            extractedTexts: List<String>,
            whitelistedItems: List<GlobalContentWhitelistEntity>
        ): Boolean {
            if (whitelistedItems.isEmpty()) return false
            val combinedText = extractedTexts.joinToString(" ").lowercase()

            return whitelistedItems.any { item ->
                val target = item.targetIdentifier.lowercase().trimStart('@')
                val label = item.displayLabel.lowercase()
                combinedText.contains(target) || combinedText.contains(label)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (SYSTEM_PACKAGES.contains(packageName)) return

        val timerState = FocusTimerService.timerState.value
        if (timerState !is TimerState.Running || timerState.isBreak) {
            // No active session or currently in break mode -> allow app
            return
        }

        serviceScope.launch {
            inspectAndEnforce(packageName, timerState.presetId)
        }
    }

    private suspend fun inspectAndEnforce(packageName: String, presetId: String) {
        val db = FluxFoxusDatabase.getDatabase(applicationContext)
        val appRule = db.presetDao().getAppRule(presetId, packageName)

        if (appRule != null && appRule.ruleMode == "BLOCKED") {
            redirectToHome()
            return
        }

        if (appRule?.ruleMode == "STUDY_MODE") {
            if (packageName == YOUTUBE_PACKAGE) {
                val whitelists = db.whitelistDao().getWhitelistItemsByType("YOUTUBE_CHANNEL").first()
                val onScreenTexts = extractOnScreenTexts(rootInActiveWindow)
                if (!isContentWhitelisted(onScreenTexts, whitelists)) {
                    redirectToHome()
                }
            } else if (BROWSER_PACKAGES.contains(packageName)) {
                val whitelists = db.whitelistDao().getWhitelistItemsByType("BROWSER_DOMAIN").first()
                val onScreenTexts = extractOnScreenTexts(rootInActiveWindow)
                if (!isContentWhitelisted(onScreenTexts, whitelists)) {
                    redirectToHome()
                }
            }
        }
    }

    private fun extractOnScreenTexts(node: AccessibilityNodeInfo?): List<String> {
        val texts = mutableListOf<String>()
        if (node == null) return texts

        fun traverse(currentNode: AccessibilityNodeInfo?) {
            if (currentNode == null) return
            val text = currentNode.text?.toString()
            val desc = currentNode.contentDescription?.toString()
            if (!text.isNullOrBlank()) texts.add(text)
            if (!desc.isNullOrBlank()) texts.add(desc)

            for (i in 0 until currentNode.childCount) {
                traverse(currentNode.getChild(i))
            }
        }

        traverse(node)
        return texts
    }

    private fun redirectToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(homeIntent)
    }

    override fun onInterrupt() {
        // Accessibility interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
