package com.highliuk.manai.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ManAiDatabaseTest {

    @Test
    fun downgradeMigration3to2_hasCorrectVersions() {
        val migration = ManAiDatabase.MIGRATION_3_2
        assertEquals(3, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun downgradeMigration2to1_hasCorrectVersions() {
        val migration = ManAiDatabase.MIGRATION_2_1
        assertEquals(2, migration.startVersion)
        assertEquals(1, migration.endVersion)
    }
}
