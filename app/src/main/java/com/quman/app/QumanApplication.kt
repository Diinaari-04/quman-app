package com.quman.app

import android.app.Application
import androidx.room.Room
import com.quman.app.data.local.QumanDatabase
import com.quman.app.data.preferences.UserPreferencesRepository
import com.quman.app.data.remote.SupabaseClientProvider
import com.quman.app.data.repository.AuthRepository
import com.quman.app.data.repository.SimCardRepository

class QumanApplication : Application() {

    val database: QumanDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            QumanDatabase::class.java,
            "quman_local.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
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

    val simCardRepository: SimCardRepository by lazy {
        SimCardRepository(
            context = applicationContext,
            simCardDao = database.simCardDao(),
            supabaseClient = SupabaseClientProvider.client
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.quman.app.util.NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: QumanApplication
            private set
    }
}
