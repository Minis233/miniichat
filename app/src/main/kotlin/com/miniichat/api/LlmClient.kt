package com.miniichat.api

import com.miniichat.data.AppSettings
import com.miniichat.data.ProviderConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ChatPart(
    val type: String,                 // "text" | "image_url"
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null
) {
    @Serializable
    data class ImageUrl(val url: String, val detail: String? = null)
}

/**
 * A chat message that can either carry plain text (string content) or a
 * multipart payload (text + image parts). We serialize manually to keep
 * compatibility with both OpenAI-style string content and array-of-parts.
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val parts: List<ChatPart>? = null
) {
    fun isMultipart(): Boolean = !parts.isNullOrEmpty()
}

@Serializable
private data class ChatChunk(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(
        val delta: Delta? = null,
        val message: ResponseMessage? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )
    @Serializable
    data class Delta(val content: String? = null, val role: String? = null)
    @Serializable
    data class ResponseMessage(val role: String = "assistant", val content: String = "")
}

@Serializable
private data class ChatResponse(
    val choices: List<ChatChunk.Choice> = emptyList()
)

@Serializable
private data class ModelEntry(val id: String)

@Serializable
private data class ModelsResponse(val data: List<ModelEntry> = emptyList())

class LlmClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }

    private fun chatEndpoint(baseUrl: String) = "${baseUrl.trimEnd('/')}/chat/completions"
    private fun modelsEndpoint(baseUrl: String) = "${baseUrl.trimEnd('/')}/models"

    /** Best-effort parse of a value that may be a number, bool, or string. */
    private fun coerceToJson(v: String): JsonElement {
        if (v.equals("true", true)) return JsonPrimitive(true)
        if (v.equals("false", true)) return JsonPrimitive(false)
        v.toDoubleOrNull()?.let { return JsonPrimitive(it) }
        v.toLongOrNull()?.let { return JsonPrimitive(it) }
        return JsonPrimitive(v)
    }

    private fun buildRequestBody(
        provider: ProviderConfig,
        modelId: String,
        messages: List<ChatMessage>,
        stream: Boolean,
        temperature: Float
    ): String {
        val msgsJson = kotlinx.serialization.json.buildJsonArray {
            for (m in messages) {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", m.role)
                    if (m.isMultipart()) {
                        put("content", kotlinx.serialization.json.buildJsonArray {
                            for (part in m.parts!!) {
                                add(json.encodeToJsonElement(ChatPart.serializer(), part))
                            }
                        })
                    } else {
                        put("content", m.content)
                    }
                })
            }
        }
        val obj = buildJsonObject {
            put("model", modelId)
            put("stream", stream)
            put("temperature", temperature)
            put("messages", msgsJson)
            for ((k, v) in provider.extraBody) {
                if (k.isBlank()) continue
                put(k, coerceToJson(v))
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    suspend fun listModels(provider: ProviderConfig): List<String> {
        val resp = client.get(modelsEndpoint(provider.baseUrl)) {
            headers {
                if (provider.apiKey.isNotBlank()) {
                    append(HttpHeaders.Authorization, "Bearer ${provider.apiKey}")
                }
                for ((k, v) in provider.customHeaders) {
                    if (k.isBlank()) continue
                    append(k, v)
                }
            }
        }
        if (!resp.status.isSuccess()) {
            val err = runCatching { resp.bodyAsText() }.getOrDefault("")
            throw RuntimeException("HTTP ${resp.status.value}: ${err.take(300)}")
        }
        val text = resp.bodyAsText()
        val parsed = runCatching {
            json.decodeFromString(ModelsResponse.serializer(), text)
        }.getOrNull()
        if (parsed != null && parsed.data.isNotEmpty()) {
            return parsed.data.map { it.id }.distinct().sorted()
        }
        val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(text).map { it.groupValues[1] }.toList()
        return ids.distinct().sorted()
    }

    fun chatStream(
        provider: ProviderConfig,
        settings: AppSettings,
        modelId: String,
        messages: List<ChatMessage>
    ): Flow<String> = flow {
        val bodyText = buildRequestBody(
            provider = provider,
            modelId = modelId,
            messages = messages,
            stream = settings.stream,
            temperature = settings.temperature
        )

        client.preparePost(chatEndpoint(provider.baseUrl)) {
            contentType(ContentType.Application.Json)
            headers {
                if (provider.apiKey.isNotBlank()) {
                    append(HttpHeaders.Authorization, "Bearer ${provider.apiKey}")
                }
                append(HttpHeaders.Accept, if (settings.stream) "text/event-stream" else "application/json")
                for ((k, v) in provider.customHeaders) {
                    if (k.isBlank()) continue
                    append(k, v)
                }
            }
            setBody(bodyText)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val errBody = runCatching { response.bodyAsText() }.getOrDefault("")
                throw RuntimeException("HTTP ${response.status.value}: ${errBody.take(500)}")
            }
            if (settings.stream) {
                val channel: ByteReadChannel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.isEmpty()) continue
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    if (payload.isEmpty()) continue
                    val chunk = runCatching {
                        json.decodeFromString(ChatChunk.serializer(), payload)
                    }.getOrNull() ?: continue
                    val delta = chunk.choices.firstOrNull()?.delta?.content
                    if (!delta.isNullOrEmpty()) emit(delta)
                }
            } else {
                val text = response.bodyAsText()
                val parsed = runCatching {
                    json.decodeFromString(ChatResponse.serializer(), text)
                }.getOrNull()
                val content = parsed?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrEmpty()) emit(content)
            }
        }
    }
}
