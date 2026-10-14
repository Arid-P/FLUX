package com.fluxfoxus.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fluxfoxus.MainActivity
import com.fluxfoxus.core.database.FluxFoxusDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Focus Timer Foreground Service
 *
 * Implements persistent countdown/stopwatch ticker with wake-lock,
 * ongoing Android notification, and reactive StateFlow<TimerState>.
 */
class FocusTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.fluxfoxus.action.START_TIMER"
        const val ACTION_PAUSE = "com.fluxfoxus.action.PAUSE_TIMER"
        const val ACTION_RESUME = "com.fluxfoxus.action.RESUME_TIMER"
        const val ACTION_TAKE_BREAK = "com.fluxfoxus.action.TAKE_BREAK"
        const val ACTION_END_BREAK = "com.fluxfoxus.action.END_BREAK"
        const val ACTION_STOP = "com.fluxfoxus.action.STOP_TIMER"

        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_PRESET_ID = "EXTRA_PRESET_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        const val EXTRA_BREAKS_COUNT = "EXTRA_BREAKS_COUNT"
        const val EXTRA_BREAK_DURATION_MINUTES = "EXTRA_BREAK_DURATION_MINUTES"
        const val EXTRA_TIMER_MODE = "EXTRA_TIMER_MODE"

        private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        fun setTimerStateForTesting(state: TimerState) {
            _timerState.value = state
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Session State Tracking
    private var currentSessionId: String = ""
    private var currentPresetId: String = ""
    private var currentTitle: String = "Focus Session"
    private var currentTimerMode: String = "COUNTDOWN"
    private var totalSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var elapsedSeconds: Int = 0
    private var breaksRemaining: Int = 2
    private var totalBreaks: Int = 2
    private var breakDurationSeconds: Int = 300
    private var isBreak: Boolean = false
    private var breakRemainingSeconds: Int = 0
    private var pauseRemainingSeconds: Int = 300 // Max 5 minutes emergency pause

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: System.currentTimeMillis().toString()
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID) ?: "default"
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Focus Session"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                val breaksCount = intent.getIntExtra(EXTRA_BREAKS_COUNT, 2)
                val breakMinutes = intent.getIntExtra(EXTRA_BREAK_DURATION_MINUTES, 5)
                val mode = intent.getStringExtra(EXTRA_TIMER_MODE) ?: "COUNTDOWN"

                startSession(sessionId, presetId, title, durationMinutes, breaksCount, breakMinutes, mode)
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_TAKE_BREAK -> startBreak()
            ACTION_END_BREAK -> endBreakEarly()
            ACTION_STOP -> stopSession(completed = false)
        }
        return START_STICKY
    }

    private fun startSession(
        sessionId: String,
        presetId: String,
        title: String,
        durationMinutes: Int,
        breaksCount: Int,
        breakMinutes: Int,
        mode: String
    ) {
        currentSessionId = sessionId
        currentPresetId = presetId
        currentTitle = title
        currentTimerMode = mode
        totalSeconds = durationMinutes * 60
        remainingSeconds = totalSeconds
        elapsedSeconds = 0
        breaksRemaining = breaksCount
        totalBreaks = breaksCount
        breakDurationSeconds = breakMinutes * 60
        isBreak = false
        pauseRemainingSeconds = 300

        val notification = buildNotification(formatRemaining(remainingSeconds), "Focusing: $title")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                tick()
            }
        }
    }

    private suspend fun tick() {
        if (isBreak) {
            if (breakRemainingSeconds > 0) {
                breakRemainingSeconds -= 1
                emitRunningState()
                updateNotification(formatRemaining(breakRemainingSeconds), "Break: $currentTitle")
            } else {
                // Break expired, auto-resume focus
                isBreak = false
                emitRunningState()
                updateNotification(formatRemaining(remainingSeconds), "Resuming: $currentTitle")
            }
        } else {
            if (currentTimerMode == "COUNTDOWN") {
                if (remainingSeconds > 0) {
                    remainingSeconds -= 1
                    elapsedSeconds += 1
                    emitRunningState()
                    updateNotification(formatRemaining(remainingSeconds), "Focusing: $currentTitle")
                } else {
                    // Session countdown reached 0:00 -> Complete!
                    stopSession(completed = true)
                }
            } else {
                // STOPWATCH mode: counts up
                elapsedSeconds += 1
                remainingSeconds = elapsedSeconds
                emitRunningState()
                updateNotification(formatRemaining(elapsedSeconds), "Elapsed: $currentTitle")
            }
        }
    }

    private fun emitRunningState() {
        _timerState.value = TimerState.Running(
            sessionId = currentSessionId,
            presetId = currentPresetId,
            sessionTitle = currentTitle,
            remainingSeconds = if (isBreak) breakRemainingSeconds else remainingSeconds,
            totalSeconds = if (isBreak) breakDurationSeconds else totalSeconds,
            elapsedSeconds = elapsedSeconds,
            isBreak = isBreak,
            breaksRemaining = breaksRemaining,
            totalBreaks = totalBreaks,
            timerMode = currentTimerMode
        )
    }

    private fun startBreak() {
        if (breaksRemaining > 0 && !isBreak) {
            breaksRemaining -= 1
            isBreak = true
            breakRemainingSeconds = breakDurationSeconds
            emitRunningState()
            updateNotification(formatRemaining(breakRemainingSeconds), "Break in Progress")
        }
    }

    private fun endBreakEarly() {
        if (isBreak) {
            isBreak = false
            emitRunningState()
            updateNotification(formatRemaining(remainingSeconds), "Focusing: $currentTitle")
        }
    }

    private fun pauseSession() {
        tickerJob?.cancel()
        _timerState.value = TimerState.Paused(
            sessionId = currentSessionId,
            sessionTitle = currentTitle,
            remainingSeconds = remainingSeconds,
            pauseSecondsRemaining = pauseRemainingSeconds
        )
        updateNotification("PAUSED (${formatRemaining(pauseRemainingSeconds)})", currentTitle)

        // Emergency pause ticker
        tickerJob = serviceScope.launch {
            while (pauseRemainingSeconds > 0) {
                delay(1000L)
                pauseRemainingSeconds -= 1
                _timerState.value = TimerState.Paused(
                    sessionId = currentSessionId,
                    sessionTitle = currentTitle,
                    remainingSeconds = remainingSeconds,
                    pauseSecondsRemaining = pauseRemainingSeconds
                )
                updateNotification("PAUSED (${formatRemaining(pauseRemainingSeconds)})", currentTitle)
            }
            // 5-minute pause timeout -> Quit prematurely
            stopSession(completed = false)
        }
    }

    private fun resumeSession() {
        pauseRemainingSeconds = 300
        startTicker()
    }

    private fun stopSession(completed: Boolean) {
        tickerJob?.cancel()

        serviceScope.launch(Dispatchers.IO) {
            val db = FluxFoxusDatabase.getDatabase(applicationContext)
            val session = db.focusSessionDao().getSessionById(currentSessionId)
            if (session != null) {
                db.focusSessionDao().updateSession(
                    session.copy(
                        status = if (completed) "COMPLETED" else "QUIT_PREMATURELY",
                        actualDurationSeconds = elapsedSeconds,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        _timerState.value = if (completed) {
            TimerState.Completed(
                sessionId = currentSessionId,
                sessionTitle = currentTitle,
                totalDurationSeconds = elapsedSeconds
            )
        } else {
            TimerState.Idle
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatRemaining(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer countdown notification for FluxFoxus"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(timeFormatted: String, subtitle: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(timeFormatted)
            .setContentText(subtitle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(timeFormatted: String, subtitle: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(timeFormatted, subtitle))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FluxFoxus:TimerWakeLock")
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // Safety cap 4 hours
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceJob.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Observable Timer State hierarchy for Compose UI and background services.
 */
sealed interface TimerState {
    data object Idle : TimerState

    data class Running(
        val sessionId: String,
        val presetId: String,
        val sessionTitle: String,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val elapsedSeconds: Int,
        val isBreak: Boolean,
        val breaksRemaining: Int,
        val totalBreaks: Int,
        val timerMode: String
    ) : TimerState

    data class Paused(
        val sessionId: String,
        val sessionTitle: String,
        val remainingSeconds: Int,
        val pauseSecondsRemaining: Int
    ) : TimerState

    data class Completed(
        val sessionId: String,
        val sessionTitle: String,
        val totalDurationSeconds: Int
    ) : TimerState
}
