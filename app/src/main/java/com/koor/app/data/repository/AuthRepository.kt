package com.koor.app.data.repository

import com.koor.app.data.preferences.UserPreferencesRepository
import com.koor.app.util.PhoneUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface AuthResult {
    data class Success(val user: UserInfo?) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AuthRepository(
    private val supabaseClient: SupabaseClient,
    private val userPreferences: UserPreferencesRepository
) {
    val sessionStatus: Flow<SessionStatus> = supabaseClient.auth.sessionStatus

    val isLoggedIn: Flow<Boolean> = sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    fun getCurrentUser(): UserInfo? {
        return supabaseClient.auth.currentUserOrNull()
    }

    suspend fun signUp(fullName: String, rawPhone: String, password: String): AuthResult {
        val trimmedName = fullName.trim()
        if (trimmedName.isEmpty()) {
            return AuthResult.Error("Fadlan magacaaga oo buuxa geli.")
        }
        val normalizedPhone = PhoneUtils.normalizeSomaliPhone(rawPhone)
            ?: return AuthResult.Error("Fadlan geli lambar sax ah oo Soomaali ah (9 lambar sida 61xxxxxxx ama 12 lambar sida 25261xxxxxxx).")

        if (password.length < 6) {
            return AuthResult.Error("Erayga sirta ah waa inuu ugu yaraan ka koobnaadaa 6 xaraf.")
        }

        val internalEmail = PhoneUtils.toInternalEmail(normalizedPhone)

        return try {
            supabaseClient.auth.signUpWith(Email) {
                email = internalEmail
                this.password = password
                data = buildJsonObject {
                    put("full_name", trimmedName)
                    put("phone", normalizedPhone)
                }
            }

            val user = supabaseClient.auth.currentUserOrNull()
            val userId = user?.id ?: ""
            userPreferences.saveCachedUserData(
                userId = userId,
                fullName = trimmedName,
                phone = normalizedPhone
            )
            userPreferences.setOnboardingCompleted(true)
            AuthResult.Success(user)
        } catch (e: Exception) {
            mapAuthException(e)
        }
    }

    suspend fun login(rawPhone: String, password: String): AuthResult {
        val normalizedPhone = PhoneUtils.normalizeSomaliPhone(rawPhone)
            ?: return AuthResult.Error("Fadlan geli lambar sax ah oo Soomaali ah (9 lambar sida 61xxxxxxx ama 12 lambar sida 25261xxxxxxx).")

        if (password.isEmpty()) {
            return AuthResult.Error("Fadlan geli eraygaaga sirta ah.")
        }

        val internalEmail = PhoneUtils.toInternalEmail(normalizedPhone)

        return try {
            supabaseClient.auth.signInWith(Email) {
                email = internalEmail
                this.password = password
            }

            val user = supabaseClient.auth.currentUserOrNull()
            val userId = user?.id ?: ""
            val name = (user?.userMetadata?.get("full_name") as? JsonPrimitive)?.content ?: ""
            userPreferences.saveCachedUserData(
                userId = userId,
                fullName = name,
                phone = normalizedPhone
            )
            userPreferences.setOnboardingCompleted(true)
            AuthResult.Success(user)
        } catch (e: Exception) {
            mapAuthException(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            userPreferences.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapAuthException(e: Exception): AuthResult.Error {
        val message = e.message?.lowercase() ?: ""
        return when {
            e is UnknownHostException || e is ConnectException || e is SocketTimeoutException || e is HttpRequestException -> {
                AuthResult.Error("Ma jiro qadka intarneetka. Fadlan hubi xiriirkaaga.")
            }
            message.contains("already registered") || message.contains("user_already_exists") || message.contains("identity already exists") -> {
                AuthResult.Error("Lambarkan horay ayaa loo diiwaangeliyay. Fadlan gal akoonkaaga.")
            }
            message.contains("invalid login credentials") || message.contains("invalid credentials") || message.contains("invalid_grant") -> {
                AuthResult.Error("Lambarka ama erayga sirta ah waa khalad. Fadlan hubi.")
            }
            message.contains("password") && (message.contains("least") || message.contains("short")) -> {
                AuthResult.Error("Erayga sirta ah waa inuu ugu yaraan ka koobnaadaa 6 xaraf.")
            }
            e is RestException -> {
                AuthResult.Error(e.description ?: "Khalad ayaa dhacay. Fadlan dib iskugu day.")
            }
            else -> {
                AuthResult.Error(e.localizedMessage ?: "Khalad ayaa dhacay. Fadlan dib iskugu day.")
            }
        }
    }
}
