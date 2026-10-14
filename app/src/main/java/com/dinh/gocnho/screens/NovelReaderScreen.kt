package com.dinh.gocnho.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinh.gocnho.data.NovelAudioStorage
import com.dinh.gocnho.model.DownloadStatus
import com.dinh.gocnho.model.Novel
import com.dinh.gocnho.model.NovelChapter
import com.dinh.gocnho.model.ReadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────────────────────────────────────
//  NovelReaderScreen – Bước 3
//  Màn hình đọc truyện thủ công: hiển thị văn bản sạch, danh sách chương,
//  chuyển chương Prev/Next và lưu tiến trình đọc tự động.
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Màn hình đọc truyện thủ công.
 *
 * @param novel Thông tin bộ truyện (tiêu đề, ID)
 * @param chapters Danh sách chương đã tải (có sanitizedContent)
 * @param initialChapterIndex Chỉ số chương mở khi vào màn hình (0-based theo list)
 * @param onBack Callback quay lại màn hình trước
 * @param onChaptersUpdated Callback khi danh sách chương thay đổi (đọc xong, đánh dấu READ)
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun NovelReaderScreen(
    novel: Novel,
    chapters: List<NovelChapter>,
    initialChapterIndex: Int = 0,
    onBack: () -> Unit,
    onChaptersUpdated: (List<NovelChapter>) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Chỉ số chương hiện tại (mutable)
    var currentIndex by rememberSaveable { mutableIntStateOf(initialChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))) }

    // Danh sách chương local (cập nhật khi đánh dấu read/reading)
    var localChapters by remember { mutableStateOf(chapters) }

    // Hiện/ẩn chapter drawer
    var showDrawer by remember { mutableStateOf(false) }

    // Scroll state của vùng nội dung (khởi tạo riêng cho mỗi chương để tránh giữ vị trí chương cũ)
    val scrollState = remember(currentIndex) { androidx.compose.foundation.ScrollState(0) }

    val currentChapter = localChapters.getOrNull(currentIndex)
    val latestIndex by rememberUpdatedState(currentIndex)
    val latestChapters by rememberUpdatedState(localChapters)

    // Helper lưu vị trí đọc hiện tại ngay lập tức (khi chuyển chương hoặc thoát)
    val saveCurrentProgress: () -> Unit = remember(context) {
        {
            val idx = latestIndex
            val list = latestChapters
            val chapter = list.getOrNull(idx)
            if (chapter != null) {
                val maxVal = scrollState.maxValue
                val ratio = if (maxVal > 0) {
                    (scrollState.value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                } else chapter.lastReadPosition

                val newStatus = when {
                    ratio >= 0.95f -> ReadStatus.READ
                    chapter.readStatus == ReadStatus.UNREAD -> ReadStatus.READING
                    else -> chapter.readStatus
                }

                if (newStatus != chapter.readStatus || kotlin.math.abs(chapter.lastReadPosition - ratio) > 0.01f) {
                    val updatedChapter = chapter.copy(lastReadPosition = ratio, readStatus = newStatus)
                    val updated = list.toMutableList()
                    updated[idx] = updatedChapter
                    localChapters = updated
                    NovelAudioStorage.saveChapter(context, updatedChapter)
                    onChaptersUpdated(updated)
                }
            }
        }
    }

    val navigateToChapter: (Int) -> Unit = { targetIndex ->
        if (targetIndex in localChapters.indices && targetIndex != currentIndex) {
            saveCurrentProgress()
            currentIndex = targetIndex
        }
    }

    val handleBack: () -> Unit = {
        saveCurrentProgress()
        onBack()
    }

    // ─── Khôi phục vị trí cuộn & đánh dấu READING khi chuyển chương ──────
    LaunchedEffect(currentIndex, scrollState) {
        val chapter = localChapters.getOrNull(currentIndex) ?: return@LaunchedEffect

        // Đánh dấu READING nếu chưa đọc
        if (chapter.readStatus == ReadStatus.UNREAD) {
            val updatedChapter = chapter.copy(readStatus = ReadStatus.READING)
            val updated = localChapters.toMutableList()
            updated[currentIndex] = updatedChapter
            localChapters = updated
            withContext(Dispatchers.IO) {
                NovelAudioStorage.saveChapter(context, updatedChapter)
            }
            onChaptersUpdated(updated)
        }

        // Khôi phục vị trí cuộn đã lưu (chờ layout đo xong maxValue)
        val savedPosition = chapter.lastReadPosition
        if (savedPosition > 0f) {
            if (scrollState.maxValue <= 0) {
                snapshotFlow { scrollState.maxValue }
                    .filter { it > 0 }
                    .first()
            }
            val target = (savedPosition * scrollState.maxValue).toInt()
            scrollState.scrollTo(target)
        }
    }

    // ─── Đánh dấu READ khi cuộn đến cuối (>= 95%) ───────────────────────
    // Chỉ kích hoạt một lần duy nhất khi chạm mốc 95% và chưa là READ
    LaunchedEffect(currentIndex, scrollState) {
        val chapter = localChapters.getOrNull(currentIndex) ?: return@LaunchedEffect
        if (chapter.readStatus != ReadStatus.READ) {
            snapshotFlow {
                if (scrollState.maxValue > 0)
                    scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                else 0f
            }
            .filter { it >= 0.95f }
            .first()

            val current = localChapters.getOrNull(currentIndex)
            if (current != null && current.readStatus != ReadStatus.READ) {
                val updatedChapter = current.copy(readStatus = ReadStatus.READ, lastReadPosition = 1f)
                val updated = localChapters.toMutableList()
                updated[currentIndex] = updatedChapter
                localChapters = updated
                withContext(Dispatchers.IO) {
                    NovelAudioStorage.saveChapter(context, updatedChapter)
                }
                onChaptersUpdated(updated)
            }
        }
    }

    // ─── Debounced auto-save vị trí cuộn khi người dùng dừng cuộn 1.5s ───
    LaunchedEffect(currentIndex, scrollState) {
        snapshotFlow {
            if (scrollState.maxValue > 0)
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            else 0f
        }
        .debounce(1500L) // KHÔNG chạy khi đang cuộn; chỉ chạy khi dừng cuộn 1.5s
        .collect { ratio ->
            val chapter = localChapters.getOrNull(currentIndex) ?: return@collect
            if (kotlin.math.abs(chapter.lastReadPosition - ratio) > 0.02f) {
                val newStatus = if (ratio >= 0.95f) ReadStatus.READ else chapter.readStatus
                val updatedChapter = chapter.copy(lastReadPosition = ratio, readStatus = newStatus)
                val updated = localChapters.toMutableList()
                updated[currentIndex] = updatedChapter
                localChapters = updated
                withContext(Dispatchers.IO) {
                    NovelAudioStorage.saveChapter(context, updatedChapter)
                }
                onChaptersUpdated(updated)
            }
        }
    }

    // ─── Lưu tiến trình khi rời màn hình / đổi chương ─────────────────────
    DisposableEffect(currentIndex) {
        onDispose {
            val idx = latestIndex
            val list = latestChapters
            val chapter = list.getOrNull(idx) ?: return@onDispose
            val maxVal = scrollState.maxValue
            if (maxVal > 0) {
                val ratio = (scrollState.value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val newStatus = if (ratio >= 0.95f) ReadStatus.READ else chapter.readStatus
                if (newStatus != chapter.readStatus || kotlin.math.abs(chapter.lastReadPosition - ratio) > 0.01f) {
                    val updatedChapter = chapter.copy(lastReadPosition = ratio, readStatus = newStatus)
                    NovelAudioStorage.saveChapter(context, updatedChapter)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ─── Main Reader Layout ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            // ── Toolbar ───────────────────────────────────────────────────
            ReaderToolbar(
                novelTitle = novel.title,
                chapter = currentChapter,
                currentIndex = currentIndex,
                totalChapters = localChapters.size,
                onBack = handleBack,
                onShowDrawer = { showDrawer = true }
            )

            // ── Reading progress bar ──────────────────────────────────────
            val scrollProgress by remember {
                derivedStateOf {
                    if (scrollState.maxValue > 0)
                        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                    else 0f
                }
            }
            LinearProgressIndicator(
                progress = { scrollProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // ── Content Area ──────────────────────────────────────────────
            if (currentChapter == null) {
                EmptyChapterPlaceholder()
            } else {
                val content = remember(currentChapter.id) {
                    currentChapter.sanitizedContent
                        ?: currentChapter.rawContent
                        ?: "Chương này chưa có nội dung."
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Tên chương
                        Text(
                            text = currentChapter.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Nội dung chương
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 28.sp,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Nút prev/next ở cuối nội dung
                        ChapterNavigationRow(
                            hasPrev = currentIndex > 0,
                            hasNext = currentIndex < localChapters.size - 1,
                            onPrev = { navigateToChapter(currentIndex - 1) },
                            onNext = { navigateToChapter(currentIndex + 1) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // ── Bottom Nav Bar ────────────────────────────────────────────
            ReaderBottomBar(
                hasPrev = currentIndex > 0,
                hasNext = currentIndex < localChapters.size - 1,
                onPrev = { navigateToChapter(currentIndex - 1) },
                onNext = { navigateToChapter(currentIndex + 1) }
            )
        }

        // ─── Chapter Drawer Overlay ───────────────────────────────────────
        AnimatedVisibility(
            visible = showDrawer,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showDrawer = false }
            )
        }

        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(tween(280)) { -it },
            exit = slideOutHorizontally(tween(250)) { -it }
        ) {
            ChapterDrawer(
                chapters = localChapters,
                currentIndex = currentIndex,
                onChapterSelected = { idx ->
                    navigateToChapter(idx)
                    showDrawer = false
                },
                onClose = { showDrawer = false }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  SUB-COMPONENTS
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReaderToolbar(
    novelTitle: String,
    chapter: NovelChapter?,
    currentIndex: Int,
    totalChapters: Int,
    onBack: () -> Unit,
    onShowDrawer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
            Text(
                text = novelTitle,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chapter?.title ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Trạng thái đọc
                chapter?.readStatus?.let { status ->
                    Spacer(modifier = Modifier.width(4.dp))
                    when (status) {
                        ReadStatus.READ -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Đã đọc",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        ReadStatus.READING -> Icon(
                            imageVector = Icons.Default.BookmarkAdded,
                            contentDescription = "Đang đọc",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        else -> {}
                    }
                }
            }
        }

        // Số chương
        Text(
            text = "${currentIndex + 1}/$totalChapters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 4.dp)
        )

        // Nút mở danh sách chương
        IconButton(onClick = onShowDrawer) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Danh sách chương",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ReaderBottomBar(
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPrev,
            enabled = hasPrev,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NavigateBefore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Chương trước", fontSize = 13.sp)
        }

        Button(
            onClick = onNext,
            enabled = hasNext,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Chương sau", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChapterNavigationRow(
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasPrev) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.NavigateBefore, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Chương trước", fontSize = 13.sp)
            }
        }
        if (hasNext) {
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Chương sau", fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.NavigateNext, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyChapterPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Chưa có chương nào để đọc",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Drawer danh sách chương (slide từ trái ra).
 */
@Composable
private fun ChapterDrawer(
    chapters: List<NovelChapter>,
    currentIndex: Int,
    onChapterSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()

    // Tự scroll đến chương hiện tại khi drawer mở
    LaunchedEffect(Unit) {
        if (currentIndex in chapters.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .fillMaxWidth(0.82f),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Danh sách chương",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${chapters.size} chương",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có chương nào",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chapters.size) { idx ->
                        val chapter = chapters[idx]
                        val isSelected = idx == currentIndex
                        ChapterDrawerItem(
                            chapter = chapter,
                            index = idx,
                            isSelected = isSelected,
                            onClick = { onChapterSelected(idx) }
                        )
                        if (idx < chapters.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterDrawerItem(
    chapter: NovelChapter,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    else
        Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Số thứ tự chương
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title.ifBlank { "Chương ${index + 1}" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Hiển thị trạng thái đọc
            if (chapter.readStatus != ReadStatus.UNREAD) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (chapter.readStatus) {
                        ReadStatus.READ    -> "✓ Đã đọc"
                        ReadStatus.READING -> "📖 Đang đọc (${(chapter.lastReadPosition * 100).toInt()}%)"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (chapter.readStatus) {
                        ReadStatus.READ    -> Color(0xFF4CAF50)
                        ReadStatus.READING -> MaterialTheme.colorScheme.primary
                        else               -> Color.Transparent
                    }
                )
            }
        }

        // Icon trạng thái đọc bên phải
        when (chapter.readStatus) {
            ReadStatus.READ -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Đã đọc",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            ReadStatus.READING -> Icon(
                imageVector = Icons.Default.BookmarkAdded,
                contentDescription = "Đang đọc",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            else -> {}
        }
    }
}
