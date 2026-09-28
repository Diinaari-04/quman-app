package com.quman.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quman.app.data.local.entities.CategoryEntity
import com.quman.app.data.local.entities.ProfileEntity
import com.quman.app.data.local.entities.SimCardEntity
import com.quman.app.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfile(id: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun clear()
}

@Dao
interface SimCardDao {
    @Query("SELECT * FROM sim_cards ORDER BY slot_index ASC")
    fun getAllSimCards(): Flow<List<SimCardEntity>>

    @Query("SELECT * FROM sim_cards WHERE user_id = :userId ORDER BY slot_index ASC")
    fun getSimCards(userId: String): Flow<List<SimCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(simCard: SimCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(simCards: List<SimCardEntity>)

    @Query("DELETE FROM sim_cards WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE user_id = :userId OR is_default = 1 ORDER BY sort_order ASC")
    fun getCategories(userId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY occurred_at DESC")
    fun getTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND direction = :direction ORDER BY occurred_at DESC")
    fun getTransactionsByDirection(userId: String, direction: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnsynced(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
