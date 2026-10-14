package com.dinh.gocnho.service

import android.content.Context
import android.util.Log
import com.dinh.gocnho.data.NovelAudioStorage
import com.dinh.gocnho.model.DownloadStatus
import com.dinh.gocnho.model.Novel
import com.dinh.gocnho.model.NovelChapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

// ──────────────────────────────────────────────────────────────────────────────
//  BatchDownloadService – Bước 3
//  Tải toàn bộ nội dung chữ của một bộ truyện theo chuỗi nextChapterUrl.
//  Lưu từng chương vào Local DB (SharedPreferences qua NovelAudioStorage).
//  Hỗ trợ Pause / Resume / Cancel và cung cấp state realtime qua StateFlow.
// ──────────────────────────────────────────────────────────────────────────────

private const val TAG = "BatchDownloadService"

/**
 * Trạng thái tổng quan của tiến trình tải.
 */
sealed class BatchDownloadState {
    /** Chưa bắt đầu / đã reset */
    object Idle : BatchDownloadState()

    /** Đang tải */
    data class Downloading(
        val novelTitle: String,
        val novelId: String,
        val downloadedCount: Int,
        val estimatedTotal: Int,
        val currentChapterTitle: String,
        val progressPercent: Int
    ) : BatchDownloadState()

    /** Đang tạm dừng (sẽ dừng sau khi chương hiện tại xong) */
    data class Paused(
        val novelTitle: String,
        val novelId: String,
        val downloadedCount: Int,
        val estimatedTotal: Int
    ) : BatchDownloadState()

    /** Hoàn thành */
    data class Completed(
        val novelTitle: String,
        val novelId: String,
        val totalDownloaded: Int
    ) : BatchDownloadState()

    /** Thất bại */
    data class Failed(
        val novelTitle: String,
        val novelId: String,
        val downloadedCount: Int,
        val reason: String
    ) : BatchDownloadState()

    /** Đã bị hủy bởi người dùng */
    data class Cancelled(
        val novelTitle: String,
        val novelId: String,
        val downloadedCount: Int
    ) : BatchDownloadState()
}

/**
 * BatchDownloadService – object singleton quản lý job tải hàng loạt.
 *
 * Một thời điểm chỉ chạy 1 job tải. Nếu muốn tải truyện mới phải
 * cancel job hiện tại trước.
 */
object BatchDownloadService {

    // Delay giữa mỗi chương để tránh bị site chặn (ms)
    private const val CHAPTER_DELAY_MS = 800L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<BatchDownloadState>(BatchDownloadState.Idle)
    val state: StateFlow<BatchDownloadState> = _state.asStateFlow()

    // Job đang chạy (nếu có)
    private var downloadJob: Job? = null

    // Flag để tạm dừng (Pause)
    @Volatile
    private var isPaused = false

    // Biến lưu trạng thái cần thiết để Resume
    private var resumeNovelId: String? = null
    private var resumeNextUrl: String? = null
    private var resumeDownloadedCount: Int = 0
    private var resumeNovelTitle: String = ""

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Bắt đầu tải toàn bộ truyện từ [startChapterUrl].
     *
     * @param context Android Context để lưu vào storage
     * @param startChapterUrl URL chương đầu tiên
     * @param existingNovelId Nếu truyện đã tồn tại trong DB, truyền ID để upsert;
     *                        nếu null thì tạo Novel mới
     */
    fun startDownload(
        context: Context,
        startChapterUrl: String,
        existingNovelId: String? = null
    ) {
        if (downloadJob?.isActive == true) {
            Log.w(TAG, "Đang có job tải chạy. Cancel trước khi tải mới.")
            return
        }

        isPaused = false
        val novelId = existingNovelId ?: NovelAudioStorage.generateNovelId()

        downloadJob = serviceScope.launch {
            runDownloadLoop(context, startChapterUrl, novelId, startedDownloadedCount = 0)
        }
    }

