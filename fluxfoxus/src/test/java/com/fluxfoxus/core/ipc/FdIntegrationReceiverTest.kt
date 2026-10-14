package com.fluxfoxus.core.ipc

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FdIntegrationReceiverTest {

    private lateinit var context: Context
    private lateinit var db: FluxFoxusDatabase
    private lateinit var receiver: FdIntegrationReceiver

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Seed a default preset
        db.presetDao().insertPreset(
            PresetEntity(
                id = "preset_olympiad",
                name = "Olympiad Math",
                emoji = "📐"
            )
        )

        receiver = FdIntegrationReceiver()
        FluxFoxusDatabase.setDatabaseForTesting(db)
    }

    @After
    fun tearDown() {
        FluxFoxusDatabase.setDatabaseForTesting(null)
        db.close()
    }

    @Test
    fun testCreateFocusBlockBroadcast() = runBlocking {
        val intent = Intent(FdIntegrationReceiver.ACTION_FOCUS_BLOCK_SYNC).apply {
            putExtra(FdIntegrationReceiver.EXTRA_ACTION, "CREATE")
            putExtra(FdIntegrationReceiver.EXTRA_TASK_ID, "task_geom_001")
            putExtra(FdIntegrationReceiver.EXTRA_TASK_NAME, "Geometry Problem Set 2")
            putExtra(FdIntegrationReceiver.EXTRA_START_TIME, 1773327600000L)
            putExtra(FdIntegrationReceiver.EXTRA_END_TIME, 1773334800000L)
            putExtra(FdIntegrationReceiver.EXTRA_DURATION_MIN, 120)
            putExtra(FdIntegrationReceiver.EXTRA_PRESET_ID, "preset_olympiad")
        }

        // Test handler
        receiver.handleSyncBroadcast(context, intent)

        val sessions = FluxFoxusDatabase.getDatabase(context).focusSessionDao().getAllSessions().first()
        val synced = sessions.find { it.linkedTaskId == "task_geom_001" }

        assertNotNull(synced)
        assertEquals("Geometry Problem Set 2", synced?.sessionTitle)
        assertEquals(120, synced?.targetDurationMinutes)
        assertEquals("PLANNED", synced?.status)
        assertEquals("TASK_SYNC", synced?.sessionOrigin)
        assertEquals("preset_olympiad", synced?.presetId)
    }

    @Test
    fun testUpdateFocusBlockBroadcast() = runBlocking {
        val sessionDao = FluxFoxusDatabase.getDatabase(context).focusSessionDao()
        val initialSession = FocusSessionEntity(
            id = "sess_update_test",
            sessionOrigin = "TASK_SYNC",
            presetId = "preset_olympiad",
            sessionTitle = "Algebra Practice",
            linkedTaskId = "task_alg_002",
            timerMode = "COUNTDOWN",
            startTime = 1000L,
            endTime = 2000L,
            targetDurationMinutes = 60,
            status = "PLANNED"
        )
        sessionDao.insertSession(initialSession)

        val updateIntent = Intent(FdIntegrationReceiver.ACTION_FOCUS_BLOCK_SYNC).apply {
            putExtra(FdIntegrationReceiver.EXTRA_ACTION, "UPDATE")
            putExtra(FdIntegrationReceiver.EXTRA_TASK_ID, "task_alg_002")
            putExtra(FdIntegrationReceiver.EXTRA_TASK_NAME, "Advanced Algebra Olympiad")
            putExtra(FdIntegrationReceiver.EXTRA_START_TIME, 3000L)
            putExtra(FdIntegrationReceiver.EXTRA_END_TIME, 5000L)
            putExtra(FdIntegrationReceiver.EXTRA_DURATION_MIN, 90)
        }

        receiver.handleSyncBroadcast(context, updateIntent)

        val sessions = sessionDao.getAllSessions().first()
        val updated = sessions.find { it.linkedTaskId == "task_alg_002" }

        assertNotNull(updated)
        assertEquals("Advanced Algebra Olympiad", updated?.sessionTitle)
        assertEquals(90, updated?.targetDurationMinutes)
        assertEquals(3000L, updated?.startTime)
    }

    @Test
    fun testDeleteFocusBlockBroadcast() = runBlocking {
        val sessionDao = FluxFoxusDatabase.getDatabase(context).focusSessionDao()
        val initialSession = FocusSessionEntity(
            id = "sess_delete_test",
            sessionOrigin = "TASK_SYNC",
            presetId = "preset_olympiad",
            sessionTitle = "Trashable Task",
            linkedTaskId = "task_trash_003",
            timerMode = "COUNTDOWN",
            startTime = 1000L,
            targetDurationMinutes = 30,
            status = "PLANNED"
        )
        sessionDao.insertSession(initialSession)

        val deleteIntent = Intent(FdIntegrationReceiver.ACTION_FOCUS_BLOCK_SYNC).apply {
            putExtra(FdIntegrationReceiver.EXTRA_ACTION, "DELETE")
            putExtra(FdIntegrationReceiver.EXTRA_TASK_ID, "task_trash_003")
        }

        receiver.handleSyncBroadcast(context, deleteIntent)

        val sessions = sessionDao.getAllSessions().first()
        val deleted = sessions.find { it.linkedTaskId == "task_trash_003" }
        assertNull(deleted)
    }

    @Test
    fun testJsonPayloadFallbackBroadcast() = runBlocking {
        val jsonPayload = """
            {
                "version": 1,
                "action": "CREATE",
                "taskId": "task_json_999",
                "taskName": "JSON Deserialized Task",
                "startTimeEpochMs": 1773327600000,
                "endTimeEpochMs": 1773334800000,
                "durationMinutes": 45,
                "presetId": "preset_olympiad"
            }
        """.trimIndent()

        val intent = Intent(FdIntegrationReceiver.ACTION_FOCUS_BLOCK_SYNC).apply {
            putExtra(FdIntegrationReceiver.EXTRA_PAYLOAD_JSON, jsonPayload)
        }

        receiver.handleSyncBroadcast(context, intent)

        val sessions = FluxFoxusDatabase.getDatabase(context).focusSessionDao().getAllSessions().first()
        val synced = sessions.find { it.linkedTaskId == "task_json_999" }

        assertNotNull(synced)
        assertEquals("JSON Deserialized Task", synced?.sessionTitle)
        assertEquals(45, synced?.targetDurationMinutes)
    }
}
