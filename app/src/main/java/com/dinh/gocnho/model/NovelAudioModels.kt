package com.dinh.gocnho.model

// ──────────────────────────────────────────────────────────────────────────────
//  NOVEL AUDIO FEATURE – DATA MODELS (Bước 1)
//  Toàn bộ Types/Interfaces/Enums phục vụ tính năng chuyển truyện chữ → audio
// ──────────────────────────────────────────────────────────────────────────────

// ── 1. Trạng thái tải về (download) của một chương audio ──────────────────────
enum class DownloadStatus {
    /** Chưa bắt đầu tải */
    IDLE,
    /** Đang trong quá trình tải */
    DOWNLOADING,
    /** Tải thành công, file đã sẵn sàng offline */
    DOWNLOADED,
    /** Tải thất bại */
    FAILED
}

// ── 2. Trạng thái đọc thủ công của một chương ───────────────────────────────
enum class ReadStatus {
    /** Chưa đọc */
    UNREAD,
    /** Đang đọc (đã mở nhưng chưa cuộn đến cuối) */
    READING,
    /** Đã đọc xong (cuộn đến cuối chương) */
    READ
}

// ── 3. Trạng thái nghe của một chương ────────────────────────────────────────
enum class ListenStatus {
    /** Chưa nghe */
    UNLISTENED,
    /** Đang nghe (đã mở nhưng chưa xong) */
    LISTENING,
    /** Đã nghe xong (≥ 90% thời lượng hoặc đến cuối) */
    LISTENED
}

// ── 4. Trạng thái của một Gemini API Key ─────────────────────────────────────
enum class ApiKeyStatus {
    /** Key sẵn sàng sử dụng */
    ACTIVE,
    /** Key đã vượt giới hạn quota (429 / Resource Exhausted) */
    QUOTA_EXCEEDED,
    /** Key sai định dạng hoặc bị thu hồi */
    INVALID
}

// ── 4. Novel – Thông tin tổng quan một bộ truyện ─────────────────────────────
data class Novel(
    val id: String = "",
    val title: String = "",
    val author: String? = null,
    val sourceUrl: String = "",
    val coverUrl: String? = null,
    val currentChapterId: String? = null,
    val totalChapters: Int = 0,
    val isFullTextDownloaded: Boolean = false,
    val downloadProgress: Int = 0
)

// ── 5. NovelChapter – Thông tin một chương truyện với trạng thái audio ────────
data class NovelChapter(
    val id: String = "",
    val novelId: String = "",
    val chapterIndex: Int = 0,
    val title: String = "",
    val sourceUrl: String = "",
    val rawContent: String? = null,
    val sanitizedContent: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.IDLE,
    val downloadProgress: Int = 0,
    /** Trạng thái đọc thủ công (Bước 3) */
    val readStatus: ReadStatus = ReadStatus.UNREAD,
    /** Vị trí cuộn scroll (0f..1f) khi đọc thủ công lần cuối (Bước 3) */
    val lastReadPosition: Float = 0f,
    val listenStatus: ListenStatus = ListenStatus.UNLISTENED,
    val lastListenedPosition: Float = 0f,
    val localAudioPath: String? = null,
    val audioFileSize: Long = -1L
)

// ── 6. ApiKeyInfo – Thông tin một Gemini API Key ─────────────────────────────
data class ApiKeyInfo(
    val id: String = "",
    val key: String = "",
    val label: String = "",
    val status: ApiKeyStatus = ApiKeyStatus.ACTIVE,
    val lastUsedAt: Long = 0L,
    val exceededAt: Long = 0L
)

// ── 7. AudioPlayerState – Trạng thái toàn cục của trình phát audio ───────────
data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentChapterId: String? = null,
    val currentChunkIndex: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int = 0,
    val remainingSleepTime: Int = 0,
    val isOfflineMode: Boolean = false,
    val activeApiKeyId: String? = null
)

// ── 8. ParserConfig – Cấu hình selector cho từng trang truyện ────────────────
data class ParserConfig(
    val domain: String = "",
    val titleSelector: String = "",
    val contentSelector: String = "",
    val nextChapterSelector: String = ""
)
