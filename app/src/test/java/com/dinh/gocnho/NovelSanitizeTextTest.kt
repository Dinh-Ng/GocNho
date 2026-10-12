package com.dinh.gocnho

import com.dinh.gocnho.service.NovelParserService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test cho hàm NovelParserService.sanitizeText (Bước 2 trong Roadmap)
 * Kiểm thử tính năng TC-MOD2-03 (Làm sạch văn bản) & TC-MOD3-06 (Chuẩn hóa cho AI TTS)
 */
class NovelSanitizeTextTest {

    @Test
    fun testRemoveAdLinesAndWatermarks() {
        val raw = """
            Chương 1: Khởi đầu
            Tải app TruyenFull để đọc tiếp chương mới nhất!
            Hắn bước vào sơn cốc, nhìn thấy một bóng người.
            Đọc tiếp tại truyenfull.vn
            Nguồn: tangthuvien.vn
            Chúc các bạn đọc truyện vui vẻ!
            Donate cho nhóm dịch qua ví Momo: 0912345678
            Gió thổi nhè nhẹ qua từng tán cây.
            -------------------
            https://truyenfull.vn/chuong-1
        """.trimIndent()

        val sanitized = NovelParserService.sanitizeText(raw)

        assertFalse(sanitized.contains("Tải app TruyenFull"))
        assertFalse(sanitized.contains("Đọc tiếp tại"))
        assertFalse(sanitized.contains("Nguồn: tangthuvien.vn"))
        assertFalse(sanitized.contains("Chúc các bạn đọc truyện vui vẻ"))
        assertFalse(sanitized.contains("Donate cho nhóm dịch"))
        assertFalse(sanitized.contains("https://truyenfull.vn"))
        assertFalse(sanitized.contains("-------------------"))

        assertTrue(sanitized.contains("Hắn bước vào sơn cốc, nhìn thấy một bóng người."))
        assertTrue(sanitized.contains("Gió thổi nhè nhẹ qua từng tán cây."))
    }

    @Test
    fun testPreserveShortDialogueLines() {
        // Kiểm tra giữ lại các câu thoại ngắn của nhân vật (không bị xóa nhầm)
        val raw = """
            Hắn nhìn nàng hỏi:
            - Nàng có đi không?
            - Ừ.
            - Đi!
            Hả?
            A!
            Được.
        """.trimIndent()

        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue("Phải giữ lại thoại 'Ừ.'", sanitized.contains("Ừ."))
        assertTrue("Phải giữ lại thoại 'Đi!'", sanitized.contains("Đi!"))
        assertTrue("Phải giữ lại thoại 'Hả?'", sanitized.contains("Hả?"))
        assertTrue("Phải giữ lại thoại 'A!'", sanitized.contains("A!"))
        assertTrue("Phải giữ lại thoại 'Được.'", sanitized.contains("Được."))
    }

    @Test
    fun testNormalizeQuotesAndDashesForTTS() {
        val raw = """
            “Ngươi là ai?” — Hắn quát lớn.
            ‘Không phải ta’… nàng đáp.
            Cái gì????? Thật sao!!!!!
        """.trimIndent()

        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue(sanitized.contains("\"Ngươi là ai?\""))
        assertTrue(sanitized.contains("- Hắn quát lớn."))
        assertTrue(sanitized.contains("'Không phải ta'..."))
        assertTrue(sanitized.contains("Cái gì?"))
        assertTrue(sanitized.contains("Thật sao!"))
    }

    @Test
    fun testNormalizeAbbreviationsAndTeencode() {
        val raw = "Hắn đi vs nàng, k bít phải làm j bây h. Đc r thui về trc đi."
        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue(sanitized.contains("với"))
        assertTrue(sanitized.contains("không biết"))
        assertTrue(sanitized.contains("làm gì"))
        assertTrue(sanitized.contains("bây giờ"))
        assertTrue(sanitized.contains("Được rồi"))
        assertTrue(sanitized.contains("thôi về trước"))
    }

    @Test
    fun testNormalizeSymbolsForTTS() {
        val raw = "Nhiệt độ hôm nay là 38°C, xác suất mưa là 50%."
        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue(sanitized.contains("38 độ C"))
        assertTrue(sanitized.contains("50 phần trăm"))
    }

    @Test
    fun testPunctuationSpacingAndWhitespace() {
        val raw = "Hắn nói .Sau đó quay lưng đi   .\n\n\n\n\nNgày hôm sau trời mưa."
        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue(sanitized.contains("Hắn nói. Sau đó quay lưng đi."))
        assertFalse(sanitized.contains("\n\n\n"))
    }

    @Test
    fun testHtmlLeftoversAndEntities() {
        val raw = "Ta &amp; nàng&nbsp;cùng đi.<br>Trời đã tối."
        val sanitized = NovelParserService.sanitizeText(raw)

        assertTrue(sanitized.contains("Ta và nàng cùng đi."))
        assertTrue(sanitized.contains("Trời đã tối."))
    }
}
