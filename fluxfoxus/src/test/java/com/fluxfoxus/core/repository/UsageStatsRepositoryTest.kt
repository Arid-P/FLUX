package com.fluxfoxus.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.FluxFoxusDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UsageStatsRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: FluxFoxusDatabase
    private lateinit var repository: UsageStatsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UsageStatsRepository(context, database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDefaultState() = runBlocking {
        val state = repository.dailyUsageState.first()
        assertEquals(LocalDate.now().toString(), state.dateIso)
        assertTrue(state.packageUsageMap.isEmpty())
        assertEquals(0L, state.totalForegroundMs)
    }

    @Test
    fun testRefreshUsageStatsHandlesNullManagerGracefully() = runBlocking {
        // Without system UsageStatsManager service, it returns failure safely without crash
        val result = repository.refreshUsageStats()
        assertTrue(result.isFailure)
    }
}