    /**
     * Resume từ vị trí đã dừng.
     */
    fun resume(context: Context) {
        val nextUrl = resumeNextUrl ?: return
        val novelId = resumeNovelId ?: return
        if (downloadJob?.isActive == true) return

        isPaused = false
        val startedCount = resumeDownloadedCount

        downloadJob = serviceScope.launch {
            runDownloadLoop(context, nextUrl, novelId, startedDownloadedCount = startedCount)
        }
    }

    /**
     * Tạm dừng sau khi chương hiện tại tải xong.
     */
    fun pause() {
        isPaused = true
        Log.d(TAG, "Yêu cầu tạm dừng – sẽ dừng sau khi chương hiện tại xong")
    }

    /**
     * Hủy toàn bộ tiến trình tải.
     */
    fun cancel() {
        downloadJob?.cancel()
        downloadJob = null
        isPaused = false
        val current = _state.value
        val (title, id, count) = when (current) {
            is BatchDownloadState.Downloading -> Triple(current.novelTitle, current.novelId, current.downloadedCount)
            is BatchDownloadState.Paused      -> Triple(current.novelTitle, current.novelId, current.downloadedCount)
            else -> Triple("", "", 0)
        }
        _state.value = BatchDownloadState.Cancelled(title, id, count)
        resumeNextUrl = null
        resumeNovelId = null
        Log.d(TAG, "Đã hủy tiến trình tải")
    }

    /** Reset về Idle */
    fun reset() {
        cancel()
        _state.value = BatchDownloadState.Idle
    }

    // =========================================================================
    //  CORE LOOP
    // =========================================================================

