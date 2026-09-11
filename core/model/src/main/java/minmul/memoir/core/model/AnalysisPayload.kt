package minmul.memoir.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AnalysisPayload(
    val title: String,
    val summary: String?,
    val detailedSummary: String,
) {
    fun encoded(): String = buildJsonObject {
        put("title", title)
        summary?.let { put("summary", it) }
        put("detailed_summary", detailedSummary)
    }.toString()

    sealed class ParseResult {
        data class Success(val payload: AnalysisPayload) : ParseResult()
        data class Failure(
            val reason: ParseFailure,
            val payloadChars: Int,
        ) : ParseResult()
    }

    enum class ParseFailure {
        EMPTY,
        MISSING_TITLE,
        MISSING_DELIMITER,
        ;

        val logName: String = name.lowercase()
    }

    companion object {
        fun parse(raw: String): AnalysisPayload? {
            val element = runCatching { Json.parseToJsonElement(jsonCandidate(raw)) }.getOrNull()
                ?: return null
            val obj = element as? JsonObject ?: return null
            val title = obj.nonBlankString("title") ?: return null
            val detailedSummary = obj.stringField("detailed_summary") ?: return null
            return AnalysisPayload(title, obj.nonBlankString("summary"), detailedSummary)
        }

        fun parseResult(raw: String): ParseResult {
            fun fail(reason: ParseFailure) = ParseResult.Failure(reason, raw.length)
            val lines = raw.lines().dropWhile { it.isBlank() }
            if (lines.isEmpty()) return fail(ParseFailure.EMPTY)
            if (!hasLabel(lines[0], "title")) return fail(ParseFailure.MISSING_TITLE)
            val title = valueAfterLabel(lines[0], "title").orEmpty()
            if (title.isBlank()) return fail(ParseFailure.MISSING_TITLE)
            var index = 1
            var summary: String? = null
            if (index < lines.size && hasLabel(lines[index], "summary")) {
                summary = valueAfterLabel(lines[index], "summary")?.takeIf { it.isNotBlank() }
                index++
            }
            if (index >= lines.size || lines[index].trim() != "---") {
                return fail(ParseFailure.MISSING_DELIMITER)
            }
            val body = lines.drop(index + 1).joinToString("\n").trim()
            return ParseResult.Success(AnalysisPayload(title, summary, body))
        }

        private fun hasLabel(line: String, label: String): Boolean =
            line.trimStart().startsWith("$label:", ignoreCase = true)

        private fun valueAfterLabel(line: String, label: String): String? {
            val trimmed = line.trimStart()
            val prefix = "$label:"
            if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
            return trimmed.substring(prefix.length).trim()
        }

        private fun jsonCandidate(raw: String): String {
            val trimmed = raw.trim()
            val fenced = FENCE.find(trimmed)?.groupValues?.get(1)?.trim()
            val body = fenced ?: trimmed
            if (body.startsWith("{")) return body
            val start = body.indexOf('{')
            val end = body.lastIndexOf('}')
            return if (start >= 0 && end > start) body.substring(start, end + 1) else body
        }

        private fun JsonObject.stringField(key: String): String? {
            val value = this[key] ?: return null
            if (value is JsonNull) return ""
            val primitive = value as? JsonPrimitive ?: return null
            if (!primitive.isString) return null
            return primitive.content
        }

        private fun JsonObject.nonBlankString(key: String): String? =
            stringField(key)?.takeIf { it.isNotBlank() }

        private val FENCE = Regex(
            """```(?:json)?\s*\n?(.*?)\n?```""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
