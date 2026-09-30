package com.quman.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ad_messages")
data class AdMessageEntity(
    @PrimaryKey
    val id: String,
    val sender: String,
    val body: String,
    val provider: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long = System.currentTimeMillis()
)
