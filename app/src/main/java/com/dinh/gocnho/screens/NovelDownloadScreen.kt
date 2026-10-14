package com.dinh.gocnho.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinh.gocnho.data.NovelAudioStorage
import com.dinh.gocnho.model.Novel
import com.dinh.gocnho.model.NovelChapter
import com.dinh.gocnho.model.ReadStatus
import com.dinh.gocnho.service.BatchDownloadService
import com.dinh.gocnho.service.BatchDownloadState

// ──────────────────────────────────────────────────────────────────────────────
//  NovelDownloadScreen – Bước 3
//  Màn hình quản lý tải và thư viện truyện chữ đã tải.
//  - Nhập link chương đầu → bắt đầu tải hàng loạt (BatchDownloadService)
//  - Xem tiến trình realtime, Pause/Resume/Cancel
//  - Thư viện: danh sách bộ truyện đã tải → mở NovelReaderScreen
// ──────────────────────────────────────────────────────────────────────────────

/** Màn hình chính của Bước 3: Tải & Đọc truyện offline */
@Composable
fun NovelDownloadScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // State từ BatchDownloadService
    val downloadState by BatchDownloadService.state.collectAsState()

    // Novels đã lưu trong DB
    var novels by remember { mutableStateOf(NovelAudioStorage.getNovels(context)) }
    var selectedNovel by remember { mutableStateOf<Novel?>(null) }
    var selectedNovelChapters by remember { mutableStateOf<List<NovelChapter>>(emptyList()) }

    // Dialog xóa truyện
    var novelToDelete by remember { mutableStateOf<Novel?>(null) }

    // URL input
    var inputUrl by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }

    // Khi downloadState hoàn thành / thay đổi → reload novels list
    LaunchedEffect(downloadState) {
        if (downloadState is BatchDownloadState.Completed
            || downloadState is BatchDownloadState.Failed
            || downloadState is BatchDownloadState.Cancelled
        ) {
            novels = NovelAudioStorage.getNovels(context)
        }
        // Cập nhật danh sách chương nếu đang xem truyện đang tải
        if (selectedNovel != null) {
            val current = downloadState
            val activeNovelId = when (current) {
                is BatchDownloadState.Downloading -> current.novelId
                is BatchDownloadState.Paused      -> current.novelId
                else -> null
            }
            if (activeNovelId != null && activeNovelId == selectedNovel?.id) {
                selectedNovelChapters = NovelAudioStorage.getChaptersForNovel(context, activeNovelId)
            }
        }
    }

    // Nếu đang mở Reader → hiển thị full screen reader
    if (selectedNovel != null) {
        val novel = selectedNovel!!

        // Tìm chương đọc dở gần nhất để khôi phục
        val resumeIndex = selectedNovelChapters.indexOfLast {
            it.readStatus == ReadStatus.READING
        }.coerceAtLeast(0)

        NovelReaderScreen(
            novel = novel,
            chapters = selectedNovelChapters,
            initialChapterIndex = resumeIndex,
            onBack = {
                // Lưu lại currentChapterId vào novel
                novels = NovelAudioStorage.getNovels(context)
                selectedNovel = null
                selectedNovelChapters = emptyList()
            },
            onChaptersUpdated = { updated ->
                selectedNovelChapters = updated
                // Lưu currentChapterId vào Novel nếu có thay đổi
                val readingIdx = updated.indexOfLast { it.readStatus == ReadStatus.READING }
                if (readingIdx >= 0) {
                    val newChapterId = updated[readingIdx].id
                    if (novel.currentChapterId != newChapterId) {
                        val updatedNovel = novel.copy(currentChapterId = newChapterId)
                        selectedNovel = updatedNovel
                        NovelAudioStorage.saveNovel(context, updatedNovel)
                    }
                }
            }
        )
        return
    }

    // ─── Main Screen ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Toolbar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tải & Đọc Truyện Offline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Bước 3: Tải hàng loạt chữ + Giao diện đọc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // ── Section 1: Download Control ───────────────────────────────────
            DownloadControlCard(
                downloadState = downloadState,
                inputUrl = inputUrl,
                onInputUrlChange = { inputUrl = it },
                showUrlInput = showUrlInput,
                onToggleUrlInput = { showUrlInput = !showUrlInput },
                onStartDownload = {
                    val url = inputUrl.trim()
                    if (url.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập URL chương đầu tiên!", Toast.LENGTH_SHORT).show()
                        return@DownloadControlCard
                    }
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(context, "URL không hợp lệ! Phải bắt đầu bằng http:// hoặc https://", Toast.LENGTH_SHORT).show()
                        return@DownloadControlCard
                    }
                    BatchDownloadService.startDownload(context, url)
                    showUrlInput = false
                    inputUrl = ""
                },
                onPaste = {
                    val text = clipboardManager.getText()?.text
                    if (!text.isNullOrBlank()) inputUrl = text.trim()
                },
                onPause  = { BatchDownloadService.pause() },
                onResume = { BatchDownloadService.resume(context) },
                onCancel = {
                    BatchDownloadService.cancel()
                    novels = NovelAudioStorage.getNovels(context)
                },
                onReset  = { BatchDownloadService.reset() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Novel Library ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📚 Thư viện truyện đã tải",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (novels.isNotEmpty()) {
                    Text(
                        text = "${novels.size} bộ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (novels.isEmpty()) {
                EmptyLibraryPlaceholder()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    novels.sortedByDescending { it.downloadProgress }.forEach { novel ->
                        NovelLibraryCard(
                            novel = novel,
                            isDownloading = run {
                                val s = downloadState
                                (s is BatchDownloadState.Downloading && s.novelId == novel.id) ||
                                (s is BatchDownloadState.Paused && s.novelId == novel.id)
                            },
                            downloadState = downloadState,
                            onOpen = {
                                val chapters = NovelAudioStorage.getChaptersForNovel(context, novel.id)
                                selectedNovelChapters = chapters.sortedBy { it.chapterIndex }
                                selectedNovel = novel
                            },
                            onDelete = { novelToDelete = novel }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialog xác nhận xóa truyện ───────────────────────────────────────────
    novelToDelete?.let { novel ->
        AlertDialog(
            onDismissRequest = { novelToDelete = null },
            title = { Text("Xóa truyện") },
            text = {
                Text("Bạn có chắc muốn xóa \"${novel.title}\" và toàn bộ ${novel.downloadProgress} chương đã tải?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        NovelAudioStorage.deleteNovel(context, novel.id)
                        novels = NovelAudioStorage.getNovels(context)
                        novelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { novelToDelete = null }) { Text("Hủy") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  DOWNLOAD CONTROL CARD
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadControlCard(
    downloadState: BatchDownloadState,
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    showUrlInput: Boolean,
    onToggleUrlInput: () -> Unit,
    onStartDownload: () -> Unit,
    onPaste: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tải toàn bộ truyện",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Nhập link chương đầu → App sẽ tự động cào toàn bộ nội dung chữ và lưu offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Trạng thái hiện tại
            DownloadStateWidget(
                state = downloadState,
                showUrlInput = showUrlInput,
                inputUrl = inputUrl,
                onInputUrlChange = onInputUrlChange,
                onToggleUrlInput = onToggleUrlInput,
                onStartDownload = onStartDownload,
                onPaste = onPaste,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun DownloadStateWidget(
    state: BatchDownloadState,
    showUrlInput: Boolean,
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    onToggleUrlInput: () -> Unit,
    onStartDownload: () -> Unit,
    onPaste: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    when (state) {
        is BatchDownloadState.Idle -> {
            IdleDownloadWidget(
                showUrlInput = showUrlInput,
                inputUrl = inputUrl,
                onInputUrlChange = onInputUrlChange,
                onToggleUrlInput = onToggleUrlInput,
                onStartDownload = onStartDownload,
                onPaste = onPaste
            )
        }

        is BatchDownloadState.Downloading -> {
            ActiveDownloadWidget(
                title = state.novelTitle,
                downloadedCount = state.downloadedCount,
                estimatedTotal = state.estimatedTotal,
                chapterTitle = state.currentChapterTitle,
                onPause = onPause,
                onCancel = onCancel
            )
        }

        is BatchDownloadState.Paused -> {
            PausedDownloadWidget(
                title = state.novelTitle,
                downloadedCount = state.downloadedCount,
                onResume = onResume,
                onCancel = onCancel
            )
        }

        is BatchDownloadState.Completed -> {
            CompletedDownloadWidget(
                title = state.novelTitle,
                total = state.totalDownloaded,
                onReset = onReset
            )
        }

        is BatchDownloadState.Failed -> {
            FailedDownloadWidget(
                title = state.novelTitle,
                reason = state.reason,
                downloadedCount = state.downloadedCount,
                onReset = onReset
            )
        }

        is BatchDownloadState.Cancelled -> {
            CancelledDownloadWidget(
                title = state.novelTitle,
                downloadedCount = state.downloadedCount,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun IdleDownloadWidget(
    showUrlInput: Boolean,
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    onToggleUrlInput: () -> Unit,
    onStartDownload: () -> Unit,
    onPaste: () -> Unit
) {
    AnimatedVisibility(visible = showUrlInput) {
        Column {
            OutlinedTextField(
                value = inputUrl,
                onValueChange = onInputUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "https://truyenfull.live/.../chuong-1/",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    if (inputUrl.isNotEmpty()) {
                        IconButton(onClick = { onInputUrlChange("") }) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showUrlInput) {
            OutlinedButton(
                onClick = onPaste,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Dán", fontSize = 13.sp)
            }
            Button(
                onClick = onStartDownload,
                enabled = inputUrl.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Bắt đầu tải", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!showUrlInput) {
            Button(
                onClick = onToggleUrlInput,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tải truyện mới", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveDownloadWidget(
    title: String,
    downloadedCount: Int,
    estimatedTotal: Int,
    chapterTitle: String,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Đang tải..." },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Đã tải: $downloadedCount chương",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Pause, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tạm dừng", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hủy", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PausedDownloadWidget(
    title: String,
    downloadedCount: Int,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đã tạm dừng",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "${title.ifBlank { "Truyện" }} – Đã tải $downloadedCount chương",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onResume,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tiếp tục", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hủy", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CompletedDownloadWidget(
    title: String,
    total: Int,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Hoàn tất!",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF4CAF50)
            )
            Text(
                "\"${title.ifBlank { "Truyện" }}\" – Đã tải $total chương",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onReset) { Text("Tải thêm") }
    }
}

@Composable
private fun FailedDownloadWidget(
    title: String,
    reason: String,
    downloadedCount: Int,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Tải thất bại",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Đã tải được $downloadedCount chương trước khi gặp lỗi.\n$reason",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 16.sp
            )
        }
        TextButton(onClick = onReset) { Text("OK") }
    }
}

@Composable
private fun CancelledDownloadWidget(
    title: String,
    downloadedCount: Int,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Đã hủy",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (downloadedCount > 0) {
                Text(
                    "Đã lưu $downloadedCount chương (có thể đọc offline).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onReset) { Text("Tải mới") }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  NOVEL LIBRARY CARD
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NovelLibraryCard(
    novel: Novel,
    isDownloading: Boolean,
    downloadState: BatchDownloadState,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon truyện
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = novel.title.ifBlank { "Không rõ tên" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (novel.isFullTextDownloaded)
                            "✅ Đã tải xong – ${novel.downloadProgress} chương"
                        else
                            "📥 ${novel.downloadProgress} chương (chưa hoàn tất)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (novel.isFullTextDownloaded) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress bar nếu đang tải
            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                val dl = downloadState
                val chap = if (dl is BatchDownloadState.Downloading) dl.currentChapterTitle
                else if (dl is BatchDownloadState.Paused) "Đã tạm dừng"
                else ""
                Text(
                    text = chap,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Đọc truyện", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Thư viện trống.\nNhập link chương đầu để bắt đầu tải truyện.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
