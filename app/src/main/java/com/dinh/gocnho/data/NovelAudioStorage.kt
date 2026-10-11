package com.dinh.gocnho.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.dinh.gocnho.model.ApiKeyInfo
import com.dinh.gocnho.model.ApiKeyStatus
import com.dinh.gocnho.model.AudioPlayerState
import com.dinh.gocnho.model.DownloadStatus
import com.dinh.gocnho.model.ListenStatus
import com.dinh.gocnho.model.Novel
import com.dinh.gocnho.model.NovelChapter
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * NovelAudioStorage - Helper lưu trữ dữ liệu tính năng Novel Audio vào SharedPreferences.
 *
 * Nhất quán với cách app hiện tại dùng SharedPreferences (story_history_prefs,
 * reading_progress_chapter, reading_progress_scroll).
 *
 * Tổ chức theo 4 namespace SharedPreferences riêng biệt:
 *  - novel_audio_novels   -> danh sách bộ truyện
 *  - novel_audio_chapters -> danh sách chương (key = chapterJson_<novelId>)
 *  - novel_audio_api_keys -> danh sách API Key (mã hóa Base64)
 *  - novel_audio_player   -> trạng thái trình phát audio
 */
object NovelAudioStorage {

    private const val PREFS_NOVELS   = "novel_audio_novels"
    private const val PREFS_CHAPTERS = "novel_audio_chapters"
    private const val PREFS_API_KEYS = "novel_audio_api_keys"
    private const val PREFS_PLAYER   = "novel_audio_player"

    private const val KEY_NOVELS_LIST   = "novels_list"
    private const val KEY_API_KEYS_LIST = "api_keys_list"

    // =====================================================================
    // NOVELS
    // =====================================================================

