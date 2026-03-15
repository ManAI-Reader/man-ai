package com.highliuk.manai.di

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.highliuk.manai.data.local.ManAiDatabase
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatabaseModuleTest {

    private lateinit var context: Context
    private lateinit var database: ManAiDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = DatabaseModule.provideDatabase(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun provideDatabase_buildsWithAllMigrations() {
        // Force the database open by running a query
        database.openHelper.writableDatabase
        assertTrue(database.isOpen)
    }

    @Test
    fun provideMangaDao_returnsDaoFromDatabase() {
        val dao = DatabaseModule.provideMangaDao(database)
        assertNotNull(dao)
    }

    @Test
    fun providePageOcrResultDao_returnsDaoFromDatabase() {
        val dao = DatabaseModule.providePageOcrResultDao(database)
        assertNotNull(dao)
    }

    @Test
    fun provideContentResolver_returnsNonNull() {
        val resolver = DatabaseModule.provideContentResolver(context)
        assertNotNull(resolver)
    }
}
