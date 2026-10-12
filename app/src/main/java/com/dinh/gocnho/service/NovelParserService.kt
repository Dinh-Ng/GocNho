package com.dinh.gocnho.service

import android.util.Log
import com.dinh.gocnho.model.ParserConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

// ──────────────────────────────────────────────────────────────────────────────
//  NovelParserService – Bước 2
//  Cào và lọc nội dung từ các trang truyện web phổ biến.
// ──────────────────────────────────────────────────────────────────────────────

private const val TAG = "NovelParserService"

/**
 * Kết quả parse một chương truyện từ URL.
 */
data class ParsedChapter(
    /** Tên bộ truyện (nếu trang có hiển thị) */
    val novelTitle: String,
    /** Tên chương */
    val chapterTitle: String,
    /** Nội dung thô (chưa làm sạch) */
    val rawContent: String,
    /** Nội dung đã làm sạch quảng cáo + chuẩn hóa */
    val sanitizedContent: String,
    /** URL chương tiếp theo (null nếu là chương cuối hoặc không tìm thấy) */
    val nextChapterUrl: String?
)

/**
 * Các lỗi có thể xảy ra khi parse.
 */
sealed class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String, cause: Throwable? = null) : ParseException(message, cause)
    class UnsupportedDomain(domain: String) : ParseException("Domain chưa được hỗ trợ: $domain")
    class ContentNotFound(selector: String) : ParseException("Không tìm thấy nội dung với selector: $selector")
    class EmptyContent : ParseException("Nội dung truyện trống sau khi lọc")
}

/**
 * NovelParserService – Service cào và lọc nội dung truyện web.
 *
 * Tất cả hàm đều là suspend function, chạy trên Dispatchers.IO.
 * Gọi từ ViewModel hoặc coroutine scope, không gọi trực tiếp trên Main thread.
 */
object NovelParserService {

    // ── Timeout mặc định ──────────────────────────────────────────────────────
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS    = 20_000

    // ── User-Agent để tránh bị site chặn ─────────────────────────────────────
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    // =========================================================================
    //  PARSER CONFIGS – Cấu hình selector cho từng domain
    //  Thêm domain mới ở đây mà không cần sửa logic.
    // =========================================================================
    private val SUPPORTED_CONFIGS: List<ParserConfig> = listOf(
        // truyenfull.live – domain chính (truyenfull.vn & .io đã đóng cửa)
        ParserConfig(
            domain              = "truyenfull.live",
            titleSelector       = "h2.chapter-title, .chapter-title",
            contentSelector     = "#chapter-c, div.chapter-c",
            nextChapterSelector = "a#next_chap, a.next_chap, a[title='Chương sau']"
        ),
        // Alias dự phòng phòng trường hợp redirect cũ
        ParserConfig(
            domain              = "truyenfull.io",
            titleSelector       = "h2.chapter-title, .chapter-title",
            contentSelector     = "#chapter-c, div.chapter-c",
            nextChapterSelector = "a#next_chap, a.next_chap, a[title='Chương sau']"
        ),
        ParserConfig(
            domain              = "metruyencv.com",
            titleSelector       = ".chapter-title, h1.h3",
            contentSelector     = "div.chapter-content",
            nextChapterSelector = "a#next-chapter, a.btn-next-chap"
        )
    )

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Parse một chương truyện từ URL.
     *
     * @param url URL trang chương truyện (http/https)
     * @return [ParsedChapter] với đầy đủ thông tin nếu thành công
     * @throws [ParseException] nếu có lỗi mạng, domain không hỗ trợ hoặc không tìm thấy nội dung
     */
    suspend fun parseChapter(url: String): ParsedChapter = withContext(Dispatchers.IO) {
        Log.d(TAG, "Bắt đầu parse URL: $url")

        // 1. Nhận diện domain và lấy config
        val config = resolveConfig(url)
            ?: throw ParseException.UnsupportedDomain(extractDomain(url))

        Log.d(TAG, "Dùng config cho domain: ${config.domain}")

        // 2. Tải HTML
        val doc = fetchDocument(url)

        // 3. Trích xuất các phần tử
        val novelTitle = extractNovelTitle(doc)
        val chapterTitle = extractText(doc, config.titleSelector)
            .ifBlank { extractFallbackChapterTitle(doc) }
        val rawContent = extractText(doc, config.contentSelector)

        if (rawContent.isBlank()) {
            Log.w(TAG, "Không tìm thấy nội dung với selector: ${config.contentSelector}")
            throw ParseException.ContentNotFound(config.contentSelector)
        }

        val sanitizedContent = sanitizeText(rawContent)
        if (sanitizedContent.isBlank()) {
            throw ParseException.EmptyContent()
        }

        val nextUrl = extractNextChapterUrl(doc, config.nextChapterSelector, url)

        Log.d(TAG, "Parse thành công – chương: '$chapterTitle', ${sanitizedContent.length} ký tự, next=$nextUrl")

        ParsedChapter(
            novelTitle       = novelTitle,
            chapterTitle     = chapterTitle,
            rawContent       = rawContent,
            sanitizedContent = sanitizedContent,
            nextChapterUrl   = nextUrl
        )
    }

