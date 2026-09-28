package com.koor.app.data.remote

import com.koor.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.Android

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        val rawUrl = BuildConfig.SUPABASE_URL.trim()
        val url = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://placeholder.supabase.co"
        }
        val rawKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        val key = if (rawKey.isNotEmpty()) rawKey else "placeholder-anon-key"

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            httpEngine = Android.create()
            install(Auth)
            install(Postgrest)
        }
    }
}
