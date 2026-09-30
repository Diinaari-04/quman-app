package com.quman.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.qumanDataStore: DataStore<Preferences> by preferencesDataStore(name = "quman_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CACHED_USER_ID = stringPreferencesKey("cached_user_id")
        val CACHED_FULL_NAME = stringPreferencesKey("cached_full_name")
        val CACHED_PHONE = stringPreferencesKey("cached_phone")
        val BYPASS_SILENT_MODE = booleanPreferencesKey("bypass_silent_mode")
    }

    val isBypassSilentMode: Flow<Boolean> = context.qumanDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.BYPASS_SILENT_MODE] ?: false
        }

    suspend fun setBypassSilentMode(enabled: Boolean) {
        context.qumanDataStore.edit { preferences ->
            preferences[PreferencesKeys.BYPASS_SILENT_MODE] = enabled
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.qumanDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    val cachedFullName: Flow<String?> = context.qumanDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CACHED_FULL_NAME]
        }

    val cachedPhone: Flow<String?> = context.qumanDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CACHED_PHONE]
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.qumanDataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveCachedUserData(userId: String, fullName: String, phone: String) {
        context.qumanDataStore.edit { preferences ->
            preferences[PreferencesKeys.CACHED_USER_ID] = userId
            preferences[PreferencesKeys.CACHED_FULL_NAME] = fullName
            preferences[PreferencesKeys.CACHED_PHONE] = phone
        }
    }

    suspend fun clearSession() {
        context.qumanDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CACHED_USER_ID)
            preferences.remove(PreferencesKeys.CACHED_FULL_NAME)
            preferences.remove(PreferencesKeys.CACHED_PHONE)
        }
    }
}
