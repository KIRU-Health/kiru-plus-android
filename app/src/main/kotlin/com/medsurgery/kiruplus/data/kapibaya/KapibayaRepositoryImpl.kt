package com.medsurgery.kiruplus.data.kapibaya

import com.medsurgery.kiruplus.BuildConfig
import com.medsurgery.kiruplus.domain.kapibaya.KapibayaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KapibayaRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val okHttpClient: OkHttpClient,
) : KapibayaRepository {

    override fun sendMessageStream(conversationId: String, message: String): Flow<String> = flow {
        val session = supabase.auth.currentSessionOrNull()
        val accessToken = session?.accessToken
        val userId = session?.user?.id?.lowercase()
        if (accessToken.isNullOrBlank() || userId.isNullOrBlank()) {
            error("No active session — login required to chat with Dr. Kapibaya.")
        }

        val request = kapibayaStreamRequest(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
            accessToken = accessToken,
            userId = userId,
            conversationId = conversationId,
            message = message,
        )

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val source = response.body?.source() ?: error("Empty response body")

            var currentEvent = ""
            val dataBuffer = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event:") -> {
                        currentEvent = line.removePrefix("event:").trim()
                        dataBuffer.clear()
                    }
                    line.startsWith("data:") -> {
                        dataBuffer.append(line.removePrefix("data:").trim())
                    }
                    line.isEmpty() && currentEvent.isNotEmpty() -> {
                        val data = dataBuffer.toString()
                        when (currentEvent) {
                            "chunk" -> {
                                val obj = runCatching {
                                    Json.parseToJsonElement(data) as? JsonObject
                                }.getOrNull()
                                val chunk = obj?.get("content")?.jsonPrimitive?.content
                                if (!chunk.isNullOrBlank()) emit(chunk)
                            }
                            "done" -> return@flow
                            "error" -> {
                                val obj = runCatching {
                                    Json.parseToJsonElement(data) as? JsonObject
                                }.getOrNull()
                                val errMsg = obj?.get("error")?.jsonPrimitive?.content ?: "Stream error"
                                Timber.w("Kapibaya stream error: %s", errMsg)
                                error(errMsg)
                            }
                        }
                        currentEvent = ""
                        dataBuffer.clear()
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}

internal fun kapibayaStreamRequest(
    supabaseUrl: String,
    anonKey: String,
    accessToken: String,
    userId: String,
    conversationId: String,
    message: String,
): Request {
    val bodyJson = buildJsonObject {
        put("message", message)
        put("conversationId", conversationId)
        put("userId", userId.lowercase())
        put("stream", true)
    }.toString()
    return Request.Builder()
        .url("${supabaseUrl.trimEnd('/')}/functions/v1/ask_kapibaya")
        .post(bodyJson.toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Accept", "text/event-stream")
        .addHeader("Content-Type", "application/json")
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class KapibayaRepositoryBindings {
    @Binds
    abstract fun bindKapibayaRepository(impl: KapibayaRepositoryImpl): KapibayaRepository
}