    fun getNovels(context: Context): List<Novel> {
        val json = novelPrefs(context).getString(KEY_NOVELS_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> novelFromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNovel(context: Context, novel: Novel) {
        val novels = getNovels(context).toMutableList()
        val idx = novels.indexOfFirst { it.id == novel.id }
        if (idx >= 0) novels[idx] = novel else novels.add(novel)
        novelPrefs(context).edit { putString(KEY_NOVELS_LIST, novelsToJson(novels)) }
    }

    fun deleteNovel(context: Context, novelId: String) {
        val novels = getNovels(context).filter { it.id != novelId }
        novelPrefs(context).edit { putString(KEY_NOVELS_LIST, novelsToJson(novels)) }
        chapterPrefs(context).edit { remove("chapters_$novelId") }
    }

    fun generateNovelId(): String = UUID.randomUUID().toString()

    // =====================================================================
    // CHAPTERS
    // =====================================================================

    fun getChaptersForNovel(context: Context, novelId: String): List<NovelChapter> {
        val json = chapterPrefs(context).getString("chapters_$novelId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> chapterFromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChapter(context: Context, chapter: NovelChapter) {
        val chapters = getChaptersForNovel(context, chapter.novelId).toMutableList()
        val idx = chapters.indexOfFirst { it.id == chapter.id }
        if (idx >= 0) chapters[idx] = chapter else chapters.add(chapter)
        chapterPrefs(context).edit {
            putString("chapters_${chapter.novelId}", chaptersToJson(chapters))
        }
    }

    fun saveListenPosition(context: Context, chapterId: String, positionSec: Float) {
        chapterPrefs(context).edit { putFloat("listen_pos_$chapterId", positionSec) }
    }

    fun getListenPosition(context: Context, chapterId: String): Float {
        return chapterPrefs(context).getFloat("listen_pos_$chapterId", 0f)
    }

    // =====================================================================
    // API KEYS
    // =====================================================================

    fun getApiKeys(context: Context): List<ApiKeyInfo> {
        val json = apiKeyPrefs(context).getString(KEY_API_KEYS_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> apiKeyFromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Trả về API Keys với key đã GIẢI MÃ - dùng khi cần gọi API thực tế */
    fun getApiKeysDecoded(context: Context): List<ApiKeyInfo> =
        getApiKeys(context).map { it.copy(key = decode(it.key)) }

    fun saveApiKey(context: Context, keyInfo: ApiKeyInfo) {
        val keys = getApiKeys(context).toMutableList()
        val encoded = keyInfo.copy(key = encode(keyInfo.key))
        val idx = keys.indexOfFirst { it.id == keyInfo.id }
        if (idx >= 0) keys[idx] = encoded else keys.add(encoded)
        apiKeyPrefs(context).edit { putString(KEY_API_KEYS_LIST, apiKeysToJson(keys)) }
    }

    fun deleteApiKey(context: Context, keyId: String) {
        val keys = getApiKeys(context).filter { it.id != keyId }
        apiKeyPrefs(context).edit { putString(KEY_API_KEYS_LIST, apiKeysToJson(keys)) }
    }

    fun updateApiKeyStatus(
        context: Context,
        keyId: String,
        status: ApiKeyStatus,
        exceededAt: Long = 0L
    ) {
        val keys = getApiKeys(context).toMutableList()
        val idx = keys.indexOfFirst { it.id == keyId }
        if (idx >= 0) {
            keys[idx] = keys[idx].copy(status = status, exceededAt = exceededAt)
            apiKeyPrefs(context).edit { putString(KEY_API_KEYS_LIST, apiKeysToJson(keys)) }
        }
    }

    fun generateKeyId(): String = UUID.randomUUID().toString()

    // =====================================================================
    // PLAYER STATE
    // =====================================================================

    fun getPlayerState(context: Context): AudioPlayerState {
        val p = playerPrefs(context)
        return AudioPlayerState(
            isPlaying          = p.getBoolean("is_playing", false),
            currentChapterId   = p.getString("current_chapter_id", null),
            currentChunkIndex  = p.getInt("current_chunk_index", 0),
            playbackSpeed      = p.getFloat("playback_speed", 1.0f),
            sleepTimerMinutes  = p.getInt("sleep_timer_minutes", 0),
            remainingSleepTime = p.getInt("remaining_sleep_time", 0),
            isOfflineMode      = p.getBoolean("is_offline_mode", false),
            activeApiKeyId     = p.getString("active_api_key_id", null)
        )
    }

    fun savePlayerState(context: Context, state: AudioPlayerState) {
        playerPrefs(context).edit {
            putBoolean("is_playing", state.isPlaying)
            putString("current_chapter_id", state.currentChapterId)
            putInt("current_chunk_index", state.currentChunkIndex)
            putFloat("playback_speed", state.playbackSpeed)
            putInt("sleep_timer_minutes", state.sleepTimerMinutes)
            putInt("remaining_sleep_time", state.remainingSleepTime)
            putBoolean("is_offline_mode", state.isOfflineMode)
            putString("active_api_key_id", state.activeApiKeyId)
        }
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private fun novelPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NOVELS, Context.MODE_PRIVATE)

    private fun chapterPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_CHAPTERS, Context.MODE_PRIVATE)

    private fun apiKeyPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_API_KEYS, Context.MODE_PRIVATE)

    private fun playerPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_PLAYER, Context.MODE_PRIVATE)

    // ── JSON serialization ────────────────────────────────────────────────

    private fun novelToJson(n: Novel) = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("author", n.author ?: "")
        put("sourceUrl", n.sourceUrl)
        put("coverUrl", n.coverUrl ?: "")
        put("currentChapterId", n.currentChapterId ?: "")
        put("totalChapters", n.totalChapters)
        put("isFullTextDownloaded", n.isFullTextDownloaded)
        put("downloadProgress", n.downloadProgress)
    }

    private fun novelFromJson(o: JSONObject) = Novel(
        id                   = o.optString("id"),
        title                = o.optString("title"),
        author               = o.optString("author").ifBlank { null },
        sourceUrl            = o.optString("sourceUrl"),
        coverUrl             = o.optString("coverUrl").ifBlank { null },
        currentChapterId     = o.optString("currentChapterId").ifBlank { null },
        totalChapters        = o.optInt("totalChapters"),
        isFullTextDownloaded = o.optBoolean("isFullTextDownloaded"),
        downloadProgress     = o.optInt("downloadProgress")
    )

    private fun novelsToJson(list: List<Novel>): String {
        val arr = JSONArray()
        list.forEach { arr.put(novelToJson(it)) }
        return arr.toString()
    }

    private fun chapterToJson(c: NovelChapter) = JSONObject().apply {
        put("id", c.id)
        put("novelId", c.novelId)
        put("chapterIndex", c.chapterIndex)
        put("title", c.title)
        put("sourceUrl", c.sourceUrl)
        put("rawContent", c.rawContent ?: "")
        put("sanitizedContent", c.sanitizedContent ?: "")
        put("downloadStatus", c.downloadStatus.name)
        put("downloadProgress", c.downloadProgress)
        put("listenStatus", c.listenStatus.name)
        put("lastListenedPosition", c.lastListenedPosition.toDouble())
        put("localAudioPath", c.localAudioPath ?: "")
        put("audioFileSize", c.audioFileSize)
    }

    private fun chapterFromJson(o: JSONObject) = NovelChapter(
        id                   = o.optString("id"),
        novelId              = o.optString("novelId"),
        chapterIndex         = o.optInt("chapterIndex"),
        title                = o.optString("title"),
        sourceUrl            = o.optString("sourceUrl"),
        rawContent           = o.optString("rawContent").ifBlank { null },
        sanitizedContent     = o.optString("sanitizedContent").ifBlank { null },
        downloadStatus       = runCatching {
            DownloadStatus.valueOf(o.optString("downloadStatus"))
        }.getOrDefault(DownloadStatus.IDLE),
        downloadProgress     = o.optInt("downloadProgress"),
        listenStatus         = runCatching {
            ListenStatus.valueOf(o.optString("listenStatus"))
        }.getOrDefault(ListenStatus.UNLISTENED),
        lastListenedPosition = o.optDouble("lastListenedPosition", 0.0).toFloat(),
        localAudioPath       = o.optString("localAudioPath").ifBlank { null },
        audioFileSize        = o.optLong("audioFileSize", -1L)
    )

    private fun chaptersToJson(list: List<NovelChapter>): String {
        val arr = JSONArray()
        list.forEach { arr.put(chapterToJson(it)) }
        return arr.toString()
    }

    private fun apiKeyToJson(k: ApiKeyInfo) = JSONObject().apply {
        put("id", k.id)
        put("key", k.key)   // key đã được encode khi lưu
        put("label", k.label)
        put("status", k.status.name)
        put("lastUsedAt", k.lastUsedAt)
        put("exceededAt", k.exceededAt)
    }

    private fun apiKeyFromJson(o: JSONObject) = ApiKeyInfo(
        id         = o.optString("id"),
        key        = o.optString("key"),
        label      = o.optString("label"),
        status     = runCatching {
            ApiKeyStatus.valueOf(o.optString("status"))
        }.getOrDefault(ApiKeyStatus.ACTIVE),
        lastUsedAt = o.optLong("lastUsedAt", 0L),
        exceededAt = o.optLong("exceededAt", 0L)
    )

    private fun apiKeysToJson(list: List<ApiKeyInfo>): String {
        val arr = JSONArray()
        list.forEach { arr.put(apiKeyToJson(it)) }
        return arr.toString()
    }

    // Mã hóa nhẹ Base64 (che khuất key trong file prefs)
    private fun encode(raw: String): String =
        Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decode(encoded: String): String = try {
        String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
    } catch (e: Exception) {
        encoded
    }
}
