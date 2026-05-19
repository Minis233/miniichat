package com.miniichat.data

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val models: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

object ProviderPresets {
    data class Preset(
        val name: String,
        val baseUrl: String,
        val sampleModel: String,
        val hint: String
    )

    val all: List<Preset> = listOf(
        Preset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini",
            "Official OpenAI API"),
        Preset("OpenRouter", "https://openrouter.ai/api/v1", "openrouter/auto",
            "Aggregator for OpenAI / Anthropic / Gemini / open-source"),
        Preset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat",
            "DeepSeek-V3 / R1"),
        Preset("Groq", "https://api.groq.com/openai/v1", "llama-3.1-70b-versatile",
            "Fast inference, free tier"),
        Preset("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest",
            "Mistral models"),
        Preset("Together AI", "https://api.together.xyz/v1",
            "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
            "Open-source models hosting"),
        Preset("Gemini (OpenAI shim)",
            "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini-1.5-flash",
            "Google Gemini via OpenAI-compatible endpoint"),
        Preset("SiliconFlow",
            "https://api.siliconflow.cn/v1",
            "Qwen/Qwen2.5-7B-Instruct",
            "国内聚合，速度快"),
        Preset("Ollama (local)", "http://10.0.2.2:11434/v1", "llama3.2",
            "Local Ollama via emulator (10.0.2.2)"),
        Preset("LM Studio (local)", "http://10.0.2.2:1234/v1", "local-model",
            "Local LM Studio via emulator"),
        Preset("Custom", "https://", "",
            "Any OpenAI-compatible endpoint"),
    )
}
