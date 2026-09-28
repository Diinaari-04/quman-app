package com.koor.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.koor.app.data.local.dao.CategoryDao
import com.koor.app.data.local.dao.ProfileDao
import com.koor.app.data.local.dao.SimCardDao
import com.koor.app.data.local.dao.TransactionDao
import com.koor.app.data.local.entities.CategoryEntity
import com.koor.app.data.local.entities.ProfileEntity
import com.koor.app.data.local.entities.SimCardEntity
import com.koor.app.data.local.entities.TransactionEntity

@Database(
    entities = [
        ProfileEntity::class,
        SimCardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KoorDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun simCardDao(): SimCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}
