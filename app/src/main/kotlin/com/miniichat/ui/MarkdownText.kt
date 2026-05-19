package com.miniichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal Markdown renderer (block + inline). No external dependency.
 * Supports headings, paragraphs, fenced code, lists, quotes, hr, bold/italic/code/strike/link.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { idx, block ->
            if (idx > 0) Spacer(Modifier.height(if (block is Block.CodeBlock || blocks[idx - 1] is Block.CodeBlock) 8.dp else 6.dp))
            when (block) {
                is Block.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(parseInline(block.text), color = color, style = style)
                }
                is Block.Paragraph -> {
                    Text(parseInline(block.text), color = color, style = MaterialTheme.typography.bodyLarge)
                }
                is Block.BulletItem -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("•  ", color = color, style = MaterialTheme.typography.bodyLarge)
                        Text(parseInline(block.text), color = color, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is Block.NumberedItem -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("${block.index}. ", color = color, style = MaterialTheme.typography.bodyLarge)
                        Text(parseInline(block.text), color = color, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is Block.Quote -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            parseInline(block.text),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        )
                    }
                }
                is Block.Hr -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                is Block.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    ) {
                        if (block.lang.isNotBlank()) {
                            Text(
                                block.lang,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                block.code,
                                color = color,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed class Block {
    data class Heading(val level: Int, val text: String) : Block()
    data class Paragraph(val text: String) : Block()
    data class BulletItem(val text: String) : Block()
    data class NumberedItem(val index: Int, val text: String) : Block()
    data class Quote(val text: String) : Block()
    data class CodeBlock(val lang: String, val code: String) : Block()
    object Hr : Block()
}

private fun parseBlocks(text: String): List<Block> {
    val result = mutableListOf<Block>()
    val lines = text.split("\n")
    var i = 0
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) result.add(Block.Paragraph(paragraph.toString().trim()))
        paragraph.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.append(lines[i]).append('\n'); i++
            }
            result.add(Block.CodeBlock(lang, code.toString().trimEnd('\n')))
            if (i < lines.size) i++
            continue
        }
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            flushParagraph(); result.add(Block.Hr); i++; continue
        }
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            result.add(Block.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++; continue
        }
        if (trimmed.startsWith("> ") || trimmed == ">") {
            flushParagraph()
            result.add(Block.Quote(trimmed.removePrefix(">").trimStart()))
            i++; continue
        }
        val bulletMatch = Regex("^[-*+]\\s+(.+)$").matchEntire(trimmed)
        if (bulletMatch != null) {
            flushParagraph(); result.add(Block.BulletItem(bulletMatch.groupValues[1])); i++; continue
        }
        val numberedMatch = Regex("^(\\d+)[.)]\\s+(.+)$").matchEntire(trimmed)
        if (numberedMatch != null) {
            flushParagraph()
            result.add(Block.NumberedItem(numberedMatch.groupValues[1].toInt(), numberedMatch.groupValues[2]))
            i++; continue
        }
        if (line.isBlank()) { flushParagraph(); i++; continue }
        if (paragraph.isNotEmpty()) paragraph.append(' ')
        paragraph.append(line.trim())
        i++
    }
    flushParagraph()
    return result
}

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22808080), fontSize = 14.sp))
                append(text.substring(i + 1, end)); pop()
                i = end + 1; continue
            }
        }
        if (rest.startsWith("**")) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(parseInline(text.substring(i + 2, end))); pop()
                i = end + 2; continue
            }
        }
        if (rest.startsWith("~~")) {
            val end = text.indexOf("~~", i + 2)
            if (end > i + 2) {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                append(parseInline(text.substring(i + 2, end))); pop()
                i = end + 2; continue
            }
        }
        if ((text[i] == '*' && !rest.startsWith("**")) || text[i] == '_') {
            val ch = text[i]
            val end = text.indexOf(ch, i + 1)
            if (end > i + 1 && (end + 1 >= text.length || text[end + 1] != ch)) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(i + 1, end)); pop()
                i = end + 1; continue
            }
        }
        if (text[i] == '[') {
            val close = text.indexOf(']', i + 1)
            if (close > i && close + 1 < text.length && text[close + 1] == '(') {
                val urlEnd = text.indexOf(')', close + 2)
                if (urlEnd > close + 1) {
                    val label = text.substring(i + 1, close)
                    pushStyle(SpanStyle(color = Color(0xFF7C5CFF), textDecoration = TextDecoration.Underline))
                    append(label); pop()
                    i = urlEnd + 1; continue
                }
            }
        }
        append(text[i]); i++
    }
}
