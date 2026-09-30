package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnalysisPayloadTest {
    @Test
    fun `parses stored json required fields and ignores leftover summary`() {
        val payload = AnalysisPayload.parse(
            """{"title":"회의","summary":"내일 10시","detailed_summary":"- 자료 공유"}""",
        )
        assertEquals(AnalysisPayload("회의", "- 자료 공유"), payload)
        assertEquals(
            """{"title":"회의","detailed_summary":"- 자료 공유"}""",
            payload?.encoded(),
        )

        val omitted = AnalysisPayload.parse(
            """{"title":"제목","detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", "본문"), omitted)

        val blank = AnalysisPayload.parse(
            """{"title":"제목","summary":"  ","detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", "본문"), blank)

        val jsonNull = AnalysisPayload.parse(
            """{"title":"제목","summary":null,"detailed_summary":"본문"}""",
        )
        assertEquals(AnalysisPayload("제목", "본문"), jsonNull)
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
        assertEquals(AnalysisPayload("제목", "본문"), payload)
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
            AnalysisPayload("제목", ""),
            AnalysisPayload.parse("""{"title":"제목","detailed_summary":""}"""),
        )
    }

    @Test
    fun `parses line format title and body`() {
        val payload = success(
            """
            회의
            - 자료 공유
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("회의", "- 자료 공유"), payload)
        assertEquals(
            """{"title":"회의","detailed_summary":"- 자료 공유"}""",
            payload.encoded(),
        )
    }

    @Test
    fun `trims title line and body region edges`() {
        val payload = success("\n  회의  \n\n  - 자료 공유\n\n  - 다음 줄\n\n")
        assertEquals("회의", payload.title)
        assertEquals("- 자료 공유\n\n  - 다음 줄", payload.detailedSummary)
        assertEquals(emptyList<AnalysisEntity>(), payload.time)
    }

    @Test
    fun `keeps markdown body`() {
        val payload = success(
            """
            제목
            ## 표
            | a | b |
            ---
            더 본문
            """.trimIndent(),
        )
        assertEquals("제목", payload.title)
        assertEquals("## 표\n| a | b |\n---\n더 본문", payload.detailedSummary)
        assertEquals(emptyList<AnalysisEntity>(), payload.time)
    }

    @Test
    fun `succeeds when body is empty`() {
        assertEquals(AnalysisPayload("제목", ""), success("제목"))
        assertEquals(AnalysisPayload("제목", ""), success("\n제목\n"))
    }

    @Test
    fun `classifies line format parse failures`() {
        assertFailure("", AnalysisPayload.ParseFailure.EMPTY)
        assertFailure("\n\n", AnalysisPayload.ParseFailure.EMPTY)
        assertFailure("   ", AnalysisPayload.ParseFailure.MISSING_TITLE)
        assertFailure("  \n", AnalysisPayload.ParseFailure.MISSING_TITLE)
    }

    @Test
    fun `encodes special characters`() {
        val encoded = AnalysisPayload("따옴표 \" 제목", "줄\n바꿈").encoded()
        assertEquals(AnalysisPayload("따옴표 \" 제목", "줄\n바꿈"), AnalysisPayload.parse(encoded))
    }

    @Test
    fun `keeps entity-like lines in the body`() {
        val payload = success(
            """
            회의
            - 자료 공유
            ---
            time(마감 시간): 내일 오후 6시
            period(행사 기간): 9월 14일~16일
            location(행사장): 강남역 2번 출구
            account(입금 계좌): 123-456-789012
            phone(전화번호): 010-1234-5678
            phone(대표번호): 02-123-4567
            """.trimIndent(),
        )
        assertEquals(
            "- 자료 공유\n---\n" +
                    "time(마감 시간): 내일 오후 6시\n" +
                    "period(행사 기간): 9월 14일~16일\n" +
                    "location(행사장): 강남역 2번 출구\n" +
                    "account(입금 계좌): 123-456-789012\n" +
                    "phone(전화번호): 010-1234-5678\n" +
                    "phone(대표번호): 02-123-4567",
            payload.detailedSummary,
        )
        assertEquals(emptyList<AnalysisEntity>(), payload.time)
        assertEquals(emptyList<AnalysisEntity>(), payload.period)
        assertEquals(emptyList<AnalysisEntity>(), payload.location)
        assertEquals(emptyList<AnalysisEntity>(), payload.account)
        assertEquals(emptyList<AnalysisEntity>(), payload.phone)
    }

    @Test
    fun `parses stored json entity arrays and omits empty kinds`() {
        val payload = AnalysisPayload.parse(
            """{"title":"제목","detailed_summary":"본문","time":[{"name":"마감 시간","value":"내일 오후 6시"}],"phone":[{"name":"전화번호","value":"010-1234-5678"}]}""",
        )
        assertEquals(
            AnalysisPayload(
                "제목",
                "본문",
                time = listOf(AnalysisEntity("마감 시간", "내일 오후 6시")),
                phone = listOf(AnalysisEntity("전화번호", "010-1234-5678")),
            ),
            payload,
        )
        assertEquals(
            """{"title":"제목","detailed_summary":"본문"""" +
                    ""","time":[{"name":"마감 시간","value":"내일 오후 6시"}]""" +
                    ""","phone":[{"name":"전화번호","value":"010-1234-5678"}]}""",
            payload?.encoded(),
        )
    }

    @Test
    fun `treats missing or malformed stored entity arrays as empty`() {
        val missing = AnalysisPayload.parse("""{"title":"제목","detailed_summary":"본문"}""")
        assertEquals(emptyList<AnalysisEntity>(), missing?.time)

        val malformed = AnalysisPayload.parse(
            """{"title":"제목","detailed_summary":"본문","time":"내일","phone":[{"name":"","value":"010"},{"value":"02"}]}""",
        )
        assertEquals(emptyList<AnalysisEntity>(), malformed?.time)
        assertEquals(emptyList<AnalysisEntity>(), malformed?.phone)
    }

    @Test
    fun `roundtrips encoded entities through stored json parse`() {
        val original = AnalysisPayload(
            "제목",
            "본문",
            location = listOf(AnalysisEntity("행사장", "강남역")),
        )
        assertEquals(original, AnalysisPayload.parse(original.encoded()))
    }

    @Test
    fun `keeps a bare entity line in the body`() {
        val payload = success(
            """
            제목
            본문
            time: 내일 오후 6시
            """.trimIndent(),
        )
        assertEquals("본문\ntime: 내일 오후 6시", payload.detailedSummary)
        assertEquals(emptyList<AnalysisEntity>(), payload.time)
    }

    @Test
    fun `does not recover bare entity lines from stored detailed summary`() {
        val payload = AnalysisPayload.parse(
            """{"title":"영수증 정보","summary":"SRT 기차표","detailed_summary":"time: 18:40\nphone: 1800-1472(고객센터)\n---\n영수금액: 47,400원"}""",
        )
        assertEquals(
            "time: 18:40\nphone: 1800-1472(고객센터)\n---\n영수금액: 47,400원",
            payload?.detailedSummary,
        )
        assertEquals(emptyList<AnalysisEntity>(), payload?.time)
        assertEquals(emptyList<AnalysisEntity>(), payload?.phone)
    }

    @Test
    fun `parses special lines on the first colon`() {
        assertEquals(
            listOf(
                AnalysisEntity("납부기한", "12월 5일"),
                AnalysisEntity("시간", "12:30"),
                AnalysisEntity("Name", "유지"),
            ),
            AnalysisPayload.parseSpecial(
                """

                납부기한: 12월 5일
                메모
                시간: 12:30
                : 값
                이름:
                name: 납부기한
                value: 12월 5일
                Name: 유지
                NULL
                """.trimIndent(),
            ),
        )
        assertEquals(emptyList<AnalysisEntity>(), AnalysisPayload.parseSpecial("NULL"))
        assertEquals(emptyList<AnalysisEntity>(), AnalysisPayload.parseSpecial("\n  NULL  \n"))
        assertEquals(emptyList<AnalysisEntity>(), AnalysisPayload.parseSpecial(""))
    }

    @Test
    fun `roundtrips a stored entities array`() {
        val payload = AnalysisPayload.parse(
            """{"title":"제목","detailed_summary":"본문","entities":[{"name":"납부기한","value":"12월 5일"}]}""",
        )
        assertEquals(listOf(AnalysisEntity("납부기한", "12월 5일")), payload?.entities)
        assertEquals(
            """{"title":"제목","detailed_summary":"본문","entities":[{"name":"납부기한","value":"12월 5일"}]}""",
            payload?.encoded(),
        )
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
