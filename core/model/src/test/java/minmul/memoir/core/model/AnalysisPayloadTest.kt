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
            title: 회의
            ---
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
    fun `ignores leftover summary line before delimiter`() {
        val leftover = success(
            """
            title: 회의
            summary: 내일 10시
            ---
            - 자료 공유
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("회의", "- 자료 공유"), leftover)
        assertEquals(
            """{"title":"회의","detailed_summary":"- 자료 공유"}""",
            leftover.encoded(),
        )

        val blank = success(
            """
            title: 제목
            summary:
            ---
            본문
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", "본문"), blank)

        val whitespace = success(
            """
            title: 제목
            summary:   
            ---
            본문
            """.trimIndent(),
        )
        assertEquals(AnalysisPayload("제목", "본문"), whitespace)
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
        assertEquals("## 표\n| a | b |", payload.detailedSummary)
    }

    @Test
    fun `succeeds when body is empty`() {
        assertEquals(
            AnalysisPayload("제목", ""),
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
        assertFailure(
            """
            title: 영수증 정보
            ---
            time: 18:40
            ---
            영수금액: 47,400원
            """.trimIndent(),
            AnalysisPayload.ParseFailure.MISSING_ENTITY_NAME,
        )
    }

    @Test
    fun `encodes special characters`() {
        val encoded = AnalysisPayload("따옴표 \" 제목", "줄\n바꿈").encoded()
        assertEquals(AnalysisPayload("따옴표 \" 제목", "줄\n바꿈"), AnalysisPayload.parse(encoded))
    }

    @Test
    fun `parses entity trailer after last delimiter`() {
        val payload = success(
            """
            title: 회의
            ---
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
        assertEquals("- 자료 공유", payload.detailedSummary)
        assertEquals(listOf(AnalysisEntity("마감 시간", "내일 오후 6시")), payload.time)
        assertEquals(listOf(AnalysisEntity("행사 기간", "9월 14일~16일")), payload.period)
        assertEquals(listOf(AnalysisEntity("행사장", "강남역 2번 출구")), payload.location)
        assertEquals(listOf(AnalysisEntity("입금 계좌", "123-456-789012")), payload.account)
        assertEquals(
            listOf(
                AnalysisEntity("전화번호", "010-1234-5678"),
                AnalysisEntity("대표번호", "02-123-4567"),
            ),
            payload.phone,
        )
        assertEquals(
            """{"title":"회의","detailed_summary":"- 자료 공유"""" +
                    ""","time":[{"name":"마감 시간","value":"내일 오후 6시"}]""" +
                    ""","period":[{"name":"행사 기간","value":"9월 14일~16일"}]""" +
                    ""","location":[{"name":"행사장","value":"강남역 2번 출구"}]""" +
                    ""","account":[{"name":"입금 계좌","value":"123-456-789012"}]""" +
                    ""","phone":[{"name":"전화번호","value":"010-1234-5678"},{"name":"대표번호","value":"02-123-4567"}]}""",
            payload.encoded(),
        )
    }

    @Test
    fun `keeps markdown delimiter in body when trailer has no allowed keys`() {
        val payload = success(
            """
            title: 제목
            ---
            ## 표
            ---
            더 본문
            """.trimIndent(),
        )
        assertEquals("## 표\n---\n더 본문", payload.detailedSummary)
        assertEquals(emptyList<AnalysisEntity>(), payload.time)
        assertEquals(emptyList<AnalysisEntity>(), payload.phone)
    }

    @Test
    fun `ignores unknown keys empty names and blank values in trailer`() {
        val payload = success(
            """
            title: 제목
            ---
            본문
            ---
            url(링크): https://example.com
            time(): 내일
            phone(대표):
            TIME(마감 시간): 내일 오후 6시
            """.trimIndent(),
        )
        assertEquals("본문", payload.detailedSummary)
        assertEquals(listOf(AnalysisEntity("마감 시간", "내일 오후 6시")), payload.time)
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
    fun `fails when entity key omits name`() {
        assertFailure(
            """
            title: 제목
            ---
            본문
            ---
            time: 내일 오후 6시
            """.trimIndent(),
            AnalysisPayload.ParseFailure.MISSING_ENTITY_NAME,
        )
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

    private fun success(raw: String): AnalysisPayload =
        (AnalysisPayload.parseResult(raw) as AnalysisPayload.ParseResult.Success).payload

    private fun assertFailure(raw: String, reason: AnalysisPayload.ParseFailure) {
        val result = AnalysisPayload.parseResult(raw)
        val failure = result as AnalysisPayload.ParseResult.Failure
        assertEquals(reason, failure.reason)
        assertEquals(raw.length, failure.payloadChars)
    }
}
