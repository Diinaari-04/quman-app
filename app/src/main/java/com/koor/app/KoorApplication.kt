package com.koor.app

import android.app.Application
import androidx.room.Room
import com.koor.app.data.local.KoorDatabase
import com.koor.app.data.preferences.UserPreferencesRepository
import com.koor.app.data.remote.SupabaseClientProvider
import com.koor.app.data.repository.AuthRepository

class KoorApplication : Application() {

    val database: KoorDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KoorDatabase::class.java,
            "koor_local.db"
        ).fallbackToDestructiveMigration().build()
    }

    val userPreferences: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            supabaseClient = SupabaseClientProvider.client,
            userPreferences = userPreferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KoorApplication
            private set
    }
}
