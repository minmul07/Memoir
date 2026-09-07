package minmul.memoir.background.analysis

import java.time.Instant
import kotlin.random.Random
import minmul.memoir.core.model.QueueItem
import org.json.JSONArray
import org.json.JSONObject

class RandomFakeAnalysis(
    private val shouldFail: () -> Boolean = { Random.nextInt(3) == 0 },
) : FakeAnalysis {
    override suspend fun analyze(item: QueueItem, text: String?): String {
        if (shouldFail()) throw IllegalStateException("fake_analysis_failed")
        val now = Instant.now().toString()
        return JSONObject().put("필수_항목", JSONObject()
            .put("고유_아이디", item.itemId)
            .put("분석_시작_시간", now)
            .put("분석_완료_시간", now)
            .put("형태", "이미지"))
            .put("분석_내용", JSONArray().put(JSONObject()
                .put("label", "Fake 분석")
                .put("value", text ?: "인식된 텍스트 없음")))
            .toString(2)
    }
}