    /**
     * Kiểm tra xem một domain có được hỗ trợ hay không.
     */
    fun isDomainSupported(url: String): Boolean = resolveConfig(url) != null

    /**
     * Trả về danh sách các domain được hỗ trợ.
     */
    fun getSupportedDomains(): List<String> = SUPPORTED_CONFIGS.map { it.domain }

    // =========================================================================
    //  SANITIZE TEXT – Làm sạch nội dung & Chuẩn hoá cho AI TTS (Bước 2)
    // =========================================================================

    /**
     * Lọc bỏ quảng cáo, link giới thiệu và chuẩn hóa văn bản cho AI TTS.
     *
     * Nhiệm vụ:
     * 1. Lọc bỏ các đoạn văn bản rác (quảng cáo chèn giữa trang, watermark, link giới thiệu).
     * 2. Chuẩn hóa các từ viết tắt, hán việt, teencode, ký tự đặc biệt để phục vụ cho bộ đọc AI.
     * 3. Giữ nguyên câu thoại đối thoại ngắn của nhân vật (ví dụ "Ừ.", "Hả?", "- Đi!").
     * 4. Chuẩn hóa ngắt câu, dấu ngoặc, khoảng trắng và dòng trống.
     */
    fun sanitizeText(rawText: String): String {
        if (rawText.isBlank()) return ""

        // Bước 1: Xử lý thẻ HTML sót lại và mã hóa HTML Entities
        var text = cleanHtmlAndEntities(rawText)

        // Bước 2: Lọc từng dòng – loại bỏ dòng quảng cáo, watermark, dòng rác vô nghĩa
        text = filterAdAndGarbageLines(text)

        // Bước 3: Xoá quảng cáo nội tuyến (links sót lại, domain watermark chèn lén giữa câu, metadata thừa)
        text = removeInlineAds(text)

        // Bước 4: Chuẩn hóa ký tự đặc biệt, dấu ngoặc, dấu câu cho AI TTS
        text = normalizePunctuationAndSymbols(text)

        // Bước 5: Chuẩn hóa từ viết tắt, teencode phổ biến trong truyện mạng
        text = normalizeAbbreviations(text)

        // Bước 6: Chuẩn hóa khoảng trắng và dòng trống
        text = cleanWhitespaceAndParagraphs(text)

        return text
    }

