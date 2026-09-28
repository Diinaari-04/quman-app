package com.quman.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quman.app.data.preferences.UserPreferencesRepository
import com.quman.app.data.repository.AuthRepository
import com.quman.app.data.repository.SimCardRepository
import com.quman.app.ui.screens.auth.AuthScreen
import com.quman.app.ui.screens.auth.AuthViewModel
import com.quman.app.ui.screens.home.MainContainerScreen
import com.quman.app.ui.screens.onboarding.OnboardingScreen
import com.quman.app.ui.theme.PrimaryGradient
import com.quman.app.ui.theme.QumanDeepBlue
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object RouteOnboarding

@Serializable
data object RouteAuth

@Serializable
data object RouteMain

@Composable
fun QumanNavHost(
    authRepository: AuthRepository,
    simCardRepository: SimCardRepository,
    userPreferences: UserPreferencesRepository,
    isLoggedIn: Boolean,
    isOnboardingCompleted: Boolean,
    cachedName: String?,
    cachedPhone: String?,
    isSessionDetermined: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val coroutineScope = rememberCoroutineScope()

    if (!isSessionDetermined) {
        // Splash branding while initial state resolves
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGradient),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    color = QumanDeepBlue,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp
                )
            }
        }
        return
    }

    val startDestination: Any = when {
        isLoggedIn -> RouteMain
        isOnboardingCompleted -> RouteAuth
        else -> RouteOnboarding
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<RouteOnboarding> {
            OnboardingScreen(
                simCardRepository = simCardRepository,
                onComplete = {
                    coroutineScope.launch {
                        userPreferences.setOnboardingCompleted(true)
                        navController.navigate(RouteAuth) {
                            popUpTo(RouteOnboarding) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<RouteAuth> {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(authRepository)
            }
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(RouteMain) {
                        popUpTo(RouteAuth) { inclusive = true }
                    }
                }
            )
        }

        composable<RouteMain> {
            MainContainerScreen(
                userName = cachedName,
                userPhone = cachedPhone,
                onLogout = {
                    coroutineScope.launch {
                        authRepository.logout()
                        navController.navigate(RouteAuth) {
                            popUpTo(RouteMain) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
