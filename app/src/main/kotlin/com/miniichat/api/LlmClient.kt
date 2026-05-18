package com.miniichat.api

import com.miniichat.data.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
    val temperature: Float
)

@Serializable
private data class ChatChunk(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(
        val delta: Delta? = null,
        val message: ChatMessage? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )
    @Serializable
    data class Delta(val content: String? = null, val role: String? = null)
}

@Serializable
private data class ChatResponse(
    val choices: List<ChatChunk.Choice> = emptyList()
)

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

    private fun endpoint(baseUrl: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return "$trimmed/chat/completions"
    }

    /**
     * Streams response chunks. Emits delta strings as they arrive.
     * If [settings.stream] is false, emits the full content once.
     */
    fun chatStream(settings: AppSettings, messages: List<ChatMessage>): Flow<String> = flow {
        val req = ChatRequest(
            model = settings.model,
            messages = messages,
            stream = settings.stream,
            temperature = settings.temperature
        )
        val bodyText = json.encodeToString(ChatRequest.serializer(), req)

        client.preparePost(endpoint(settings.baseUrl)) {
            contentType(ContentType.Application.Json)
            headers {
                if (settings.apiKey.isNotBlank()) {
                    append(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                }
                append(HttpHeaders.Accept, if (settings.stream) "text/event-stream" else "application/json")
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
