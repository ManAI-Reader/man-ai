package com.highliuk.manai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity

@Database(entities = [MangaEntity::class], version = 1, exportSchema = true)
abstract class ManAiDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
}
