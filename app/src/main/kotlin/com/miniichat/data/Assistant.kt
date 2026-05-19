package com.miniichat.data

import kotlinx.serialization.Serializable

@Serializable
data class Assistant(
    val id: String,
    val name: String,
    val avatar: String = "🤖",
    val systemPrompt: String = "You are a helpful assistant.",
    val preferredProviderId: String? = null,
    val preferredModel: String? = null,
    val temperature: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object AssistantPresets {
    fun defaults(): List<Assistant> = listOf(
        Assistant(
            id = "default",
            name = "Default",
            avatar = "🤖",
            systemPrompt = "You are a helpful assistant. Today is {date}. The user is talking to you via {model}."
        ),
        Assistant(
            id = "coder",
            name = "Coder",
            avatar = "💻",
            systemPrompt = "You are an expert programmer. Answer with concise, correct code. " +
                "Prefer modern idiomatic style. When showing code, always use fenced code blocks with the language tag. " +
                "Today is {date}."
        ),
        Assistant(
            id = "translator",
            name = "Translator",
            avatar = "🌐",
            systemPrompt = "You are a professional translator. Translate the user's input between English and Chinese. " +
                "Preserve tone, formatting and technical terms. If the input is mixed, translate every sentence to the other language."
        ),
        Assistant(
            id = "writer",
            name = "Writer",
            avatar = "✍️",
            systemPrompt = "You are a writing partner. Help the user write clear, engaging prose. " +
                "Suggest edits, rephrase, brainstorm ideas. Keep responses tight unless asked for longer drafts."
        )
    )
}
