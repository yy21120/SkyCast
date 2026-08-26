package com.yy21120.skycast.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunityCacheDaoTest {
    private lateinit var database: SkyCastDatabase
    private lateinit var dao: OpportunityCacheDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SkyCastDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.opportunityCacheDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertReadAndDeleteSnapshot() = runTest {
        val initial = OpportunityCacheEntity(
            cacheKey = "wuhan:sunset:3",
            payloadJson = "{\"version\":1}",
            cachedAtEpochMillis = 1_000L,
        )
        dao.upsert(initial)

        assertEquals(initial, dao.read(initial.cacheKey))

        val updated = initial.copy(
            payloadJson = "{\"version\":2}",
            cachedAtEpochMillis = 2_000L,
        )
        dao.upsert(updated)
        assertEquals(updated, dao.read(initial.cacheKey))

        dao.delete(initial.cacheKey)
        assertNull(dao.read(initial.cacheKey))
    }
}
