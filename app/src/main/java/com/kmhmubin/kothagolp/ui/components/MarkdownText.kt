package com.kmhmubin.kothagolp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders markdown-formatted text with proper styling
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val formattedSections = parseMarkdownSections(text)

    Column(modifier = modifier) {
        formattedSections.take(maxLines).forEachIndexed { index, section ->
            when (section) {
                is MarkdownSection.Header1 -> {
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                }
                is MarkdownSection.Header2 -> {
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                }
                is MarkdownSection.Header3 -> {
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MarkdownSection.BulletPoint -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(section.text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownSection.NumberedItem -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        Text(
                            text = "${section.number}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(section.text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownSection.Paragraph -> {
                    if (section.text.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(section.text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownSection.Spacer -> {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

private sealed class MarkdownSection {
    data class Header1(val text: String) : MarkdownSection()
    data class Header2(val text: String) : MarkdownSection()
    data class Header3(val text: String) : MarkdownSection()
    data class BulletPoint(val text: String) : MarkdownSection()
    data class NumberedItem(val number: Int, val text: String) : MarkdownSection()
    data class Paragraph(val text: String) : MarkdownSection()
    object Spacer : MarkdownSection()
}

private fun parseMarkdownSections(text: String): List<MarkdownSection> {
    val sections = mutableListOf<MarkdownSection>()
    val lines = text.lines()

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()

        when {
            line.isEmpty() -> {
                sections.add(MarkdownSection.Spacer)
            }
            line.startsWith("### ") -> {
                sections.add(MarkdownSection.Header3(line.removePrefix("### ").trim()))
            }
            line.startsWith("## ") -> {
                sections.add(MarkdownSection.Header2(line.removePrefix("## ").trim()))
            }
            line.startsWith("# ") -> {
                sections.add(MarkdownSection.Header1(line.removePrefix("# ").trim()))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                sections.add(MarkdownSection.BulletPoint(line.drop(2).trim()))
            }
            line.matches(Regex("^\\d+\\.\\s.*")) -> {
                val match = Regex("^(\\d+)\\.\\s(.*)").find(line)
                if (match != null) {
                    val (number, content) = match.destructured
                    sections.add(MarkdownSection.NumberedItem(number.toInt(), content.trim()))
                }
            }
            else -> {
                sections.add(MarkdownSection.Paragraph(line))
            }
        }
        i++
    }

    return sections
}

@Composable
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text

        while (remaining.isNotEmpty()) {
            // Handle **bold**
            val boldMatch = Regex("\\*\\*(.+?)\\*\\*").find(remaining)
            // Handle `code`
            val codeMatch = Regex("`(.+?)`").find(remaining)

            val firstMatch = listOfNotNull(boldMatch, codeMatch)
                .minByOrNull { it.range.first }

            if (firstMatch != null && firstMatch.range.first < remaining.length) {
                // Add text before the match
                if (firstMatch.range.first > 0) {
                    append(remaining.substring(0, firstMatch.range.first))
                }

                // Add the styled text
                when (firstMatch) {
                    boldMatch -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(firstMatch.groupValues[1])
                        }
                    }
                    codeMatch -> {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )) {
                            append(firstMatch.groupValues[1])
                        }
                    }
                }

                remaining = remaining.substring(firstMatch.range.last + 1)
            } else {
                append(remaining)
                remaining = ""
            }
        }
    }
}