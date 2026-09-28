package com.koor.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "sim_id")
    val simId: String? = null,
    val provider: String, // 'EVC Plus' | 'ZAAD' | 'Sahal' | 'eDahab'
    val sender: String? = null,
    val direction: String, // 'out' | 'in'
    val amount: Double,
    @ColumnInfo(name = "counterparty_name")
    val counterpartyName: String? = null,
    @ColumnInfo(name = "counterparty_phone")
    val counterpartyPhone: String? = null,
    @ColumnInfo(name = "balance_after")
    val balanceAfter: Double? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "sms_hash")
    val smsHash: String? = null,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = true
)
