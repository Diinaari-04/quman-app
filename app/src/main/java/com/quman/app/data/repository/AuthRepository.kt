package com.quman.app.data.repository

import com.quman.app.data.preferences.UserPreferencesRepository
import com.quman.app.util.PhoneUtils
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
        val rawMessage = e.message ?: ""
        val message = rawMessage.lowercase()
        val cause = e.cause
        val causeMessage = cause?.message?.lowercase() ?: ""

        // Detect if build/app is still configured with placeholder Supabase URL
        if (message.contains("placeholder.supabase.co") || causeMessage.contains("placeholder.supabase.co")) {
            return AuthResult.Error("Supabase URL lama habaynin (Placeholder ayaa weli ku jirta). Fadlan habee furayaasha Supabase.")
        }

        // Handle structured Supabase REST exceptions first
        if (e is RestException) {
            val desc = e.description
            val restMsg = desc?.lowercase() ?: message
            when {
                restMsg.contains("already registered") || restMsg.contains("user_already_exists") || restMsg.contains("identity already exists") -> {
                    return AuthResult.Error("Lambarkan horay ayaa loo diiwaangeliyay. Fadlan gal akoonkaaga.")
                }
                restMsg.contains("invalid login credentials") || restMsg.contains("invalid credentials") || restMsg.contains("invalid_grant") -> {
                    return AuthResult.Error("Lambarka ama erayga sirta ah waa khalad. Fadlan hubi.")
                }
                restMsg.contains("password") && (restMsg.contains("least") || restMsg.contains("short") || restMsg.contains("weak")) -> {
                    return AuthResult.Error("Erayga sirta ah waa inuu ugu yaraan ka koobnaadaa 6 xaraf.")
                }
                restMsg.contains("rate limit") || restMsg.contains("over_email_send_rate_limit") || restMsg.contains("too many requests") || restMsg.contains("429") -> {
                    return AuthResult.Error("Codsi badan ayaa la diray mar qura. Fadlan sug cabbaar ka hor inta aadan dib isku dayin.")
                }
                restMsg.contains("signup is disabled") || restMsg.contains("signups not allowed") -> {
                    return AuthResult.Error("Diiwaangelinta akoonnada cusub hadda waa xiran tahay.")
                }
                !desc.isNullOrBlank() -> {
                    return AuthResult.Error(desc)
                }
            }
        }

        // Check common Supabase auth error patterns from message or cause
        if (message.contains("already registered") || message.contains("user_already_exists") || 
            message.contains("identity already exists") || causeMessage.contains("user_already_exists")) {
            return AuthResult.Error("Lambarkan horay ayaa loo diiwaangeliyay. Fadlan gal akoonkaaga.")
        }

        if (message.contains("invalid login credentials") || message.contains("invalid credentials") || 
            message.contains("invalid_grant") || causeMessage.contains("invalid_grant")) {
            return AuthResult.Error("Lambarka ama erayga sirta ah waa khalad. Fadlan hubi.")
        }

        if ((message.contains("password") || causeMessage.contains("password")) && 
            (message.contains("least") || message.contains("short") || message.contains("weak"))) {
            return AuthResult.Error("Erayga sirta ah waa inuu ugu yaraan ka koobnaadaa 6 xaraf.")
        }

        if (message.contains("rate limit") || message.contains("over_email_send_rate_limit") || 
            message.contains("too many requests") || message.contains("429") || causeMessage.contains("429")) {
            return AuthResult.Error("Codsi badan ayaa la diray. Fadlan sug cabbaar ka hor inta aadan dib isku dayin.")
        }

        if (message.contains("signup is disabled") || message.contains("signups not allowed")) {
            return AuthResult.Error("Diiwaangelinta akoonnada cusub hadda waa xiran tahay.")
        }

        // True network connectivity failures (DNS resolution failure or connection refused)
        if (e is UnknownHostException || cause is UnknownHostException || 
            e is ConnectException || cause is ConnectException) {
            return AuthResult.Error("Ma jiro qadka intarneetka ama server-ka lama gaari karo. Fadlan hubi xiriirkaaga.")
        }

        // Timeout
        if (e is SocketTimeoutException || cause is SocketTimeoutException) {
            return AuthResult.Error("Xiriirka wuu daahay (Timeout). Fadlan dib iskugu day.")
        }

        // Generic HTTP request exception with detail
        if (e is HttpRequestException) {
            val detail = cause?.localizedMessage ?: rawMessage
            return if (detail.isNotBlank()) {
                AuthResult.Error("Khalad xiriirka HTTP ah: $detail")
            } else {
                AuthResult.Error("Ma jiro qadka intarneetka. Fadlan hubi xiriirkaaga.")
            }
        }

        return AuthResult.Error(
            e.localizedMessage ?: "Khalad aan la filayn ayaa dhacay. Fadlan dib iskugu day."
        )
    }
}
