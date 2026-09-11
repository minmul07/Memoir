package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnalysisPayloadTest {
    @Test
    fun `parses stored json required fields and optional summary`() {
        val payload = AnalysisPayload.parse(
            """{"title":"회의","summary":"내일 10시","detailed_summary":"- 자료 공유"}""",
        )
        assertEquals(AnalysisPayload("회의", "내일 10시", "- 자료 공유"), payload)
        assertEquals(
            """{"title":"회의","summary":"내일 10시","detailed_summary":"- 자료 공유"}""",
            payload?.encoded(),
        )
    }

    @Test
    fun `omits blank or missing summary in stored json`() {
        val omitted = AnalysisPayload.parse(
            """{"title":"제목","detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), omitted)
        assertEquals("""{"title":"제목","detailed_summary":"본문"}""", omitted?.encoded())

        val blank = AnalysisPayload.parse(
            """{"title":"제목","summary":"  ","detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), blank)

        val jsonNull = AnalysisPayload.parse(
            """{"title":"제목","summary":null,"detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), jsonNull)
    }

    @Test
    fun `strips markdown fences around stored json`() {
        val payload = AnalysisPayload.parse(
            """
            다음은 JSON입니다.
            ```json
            {"title":"제목","detailed_summary":"본문"}
            ```
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), payload)
    }

    @Test
    fun `rejects invalid stored json`() {
        assertNull(AnalysisPayload.parse("""{"summary":"한 줄"}"""))
        assertNull(AnalysisPayload.parse("""{"title":"","detailed_summary":"본문"}"""))
        assertNull(AnalysisPayload.parse("not json"))
        assertNull(AnalysisPayload.parse(""))
    }

    @Test
    fun `allows empty detailed summary in stored json`() {
        assertEquals(
            AnalysisPayload("제목", null, ""),
            AnalysisPayload.parse("""{"title":"제목","detailed_summary":""}"""),
        )
    }

    @Test
    fun `parses line format title summary and body`() {
        val result = AnalysisPayload.parseResult(
            """
            title: 회의
            summary: 내일 10시
            ---
            - 자료 공유
            """.trimIndent(),
        )
        val payload = (result as AnalysisPayload.ParseResult.Success).payload
        assertEquals(AnalysisPayload("회의", "내일 10시", "- 자료 공유"), payload)
        assertEquals(
            """{"title":"회의","summary":"내일 10시","detailed_summary":"- 자료 공유"}""",
            payload.encoded(),
        )
    }

    @Test
    fun `treats missing or blank summary as null`() {
        val omitted = success(
            """
            title: 제목
            ---
            본문
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), omitted)

        val blank = success(
            """
            title: 제목
            summary:
            ---
            본문
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), blank)

        val whitespace = success(
            """
            title: 제목
            summary:   
            ---
            본문
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", null, "본문"), whitespace)
    }

    @Test
    fun `keeps markdown body after delimiter`() {
        val payload = success(
            """
            TITLE: 제목
            ---
            ## 표
            | a | b |
            """.trimIndent(),
        )
        assertEquals("제목", payload.title)
        assertNull(payload.summary)
        assertEquals("## 표\n| a | b |", payload.detailedSummary)
    }

    @Test
    fun `succeeds when body is empty`() {
        assertEquals(
            AnalysisPayload("제목", null, ""),
            success(
                """
                title: 제목
                ---
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `classifies line format parse failures`() {
        assertFailure("", AnalysisPayload.ParseFailure.EMPTY)
        assertFailure("not a title", AnalysisPayload.ParseFailure.MISSING_TITLE)
        assertFailure("title:\n---\n본문", AnalysisPayload.ParseFailure.MISSING_TITLE)
        assertFailure(
            """
            title: 제목
            본문만
            """.trimIndent(),
            AnalysisPayload.ParseFailure.MISSING_DELIMITER,
        )
    }

    @Test
    fun `encodes special characters`() {
        val encoded = AnalysisPayload("따옴표 \" 제목", null, "줄\n바꿈").encoded()
        assertEquals(AnalysisPayload("따옴표 \" 제목", null, "줄\n바꿈"), AnalysisPayload.parse(encoded))
    }

    private fun success(raw: String): AnalysisPayload =
        (AnalysisPayload.parseResult(raw) as AnalysisPayload.ParseResult.Success).payload

    private fun assertFailure(raw: String, reason: AnalysisPayload.ParseFailure) {
        val result = AnalysisPayload.parseResult(raw)
        val failure = result as AnalysisPayload.ParseResult.Failure
        assertEquals(reason, failure.reason)
        assertEquals(raw.length, failure.payloadChars)
    }
}
