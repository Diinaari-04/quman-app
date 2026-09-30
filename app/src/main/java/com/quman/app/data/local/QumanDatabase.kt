package com.quman.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quman.app.data.local.dao.AdMessageDao
import com.quman.app.data.local.dao.CategoryDao
import com.quman.app.data.local.dao.ProfileDao
import com.quman.app.data.local.dao.SimCardDao
import com.quman.app.data.local.dao.TransactionDao
import com.quman.app.data.local.entities.AdMessageEntity
import com.quman.app.data.local.entities.CategoryEntity
import com.quman.app.data.local.entities.ProfileEntity
import com.quman.app.data.local.entities.SimCardEntity
import com.quman.app.data.local.entities.TransactionEntity

@Database(
    entities = [
        ProfileEntity::class,
        SimCardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AdMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QumanDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun simCardDao(): SimCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun adMessageDao(): AdMessageDao
}
