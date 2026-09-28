package com.quman.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.quman.app.data.local.dao.SimCardDao
import com.quman.app.data.local.entities.SimCardEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimCardDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("slot_index")
    val slotIndex: Int,
    val label: String,
    val carrier: String,
    @SerialName("phone_number")
    val phoneNumber: String,
    @SerialName("last_balance")
    val lastBalance: Double? = null,
    @SerialName("balance_at")
    val balanceAt: Long? = null
)

class SimCardRepository(
    private val context: Context,
    private val simCardDao: SimCardDao,
    private val supabaseClient: SupabaseClient
) {
    /**
     * Silently detects active SIM cards, maps carriers heuristically,
     * persists to local Room database and syncs to Supabase sim_cards table.
     */
    suspend fun detectAndPersistActiveSims(): List<SimCardEntity> = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return@withContext emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val activeSubscriptions = try {
            subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }

        if (activeSubscriptions.isEmpty()) {
            return@withContext emptyList()
        }

        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: "local_user"
        val entities = mutableListOf<SimCardEntity>()

        for (sub in activeSubscriptions) {
            val slot = sub.simSlotIndex
            val rawCarrier = sub.carrierName?.toString()?.trim() ?: "SIM ${slot + 1}"
            val lower = rawCarrier.lowercase()
            val mappedCarrier = when {
                lower.contains("hormuud") -> "hormuud"
                lower.contains("somnet") -> "somnet"
                else -> rawCarrier
            }
            val number = try {
                sub.number ?: ""
            } catch (e: Exception) {
                ""
            }
            val simId = "sim_${slot}_${sub.subscriptionId}"
            val entity = SimCardEntity(
                id = simId,
                userId = currentUserId,
                slotIndex = slot,
                label = rawCarrier,
                carrier = mappedCarrier,
                phoneNumber = number,
                lastBalance = null,
                balanceAt = null,
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            simCardDao.insertOrUpdate(entity)
            entities.add(entity)
        }

        // Attempt silent sync to Supabase if user is authenticated
        if (currentUserId != "local_user") {
            try {
                for (entity in entities) {
                    val dto = SimCardDto(
                        id = entity.id,
                        userId = entity.userId,
                        slotIndex = entity.slotIndex,
                        label = entity.label,
                        carrier = entity.carrier,
                        phoneNumber = entity.phoneNumber,
                        lastBalance = entity.lastBalance,
                        balanceAt = entity.balanceAt
                    )
                    supabaseClient.postgrest["sim_cards"].upsert(dto)
                    simCardDao.insertOrUpdate(entity.copy(synced = true))
                }
            } catch (_: Exception) {
                // Keep local row with synced = false for later sync
            }
        }

        entities
    }
}
