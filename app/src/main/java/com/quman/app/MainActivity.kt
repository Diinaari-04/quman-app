package com.quman.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quman.app.ui.navigation.QumanNavHost
import com.quman.app.ui.theme.QumanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QumanApplication
        val authRepository = app.authRepository
        val simCardRepository = app.simCardRepository
        val userPreferences = app.userPreferences

        setContent {
            QumanTheme {
                val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
                val isOnboardingCompleted by userPreferences.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
                val cachedName by userPreferences.cachedFullName.collectAsStateWithLifecycle(initialValue = null)
                val cachedPhone by userPreferences.cachedPhone.collectAsStateWithLifecycle(initialValue = null)

                var sessionDetermined by remember { mutableStateOf(false) }

                val sessionStatus by authRepository.sessionStatus.collectAsStateWithLifecycle(
                    initialValue = null
                )

                if (sessionStatus != null) {
                    sessionDetermined = true
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    QumanNavHost(
                        authRepository = authRepository,
                        simCardRepository = simCardRepository,
                        userPreferences = userPreferences,
                        isLoggedIn = isLoggedIn,
                        isOnboardingCompleted = isOnboardingCompleted,
                        cachedName = cachedName,
                        cachedPhone = cachedPhone,
                        isSessionDetermined = sessionDetermined || sessionStatus != null
                    )
                }
            }
        }
    }
}
