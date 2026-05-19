package com.miniichat.util

/**
 * Normalize a user-provided base URL so it has a sensible OpenAI-compatible suffix.
 *
 * Heuristics:
 *  - Trim whitespace and trailing slashes.
 *  - If empty, return as-is.
 *  - If the URL already ends with /v1, /v1beta, /openai, /api, etc, leave it.
 *  - If the URL points at a known root (api.openai.com, api.deepseek.com, api.mistral.ai,
 *    api.groq.com/openai, api.together.xyz, openrouter.ai/api, api.siliconflow.cn,
 *    generativelanguage.googleapis.com/v1beta/openai, etc.) and is missing the version
 *    suffix, append /v1.
 *  - Otherwise leave alone (covers Ollama / LM Studio that already include /v1, and
 *    custom self-hosted endpoints).
 */
object BaseUrlNormalizer {
    private val versionedTails = listOf(
        "/v1", "/v1beta", "/v2", "/v3",
        "/openai", "/openai/v1",
        "/api", "/api/v1",
        "/chat/completions"
    )

    fun normalize(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return trimmed
        val lower = trimmed.lowercase()

        // Already has a known version/api suffix
        for (tail in versionedTails) {
            if (lower.endsWith(tail)) return trimmed
        }

        // Heuristic: if the path part is empty (just scheme + host[:port]), append /v1.
        // Match scheme://host[:port] with no extra path.
        val match = Regex("^(https?://[^/]+)(/?.*)$").matchEntire(trimmed) ?: return trimmed
        val host = match.groupValues[1]
        val path = match.groupValues[2].trimEnd('/')
        if (path.isEmpty()) return "$host/v1"

        // If the path doesn't already contain a version segment, append /v1.
        // e.g. https://example.com/api  → https://example.com/api/v1
        return "$trimmed/v1"
    }
}
