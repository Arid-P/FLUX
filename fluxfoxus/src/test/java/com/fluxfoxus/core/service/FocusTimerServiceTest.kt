package com.fluxfoxus.core.service

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FocusTimerServiceTest {

    @Test
    fun testTimerStateFlowEmissions() = runBlocking {
        // Initial state
        FocusTimerService.setTimerStateForTesting(TimerState.Idle)
        val initial = FocusTimerService.timerState.first()
        assertTrue(initial is TimerState.Idle)

        // Running state
        val runningState = TimerState.Running(
            sessionId = "sess_101",
            presetId = "preset_deep_work",
            sessionTitle = "Olympiad Math Deep Work",
            remainingSeconds = 1500,
            totalSeconds = 1500,
            elapsedSeconds = 0,
            isBreak = false,
            breaksRemaining = 2,
            totalBreaks = 2,
            timerMode = "COUNTDOWN"
        )
        FocusTimerService.setTimerStateForTesting(runningState)

        val running = FocusTimerService.timerState.first()
        assertTrue(running is TimerState.Running)
        val r = running as TimerState.Running
        assertEquals("sess_101", r.sessionId)
        assertEquals(1500, r.remainingSeconds)
        assertFalse(r.isBreak)
        assertEquals(2, r.breaksRemaining)

        // Paused state
        val pausedState = TimerState.Paused(
            sessionId = "sess_101",
            sessionTitle = "Olympiad Math Deep Work",
            remainingSeconds = 1200,
            pauseSecondsRemaining = 280
        )
        FocusTimerService.setTimerStateForTesting(pausedState)

        val paused = FocusTimerService.timerState.first()
        assertTrue(paused is TimerState.Paused)
        val p = paused as TimerState.Paused
        assertEquals(1200, p.remainingSeconds)
        assertEquals(280, p.pauseSecondsRemaining)

        // Completed state
        val completedState = TimerState.Completed(
            sessionId = "sess_101",
            sessionTitle = "Olympiad Math Deep Work",
            totalDurationSeconds = 1500
        )
        FocusTimerService.setTimerStateForTesting(completedState)

        val completed = FocusTimerService.timerState.first()
        assertTrue(completed is TimerState.Completed)
        val c = completed as TimerState.Completed
        assertEquals(1500, c.totalDurationSeconds)
    }
}
