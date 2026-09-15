package minmul.memoir.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AnalysisEntity(
    val name: String,
    val value: String,
)

data class AnalysisPayload(
    val title: String,
    val detailedSummary: String,
    val time: List<AnalysisEntity> = emptyList(),
    val period: List<AnalysisEntity> = emptyList(),
    val location: List<AnalysisEntity> = emptyList(),
    val account: List<AnalysisEntity> = emptyList(),
    val phone: List<AnalysisEntity> = emptyList(),
) {
    fun encoded(): String = buildJsonObject {
        put("title", title)
        put("detailed_summary", detailedSummary)
        putEntities("time", time)
        putEntities("period", period)
        putEntities("location", location)
        putEntities("account", account)
        putEntities("phone", phone)
    }.toString()

    sealed class ParseResult {
        data class Success(val payload: AnalysisPayload) : ParseResult()
        data class Failure(
            val reason: ParseFailure,
            val payloadChars: Int,
        ) : ParseResult()
    }

    enum class ParseFailure {
        EMPTY, MISSING_TITLE, MISSING_DELIMITER, MISSING_ENTITY_NAME, ;

        val logName: String = name.lowercase()
    }

    companion object {
        fun parse(raw: String): AnalysisPayload? {
            val element = runCatching { Json.parseToJsonElement(jsonCandidate(raw)) }.getOrNull()
                ?: return null
            val obj = element as? JsonObject ?: return null
            val title = obj.nonBlankString("title") ?: return null
            val detailedSummary = obj.stringField("detailed_summary") ?: return null
            val parsed = AnalysisPayload(
                title,
                detailedSummary,
                time = obj.entities("time"),
                period = obj.entities("period"),
                location = obj.entities("location"),
                account = obj.entities("account"),
                phone = obj.entities("phone"),
            )
            return parsed.withRecoveredEntities()
        }

        fun parseResult(raw: String): ParseResult {
            fun fail(reason: ParseFailure) = ParseResult.Failure(reason, raw.length)
            val lines = raw.lines().dropWhile { it.isBlank() }
            if (lines.isEmpty()) return fail(ParseFailure.EMPTY)
            if (!hasLabel(lines[0], "title")) return fail(ParseFailure.MISSING_TITLE)
            val title = valueAfterLabel(lines[0], "title").orEmpty()
            if (title.isBlank()) return fail(ParseFailure.MISSING_TITLE)
            var index = 1
            if (index < lines.size && hasLabel(lines[index], "summary")) {
                index++
            }
            if (index >= lines.size || lines[index].trim() != "---") {
                return fail(ParseFailure.MISSING_DELIMITER)
            }
            val afterHeader = lines.drop(index + 1)
            if (afterHeader.any(::isBareEntityLine)) {
                return fail(ParseFailure.MISSING_ENTITY_NAME)
            }
            val split = splitEntities(afterHeader)
            return ParseResult.Success(
                AnalysisPayload(
                    title,
                    split.body,
                    time = split.entities.time,
                    period = split.entities.period,
                    location = split.entities.location,
                    account = split.entities.account,
                    phone = split.entities.phone,
                ),
            )
        }

        private fun hasLabel(line: String, label: String): Boolean =
            line.trimStart().startsWith("$label:", ignoreCase = true)

        private fun valueAfterLabel(line: String, label: String): String? {
            val trimmed = line.trimStart()
            val prefix = "$label:"
            if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
            return trimmed.substring(prefix.length).trim()
        }

        private fun AnalysisPayload.withRecoveredEntities(): AnalysisPayload {
            if (hasEntities) return this
            val split = splitEntities(detailedSummary.lines())
            if (!split.entities.hasAny) return this
            return copy(
                detailedSummary = split.body,
                time = split.entities.time,
                period = split.entities.period,
                location = split.entities.location,
                account = split.entities.account,
                phone = split.entities.phone,
            )
        }

        private val AnalysisPayload.hasEntities: Boolean
            get() = time.isNotEmpty() ||
                    period.isNotEmpty() ||
                    location.isNotEmpty() ||
                    account.isNotEmpty() ||
                    phone.isNotEmpty()

        private fun splitEntities(lines: List<String>): BodyAndEntities {
            val lastDelim = lines.indexOfLast { it.trim() == "---" }
            if (lastDelim >= 0) {
                val trailing = parseEntities(lines.drop(lastDelim + 1))
                if (trailing.hasAny) {
                    return BodyAndEntities(
                        lines.take(lastDelim).joinToString("\n").trim(),
                        trailing,
                    )
                }
            }
            val leadEnd = leadingEntityEnd(lines)
            if (leadEnd > 0) {
                val entities = parseEntities(lines.take(leadEnd))
                var rest = lines.drop(leadEnd)
                if (rest.firstOrNull()?.trim() == "---") rest = rest.drop(1)
                return BodyAndEntities(rest.joinToString("\n").trim(), entities)
            }
            return BodyAndEntities(lines.joinToString("\n").trim(), EntityLists())
        }

        private fun leadingEntityEnd(lines: List<String>): Int {
            var index = 0
            while (index < lines.size && lines[index].isBlank()) index++
            val start = index
            while (index < lines.size && parseEntityLine(lines[index]) != null) index++
            return if (index > start) index else 0
        }

        private fun parseEntities(lines: List<String>): EntityLists {
            val grouped = lines.mapNotNull(::parseEntityLine).groupBy({ it.first }, { it.second })
            return EntityLists(
                time = grouped["time"].orEmpty(),
                period = grouped["period"].orEmpty(),
                location = grouped["location"].orEmpty(),
                account = grouped["account"].orEmpty(),
                phone = grouped["phone"].orEmpty(),
            )
        }

        private fun parseEntityLine(line: String): Pair<String, AnalysisEntity>? {
            val named = NAMED_ENTITY.matchEntire(line) ?: return null
            val name = named.groupValues[2].trim()
            val value = named.groupValues[3].trim()
            if (name.isEmpty() || value.isEmpty()) return null
            return named.groupValues[1].lowercase() to AnalysisEntity(name, value)
        }

        private fun isBareEntityLine(line: String): Boolean =
            NAMED_ENTITY.matchEntire(line) == null && BARE_ENTITY.matchEntire(line) != null

        private fun jsonCandidate(raw: String): String {
            val trimmed = raw.trim()
            val fenced = FENCE.find(trimmed)?.groupValues?.get(1)?.trim()
            val body = fenced ?: trimmed
            if (body.startsWith("{")) return body
            val start = body.indexOf('{')
            val end = body.lastIndexOf('}')
            return if (start in 0..<end) body.substring(start, end + 1) else body
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

        private fun JsonObject.entities(key: String): List<AnalysisEntity> {
            val array = this[key] as? JsonArray ?: return emptyList()
            return array.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val name = obj.nonBlankString("name") ?: return@mapNotNull null
                val value = obj.nonBlankString("value") ?: return@mapNotNull null
                AnalysisEntity(name, value)
            }
        }

        private fun JsonObjectBuilder.putEntities(
            key: String,
            entities: List<AnalysisEntity>,
        ) {
            if (entities.isEmpty()) return
            put(
                key,
                buildJsonArray {
                    entities.forEach { entity ->
                        add(
                            buildJsonObject {
                                put("name", entity.name)
                                put("value", entity.value)
                            },
                        )
                    }
                },
            )
        }

        private val NAMED_ENTITY = Regex(
            """^\s*(time|period|location|account|phone)\(([^)]+)\)\s*:\s*(.*)$""",
            RegexOption.IGNORE_CASE,
        )

        private val BARE_ENTITY = Regex(
            """^\s*(time|period|location|account|phone)\s*:\s*(.*)$""",
            RegexOption.IGNORE_CASE,
        )

        private val FENCE = Regex(
            """```(?:json)?\s*\n?(.*?)\n?```""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}

private data class BodyAndEntities(
    val body: String,
    val entities: EntityLists,
)

private data class EntityLists(
    val time: List<AnalysisEntity> = emptyList(),
    val period: List<AnalysisEntity> = emptyList(),
    val location: List<AnalysisEntity> = emptyList(),
    val account: List<AnalysisEntity> = emptyList(),
    val phone: List<AnalysisEntity> = emptyList(),
) {
    val hasAny: Boolean
        get() = time.isNotEmpty() || period.isNotEmpty() || location.isNotEmpty() || account.isNotEmpty() || phone.isNotEmpty()
}