    private fun cleanHtmlAndEntities(input: String): String {
        return input
            .replace(Regex("""(?i)<br\s*/?>"""), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", " và ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&hellip;", "...")
    }

    private val INLINE_AD_PATTERNS = listOf(
        // URL web thô
        Regex("""https?://\S+"""),
        Regex("""\bwww\.\S+""", RegexOption.IGNORE_CASE),
        // Domain các trang truyện thường bị chèn lén vào giữa câu
        Regex("""\b(truyenfull\.(vn|io)|bachngocsach\.(com|com\.vn)|metruyencv\.com|tangthuvien\.vn|truyen\.tangthuvien\.vn|dtruyen\.com|sstruyen\.com|truyenyy\.com|wattpad\.com)\b""", RegexOption.IGNORE_CASE),
        // Đoạn ghi chú rác trong ngoặc đơn hoặc ngoặc vuông
        Regex("""\((nguồn\s*:?[^\)]+|nhóm\s*dịch\s*:[^\)]+|chương\s*này\s*chưa\s*được\s*edit[^\)]*)\)""", RegexOption.IGNORE_CASE),
        Regex("""\[(nguồn\s*:?[^\]]+|nhóm\s*dịch\s*:[^\]]+|chương\s*này\s*chưa\s*được\s*edit[^\]]*)\]""", RegexOption.IGNORE_CASE),
        // Tiêu đề chap/chapter chèn giữa thân bài
        Regex("""(chap\s*\d+\s*-\s*|chapter\s*\d+\s*-\s*)""", RegexOption.IGNORE_CASE)
    )

    private fun removeInlineAds(input: String): String {
        var result = input
        for (pattern in INLINE_AD_PATTERNS) {
            result = result.replace(pattern, "")
        }
        return result
    }

    private val LINE_AD_PATTERNS = listOf(
        Regex("""(tải\s*app|cài\s*đặt\s*app|download\s*app)""", RegexOption.IGNORE_CASE),
        Regex("""(đọc\s*(truyện|tiếp|tại)?\s*tại|xem\s*tiếp\s*tại|đọc\s*bản\s*dịch\s*tại)""", RegexOption.IGNORE_CASE),
        Regex("""(chúc\s*(bạn|các\s*bạn)?\s*đọc\s*truyện\s*vui\s*vẻ)""", RegexOption.IGNORE_CASE),
        Regex("""(ủng\s*hộ\s*(dịch\s*giả|converter|nhóm\s*dịch)|donate|momo\s*:|stk\s*:|ngân\s*hàng\s*:)""", RegexOption.IGNORE_CASE),
        Regex("""(tham\s*gia\s*nhóm|group\s*facebook|fanpage\s*:|kênh\s*discord)""", RegexOption.IGNORE_CASE),
        Regex("""(like\s*và\s*share|đánh\s*giá\s*5\s*sao|bình\s*chọn\s*cho\s*truyện|bỏ\s*phiếu|tặng\s*hoa|nhấn\s*theo\s*dõi)""", RegexOption.IGNORE_CASE),
        Regex("""(truyện\s*được\s*(đăng|convert|dịch|lấy|chia\s*sẻ)\s*(chính\s*thức\s*)?tại)""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(converter|editor|dịch\s*giả|trans|nhóm\s*dịch|nguồn)\s*:""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(chap|chapter)\s*\d+\s*[-:]?\s*$""", RegexOption.IGNORE_CASE),
        // Dòng link web
        Regex("""^\s*https?://\S+\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*www\.\S+\s*$""", RegexOption.IGNORE_CASE),
        // Dòng domain truyện đứng riêng
        Regex("""^\s*(truyenfull\.(vn|io)|bachngocsach\.(com|com\.vn)|tangthuvien\.vn|metruyencv\.com)\s*$""", RegexOption.IGNORE_CASE),
        // Dòng phân cách / decor rác: chỉ gồm ký tự như ---, ***, ===, ___, ~~~, etc.
        Regex("""^[\s\-_=*~•·★☆◆◇●○#]{3,}$""")
    )

    private fun filterAdAndGarbageLines(input: String): String {
        return input.lines()
            .map { it.trim() }
            .filter { line ->
                if (line.isEmpty()) return@filter true // Giữ lại dòng trống để xử lý đoạn văn sau

                // Kiểm tra xem dòng có khớp với pattern quảng cáo không
                val isAd = LINE_AD_PATTERNS.any { pattern -> pattern.containsMatchIn(line) }
                if (isAd) return@filter false

                // Nếu dòng không chứa bất kỳ chữ cái nào (chỉ toàn số/dấu rác sót lại) -> loại bỏ
                // Lưu ý: dòng đối thoại ngắn như "Ừ.", "Hả?", "Đi!", "- Không." CÓ chứa chữ cái nên được giữ lại!
                val hasLetter = line.any { it.isLetter() }
                if (!hasLetter) return@filter false

                // Loại các dòng là các từ quảng cáo ngắn gọn lẻ tẻ
                val lower = line.lowercase()
                if (lower in setOf("ad", "ads", "qc", "pr", "vip")) return@filter false

                true
            }
            .joinToString("\n")
    }

    private fun normalizePunctuationAndSymbols(input: String): String {
        var text = input

        // 1. Chuẩn hóa dấu ngoặc kép / trích dẫn thoại
        text = text
            .replace('“', '"')
            .replace('”', '"')
            .replace('„', '"')
            .replace('‟', '"')
            .replace('«', '"')
            .replace('»', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('‚', '\'')
            .replace('‛', '\'')

        // 2. Chuẩn hóa dấu gạch ngang (em-dash, en-dash, horizontal bar)
        text = text
            .replace('—', '-')
            .replace('–', '-')
            .replace('―', '-')

        // 3. Chuẩn hóa dấu ba chấm (ellipsis)
        text = text.replace("…", "...")
        text = text.replace(Regex("""\.{4,}"""), "...")

        // 4. Chuẩn hóa dấu chấm than, chấm hỏi liên tiếp để AI TTS đọc tự nhiên
        text = text.replace(Regex("""\?{2,}"""), "?")
        text = text.replace(Regex("""!{2,}"""), "!")
        text = text.replace(Regex("""\?!+"""), "?!")
        text = text.replace(Regex("""!\?+"""), "!?")

        // 5. Xóa ký tự định dạng markdown hoặc trang trí web
        text = text
            .replace("**", "")
            .replace("__", "")
            .replace("~~", "")
            .replace(Regex("""[◆◇●○★☆▲▼■□✿❀✦✧]"""), "")

        // 6. Đổi một số ký hiệu thành chữ để bộ đọc AI phát âm chuẩn tiếng Việt
        text = text.replace(Regex("""(?<=\d)\s*%\b|%"""), " phần trăm")
        text = text.replace(Regex("""(?<=\d)\s*°C\b"""), " độ C")
        text = text.replace(Regex("""\b&\b"""), " và ")
        text = text.replace(Regex("""(?<=\d)\s*\+\s*(?=\d)"""), " cộng ")
        text = text.replace(Regex("""\b@\b"""), " a còng ")

        // 7. Chuẩn hóa khoảng cách xung quanh dấu câu
        // Bỏ khoảng trắng trước dấu câu (ví dụ: "nói ." -> "nói.")
        text = text.replace(Regex("""\s+([.,;:!?])"""), "$1")
        // Thêm khoảng cách sau dấu câu nếu dính liền với từ tiếp theo (ví dụ: "nói.Sau đó" -> "nói. Sau đó")
        text = text.replace(Regex("""([.,;:!?])(\p{L})"""), "$1 $2")

        return text
    }

    private fun wordPattern(word: String, ignoreCase: Boolean = false): Regex {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        return Regex("""(?<=^|[^\p{L}\p{N}])${Regex.escape(word)}(?=[^\p{L}\p{N}]|$)""", options)
    }

    private val ABBREVIATIONS = listOf(
        wordPattern("vs", true) to "với",
        wordPattern("ko", true) to "không",
        wordPattern("K") to "Không",
        wordPattern("k") to "không",
        wordPattern("Đc") to "Được",
        wordPattern("đc") to "được",
        wordPattern("Dc") to "Được",
        wordPattern("dc") to "được",
        wordPattern("bít", true) to "biết",
        wordPattern("Ng") to "Người",
        wordPattern("ng") to "người",
        wordPattern("trc", true) to "trước",
        wordPattern("wa", true) to "quá",
        wordPattern("j", true) to "gì",
        wordPattern("ntn", true) to "như thế nào",
        wordPattern("h", true) to "giờ",
        wordPattern("r", true) to "rồi",
        wordPattern("thui", true) to "thôi",
        wordPattern("hok", true) to "không",
        wordPattern("hổng", true) to "không",
        wordPattern("oh", true) to "ồ",
        wordPattern("ah", true) to "à",
    )

    private fun normalizeAbbreviations(input: String): String {
        var text = input
        for ((regex, replacement) in ABBREVIATIONS) {
            text = text.replace(regex, replacement)
        }
        return text
    }

    private fun cleanWhitespaceAndParagraphs(input: String): String {
        val lines = input.lines()
            .map { it.replace(Regex("""[ \t]+"""), " ").trim() }

        val cleanText = lines.joinToString("\n")
        // Thu gọn các dòng trống liên tiếp (chỉ giữ tối đa 1 dòng trống giữa 2 đoạn)
        return cleanText
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    // =========================================================================
    //  PRIVATE HELPERS
    // =========================================================================

    private suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        try {
            Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(CONNECT_TIMEOUT_MS)
                .maxBodySize(5 * 1024 * 1024) // Giới hạn 5MB để tránh nội dung quá lớn
                .get()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi mạng khi tải URL: $url", e)
            throw ParseException.NetworkError("Không thể tải trang: ${e.localizedMessage}", e)
        }
    }

    /** Tìm config phù hợp dựa trên domain của URL */
    private fun resolveConfig(url: String): ParserConfig? {
        val domain = extractDomain(url)
        return SUPPORTED_CONFIGS.find { config ->
            domain.contains(config.domain, ignoreCase = true)
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Trích xuất text từ selector CSS, thử lần lượt các selector cách nhau bằng dấu phẩy.
     * Trả về text của selector đầu tiên có nội dung.
     */
    private fun extractText(doc: Document, selectorCsv: String): String {
        val selectors = selectorCsv.split(",").map { it.trim() }
        for (sel in selectors) {
            val el = doc.selectFirst(sel) ?: continue
            val text = el.text().trim()
            if (text.isNotBlank()) return text
        }
        return ""
    }

    private fun extractNovelTitle(doc: Document): String {
        // Thử các selector phổ biến cho tên bộ truyện
        val selectors = listOf(
            "a.truyen-title", ".truyen-title",
            "h1.novel-title", ".novel-title",
            "a[itemprop=name]",
            "title"
        )
        for (sel in selectors) {
            val el = doc.selectFirst(sel) ?: continue
            val text = el.text().trim()
            if (text.isNotBlank()) return text
        }
        // Fallback: dùng <title> tag của trang
        return doc.title()
            .split("|", "-").firstOrNull()?.trim() ?: "Không rõ tên truyện"
    }

    private fun extractFallbackChapterTitle(doc: Document): String {
        // Thử thêm các selector phổ biến
        val selectors = listOf("h1", "h2", ".chapter-name", "#chapter-big-container h2")
        for (sel in selectors) {
            val el = doc.selectFirst(sel) ?: continue
            val text = el.text().trim()
            if (text.isNotBlank()) return text
        }
        return "Chương không tên"
    }

    /**
     * Lấy URL chương tiếp theo.
     * Xử lý cả URL tương đối và tuyệt đối.
     */
    private fun extractNextChapterUrl(doc: Document, selectorCsv: String, baseUrl: String): String? {
        val selectors = selectorCsv.split(",").map { it.trim() }
        for (sel in selectors) {
            val el = doc.selectFirst(sel) ?: continue
            val href = el.attr("abs:href").ifBlank { el.attr("href") }.trim()
            if (href.isBlank() || href == "#" || href.startsWith("javascript:")) continue

            // Kiểm tra nút không bị disabled (truyenfull dùng class disabled)
            if (el.hasClass("disabled") || el.attr("disabled").isNotBlank()) continue

            // Chuyển URL tương đối thành tuyệt đối nếu cần
            return if (href.startsWith("http")) href
            else resolveRelativeUrl(baseUrl, href)
        }
        return null
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = java.net.URL(baseUrl)
            java.net.URL(base, relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
