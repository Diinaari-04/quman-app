package com.quman.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sim_cards")
data class SimCardEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "slot_index")
    val slotIndex: Int,
    val label: String,
    val carrier: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "last_balance")
    val lastBalance: Double? = null,
    @ColumnInfo(name = "balance_at")
    val balanceAt: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = true
)
