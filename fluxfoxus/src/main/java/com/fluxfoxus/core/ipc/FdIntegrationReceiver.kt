package com.fluxfoxus.core.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Inter-App IPC Gateway Receiver
 *
 * Implements IPC_SPEC_FD_FF_NATIVE.md:
 * Ingests scheduled time-blocks broadcast by FluxDone (`com.flux.action.FOCUS_BLOCK_SYNC`).
 * Protected by signature-level permission `com.fluxfoxus.permission.FOCUS_IPC`.
 */
class FdIntegrationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FOCUS_BLOCK_SYNC = "com.flux.action.FOCUS_BLOCK_SYNC"

        const val EXTRA_VERSION = "EXTRA_VERSION"
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_END_TIME = "EXTRA_END_TIME"
        const val EXTRA_DURATION_MIN = "EXTRA_DURATION_MIN"
        const val EXTRA_LIST_ID = "EXTRA_LIST_ID"
        const val EXTRA_COLOR_HEX = "EXTRA_COLOR_HEX"
        const val EXTRA_PRESET_ID = "EXTRA_PRESET_ID"
        const val EXTRA_PAYLOAD_JSON = "EXTRA_PAYLOAD_JSON"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION_FOCUS_BLOCK_SYNC) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                handleSyncBroadcast(context, intent)
            } catch (_: Exception) {
                // Securely catch any parsing/persistence errors
            } finally {
                pendingResult.finish()
            }
        }
    }

    suspend fun handleSyncBroadcast(context: Context, intent: Intent) {
        var action: String? = intent.getStringExtra(EXTRA_ACTION)
        var taskId: String? = intent.getStringExtra(EXTRA_TASK_ID)
        var taskName: String = intent.getStringExtra(EXTRA_TASK_NAME) ?: "FluxDone Task"
        var startTime: Long = intent.getLongExtra(EXTRA_START_TIME, 0L)
        var endTime: Long = intent.getLongExtra(EXTRA_END_TIME, 0L)
        var durationMinutes: Int = intent.getIntExtra(EXTRA_DURATION_MIN, 0)
        var presetId: String? = intent.getStringExtra(EXTRA_PRESET_ID)

        // Parse JSON payload fallback if provided
        val jsonString = intent.getStringExtra(EXTRA_PAYLOAD_JSON)
        if (!jsonString.isNullOrEmpty()) {
            try {
                val json = JSONObject(jsonString)
                if (action == null && json.has("action")) action = json.getString("action")
                if (taskId == null && json.has("taskId")) taskId = json.getString("taskId")
                if (json.has("taskName")) taskName = json.getString("taskName")
                if (startTime == 0L && json.has("startTimeEpochMs")) startTime = json.getLong("startTimeEpochMs")
                if (endTime == 0L && json.has("endTimeEpochMs")) endTime = json.getLong("endTimeEpochMs")
                if (durationMinutes == 0 && json.has("durationMinutes")) durationMinutes = json.getInt("durationMinutes")
                if (presetId == null && json.has("presetId") && !json.isNull("presetId")) presetId = json.getString("presetId")
            } catch (_: Exception) {
                // Ignore JSON parse errors and continue with extras
            }
        }

        if (action.isNullOrEmpty() || taskId.isNullOrEmpty()) return

        val db = FluxFoxusDatabase.getDatabase(context)
        val sessionDao = db.focusSessionDao()
        val presetDao = db.presetDao()

        when (action.uppercase()) {
            "CREATE" -> {
                // Verify or fallback preset ID ensuring foreign key exists
                val resolvedPresetId = if (presetId != null) {
                    if (presetDao.getPresetById(presetId) == null) {
                        val fallback = PresetEntity(
                            id = presetId,
                            name = taskName,
                            emoji = "⏳",
                            breakCount = 2,
                            breakDurationMinutes = 5
                        )
                        presetDao.insertPreset(fallback)
                    }
                    presetId
                } else {
                    val allPresets = presetDao.getAllPresets().first()
                    if (allPresets.isNotEmpty()) {
                        allPresets.first().id
                    } else {
                        val defaultPreset = PresetEntity(
                            id = "default_focus",
                            name = "Deep Work",
                            emoji = "🧠",
                            breakCount = 2,
                            breakDurationMinutes = 5
                        )
                        presetDao.insertPreset(defaultPreset)
                        defaultPreset.id
                    }
                }

                if (durationMinutes <= 0 && endTime > startTime) {
                    durationMinutes = ((endTime - startTime) / (60 * 1000)).toInt()
                }

                val newSession = FocusSessionEntity(
                    id = UUID.randomUUID().toString(),
                    sessionOrigin = "TASK_SYNC",
                    presetId = resolvedPresetId,
                    sessionTitle = taskName,
                    linkedTaskId = taskId,
                    timerMode = "COUNTDOWN",
                    startTime = startTime,
                    endTime = endTime,
                    targetDurationMinutes = durationMinutes.coerceAtLeast(1),
                    status = "PLANNED"
                )
                sessionDao.insertSession(newSession)
            }

            "UPDATE" -> {
                val allSessions = sessionDao.getAllSessions().first()
                val linkedSession = allSessions.find { it.linkedTaskId == taskId }
                if (linkedSession != null) {
                    val updated = linkedSession.copy(
                        sessionTitle = taskName,
                        startTime = if (startTime > 0L) startTime else linkedSession.startTime,
                        endTime = if (endTime > 0L) endTime else linkedSession.endTime,
                        targetDurationMinutes = if (durationMinutes > 0) durationMinutes else linkedSession.targetDurationMinutes,
                        presetId = presetId ?: linkedSession.presetId,
                        updatedAt = System.currentTimeMillis()
                    )
                    sessionDao.updateSession(updated)
                }
            }

            "DELETE" -> {
                val allSessions = sessionDao.getAllSessions().first()
                val linkedSession = allSessions.find { it.linkedTaskId == taskId }
                if (linkedSession != null) {
                    if (linkedSession.status == "PLANNED") {
                        sessionDao.deleteSession(linkedSession)
                    } else if (linkedSession.status == "ACTIVE" || linkedSession.status == "PAUSED_BREAK") {
                        sessionDao.updateSession(
                            linkedSession.copy(
                                status = "QUIT_PREMATURELY",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}