    private suspend fun runDownloadLoop(
        context: Context,
        startUrl: String,
        novelId: String,
        startedDownloadedCount: Int
    ) {
        var currentUrl: String? = startUrl
        var downloadedCount = startedDownloadedCount
        var novelTitle = resumeNovelTitle.ifBlank { "Đang tải..." }

        try {
            while (currentUrl != null && currentCoroutineContext().isActive) {

                // ── Xử lý Pause ───────────────────────────────────────────
                if (isPaused) {
                    resumeNextUrl     = currentUrl
                    resumeNovelId     = novelId
                    resumeDownloadedCount = downloadedCount
                    resumeNovelTitle  = novelTitle
                    _state.value = BatchDownloadState.Paused(
                        novelTitle      = novelTitle,
                        novelId         = novelId,
                        downloadedCount = downloadedCount,
                        estimatedTotal  = downloadedCount + 1 // unknown
                    )
                    Log.d(TAG, "Đã tạm dừng tại URL: $currentUrl")
                    // Chờ resume được gọi (job sẽ bị cancel hoặc resume qua job mới)
                    return
                }

                // ── Cập nhật state đang tải ───────────────────────────────
                _state.value = BatchDownloadState.Downloading(
                    novelTitle          = novelTitle,
                    novelId             = novelId,
                    downloadedCount     = downloadedCount,
                    estimatedTotal      = downloadedCount + 1,
                    currentChapterTitle = "Đang tải chương ${downloadedCount + 1}...",
                    progressPercent     = 0
                )

                // ── Parse chương ──────────────────────────────────────────
                Log.d(TAG, "Bắt đầu tải chương ${downloadedCount + 1}: $currentUrl")
                val parsed = try {
                    NovelParserService.parseChapter(currentUrl)
                } catch (e: ParseException.UnsupportedDomain) {
                    _state.value = BatchDownloadState.Failed(
                        novelTitle      = novelTitle,
                        novelId         = novelId,
                        downloadedCount = downloadedCount,
                        reason          = "Domain không được hỗ trợ: ${e.message}"
                    )
                    return
                } catch (e: ParseException.NetworkError) {
                    _state.value = BatchDownloadState.Failed(
                        novelTitle      = novelTitle,
                        novelId         = novelId,
                        downloadedCount = downloadedCount,
                        reason          = "Lỗi mạng: ${e.message}"
                    )
                    return
                } catch (e: ParseException.ContentNotFound) {
                    // Thử bỏ qua chương này và coi như đã hết truyện
                    Log.w(TAG, "Không tìm thấy nội dung chương, kết thúc vòng lặp: ${e.message}")
                    break
                } catch (e: Exception) {
                    _state.value = BatchDownloadState.Failed(
                        novelTitle      = novelTitle,
                        novelId         = novelId,
                        downloadedCount = downloadedCount,
                        reason          = e.localizedMessage ?: e.javaClass.simpleName
                    )
                    return
                }

                // Cập nhật tên truyện sau chương đầu tiên
                if (downloadedCount == startedDownloadedCount) {
                    novelTitle = parsed.novelTitle.ifBlank { novelTitle }
                }

                // ── Lưu Novel (upsert) ────────────────────────────────────
                val existingNovels = NovelAudioStorage.getNovels(context)
                val existingNovel  = existingNovels.firstOrNull { it.id == novelId }
                val novel = (existingNovel ?: Novel(
                    id        = novelId,
                    title     = novelTitle,
                    sourceUrl = startUrl
                )).copy(
                    title                = novelTitle,
                    downloadProgress     = downloadedCount,
                    isFullTextDownloaded = false
                )
                NovelAudioStorage.saveNovel(context, novel)

                // ── Lưu Chapter ───────────────────────────────────────────
                val existingChapters = NovelAudioStorage.getChaptersForNovel(context, novelId)
                // Tránh tải trùng cùng URL
                val alreadyExists = existingChapters.any { it.sourceUrl == currentUrl }
                if (!alreadyExists) {
                    val chapter = NovelChapter(
                        id               = UUID.randomUUID().toString(),
                        novelId          = novelId,
                        chapterIndex     = downloadedCount,
                        title            = parsed.chapterTitle,
                        sourceUrl        = currentUrl,
                        rawContent       = parsed.rawContent,
                        sanitizedContent = parsed.sanitizedContent,
                        downloadStatus   = DownloadStatus.DOWNLOADED,
                        downloadProgress = 100
                    )
                    NovelAudioStorage.saveChapter(context, chapter)
                    downloadedCount++
                } else {
                    Log.d(TAG, "Chương đã tồn tại trong DB, bỏ qua: $currentUrl")
                    downloadedCount++
                }

                // ── Cập nhật state progress ───────────────────────────────
                _state.value = BatchDownloadState.Downloading(
                    novelTitle          = novelTitle,
                    novelId             = novelId,
                    downloadedCount     = downloadedCount,
                    estimatedTotal      = downloadedCount, // chưa biết tổng
                    currentChapterTitle = parsed.chapterTitle,
                    progressPercent     = 100
                )

                currentUrl = parsed.nextChapterUrl

                // Delay礼 giữa các request để tránh bị block
                if (currentUrl != null) {
                    delay(CHAPTER_DELAY_MS)
                }
            }

            // ── Hoàn thành ────────────────────────────────────────────────
            if (currentCoroutineContext().isActive) {
                // Đánh dấu Novel là đã tải xong toàn bộ
                val novels    = NovelAudioStorage.getNovels(context)
                val novel     = novels.firstOrNull { it.id == novelId }
                if (novel != null) {
                    NovelAudioStorage.saveNovel(
                        context,
                        novel.copy(
                            isFullTextDownloaded = true,
                            totalChapters        = downloadedCount,
                            downloadProgress     = downloadedCount
                        )
                    )
                }
                _state.value = BatchDownloadState.Completed(
                    novelTitle     = novelTitle,
                    novelId        = novelId,
                    totalDownloaded = downloadedCount
                )
                Log.d(TAG, "Hoàn thành tải $downloadedCount chương cho truyện '$novelTitle'")
                resumeNextUrl = null
                resumeNovelId = null
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Job bị cancel bởi người dùng")
            // State đã được set trong cancel()
        }
    }
}
