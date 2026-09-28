package com.koor.app

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
import com.koor.app.ui.navigation.KoorNavHost
import com.koor.app.ui.theme.KoorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KoorApplication
        val authRepository = app.authRepository
        val userPreferences = app.userPreferences

        setContent {
            KoorTheme {
                val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
                val isOnboardingCompleted by userPreferences.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
                val cachedName by userPreferences.cachedFullName.collectAsStateWithLifecycle(initialValue = null)
                val cachedPhone by userPreferences.cachedPhone.collectAsStateWithLifecycle(initialValue = null)

                // Track if we have performed initial session determination
                var sessionDetermined by remember { mutableStateOf(false) }

                // Once auth status or preferences emit, mark determined
                val sessionStatus by authRepository.sessionStatus.collectAsStateWithLifecycle(
                    initialValue = null
                )

                if (sessionStatus != null) {
                    sessionDetermined = true
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    KoorNavHost(
                        authRepository = authRepository,
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
