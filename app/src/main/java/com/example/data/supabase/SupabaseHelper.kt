package com.example.data.supabase

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.delay

object SupabaseHelper {
    val client = createSupabaseClient(
        supabaseUrl = "https://sazbudkzuxbvmuztaxeg.supabase.co",
        supabaseKey = "sb_publishable_vvR8V-Y4Ge4-PMZa1AuFnQ_t9TJrwnx"
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    suspend fun <T> safeCall(
        retryCount: Int = 3,
        delayMillis: Long = 2000,
        onRetry: (attempt: Int, message: String) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): Result<T> {
        var lastError: Throwable? = null
        for (attempt in 1..retryCount) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastError = e
                val message = e.localizedMessage ?: e.message ?: "خطأ في الاتصال بالشبكة"
                if (attempt < retryCount) {
                    onRetry(attempt, message)
                    delay(delayMillis)
                }
            }
        }
        return Result.failure(lastError ?: Exception("فشل الاتصال بخادم دليلي، يرجى المحاولة لاحقاً"))
    }
}
