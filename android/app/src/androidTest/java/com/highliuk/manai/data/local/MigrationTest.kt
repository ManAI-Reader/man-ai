package com.highliuk.manai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ManAiDatabase::class.java
    )

    @Test
    fun migration2to3_addsContentHashColumn() {
        helper.createDatabase("test-db", 2).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage) VALUES ('content://test', 'Test', 10, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("test-db", 3, true, ManAiDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT contentHash FROM manga")
        cursor.moveToFirst()
        assertEquals("", cursor.getString(0))
        cursor.close()
        db.close()
    }
}
