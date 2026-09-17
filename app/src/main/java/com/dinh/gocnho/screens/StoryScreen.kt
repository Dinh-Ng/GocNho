package com.dinh.gocnho.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinh.gocnho.model.Chapter
import com.dinh.gocnho.model.Story
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.filled.PlayArrow

@Composable
fun StoryScreen(
    onStorySelected: ((Story) -> Unit)? = null
) {
    val context = LocalContext.current
    var storyList by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStoryForDialog by remember { mutableStateOf<Story?>(null) }
    
    // Quản lý chế độ sắp xếp
    var isSortByRecent by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Quản lý điều hướng danh sách chương và màn hình đọc truyện
    var activeStoryForChapters by remember { mutableStateOf<Story?>(null) }
    var activeChapterForReading by remember { mutableStateOf<Chapter?>(null) }

    // Sử dụng SharedPreferences để quản lý lịch sử đọc truyện lưu local
    val sharedPrefs = remember { context.getSharedPreferences("story_history_prefs", Context.MODE_PRIVATE) }
    // Map lưu giữ cặp giá trị: key = storyId, value = timestamp (Long) thời gian vừa đọc truyện đó
    var historyMap by remember {
        mutableStateOf(
            sharedPrefs.all.mapValues { entry -> (entry.value as? Long) ?: 0L }
        )
    }

    // SharedPreferences lưu tiến độ đọc: chapter cuối & vị trí scroll
    val chapterProgressPrefs = remember {
        context.getSharedPreferences("reading_progress_chapter", Context.MODE_PRIVATE)
    }
    val scrollProgressPrefs = remember {
        context.getSharedPreferences("reading_progress_scroll", Context.MODE_PRIVATE)
    }

    // Hàm cập nhật lịch sử đọc truyện
    val updateStoryReadHistory = { storyId: String ->
        val currentTime = System.currentTimeMillis()
        sharedPrefs.edit().putLong(storyId, currentTime).apply()
        // Cập nhật lại state để kích hoạt recompose và sắp xếp lại danh sách
        historyMap = sharedPrefs.all.mapValues { entry -> (entry.value as? Long) ?: 0L }
    }

    // Lắng nghe dữ liệu realtime từ Firestore collection "stories"
    DisposableEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        var registration: ListenerRegistration? = null

        try {
            registration = firestore.collection("stories")
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        errorMessage = "Lỗi khi tải dữ liệu: ${error.localizedMessage}"
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        errorMessage = null
                        Log.d("StoryScreen", "Tổng số documents nhận được: ${snapshot.size()}")
                        
                        val mappedStories = mutableListOf<Story>()
                        for (doc in snapshot.documents) {
                            try {
                                val story = doc.toObject(Story::class.java)?.copy(id = doc.id)
                                Log.d("StoryScreen", "Doc ID: ${doc.id}, data: ${doc.data}, parsed Story: $story")
                                if (story != null) {
                                    mappedStories.add(story)
                                }
                            } catch (e: Exception) {
                                Log.e("StoryScreen", "Lỗi map Document ID: ${doc.id}, Exception: ${e.message}", e)
                            }
                        }
                        storyList = mappedStories
                        Log.d("StoryScreen", "Tổng số story map thành công: ${storyList.size}")
                    }
                }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Ngoại lệ: ${e.localizedMessage}"
        }

        onDispose {
            registration?.remove()
        }
    }

    // Thực hiện sắp xếp danh sách truyện tại bộ nhớ Memory (Client Side Sorting)
    val sortedStoryList = remember(storyList, historyMap, isSortByRecent) {
        if (isSortByRecent) {
            storyList.sortedWith { s1, s2 ->
                val time1 = historyMap[s1.id] ?: 0L
                val time2 = historyMap[s2.id] ?: 0L
                
                when {
                    time1 > 0L && time2 > 0L -> time2.compareTo(time1)
                    time1 > 0L && time2 == 0L -> -1
                    time1 == 0L && time2 > 0L -> 1
                    else -> {
                        val t1 = s1.updatedAt?.seconds ?: s1.createdAt?.seconds ?: 0L
                        val t2 = s2.updatedAt?.seconds ?: s2.createdAt?.seconds ?: 0L
                        if (t1 != t2) t2.compareTo(t1) else s1.title.compareTo(s2.title)
                    }
                }
            }
        } else {
            // Sắp xếp mặc định: updatedAt -> createdAt -> title
            storyList.sortedWith { s1, s2 ->
                val t1 = s1.updatedAt?.seconds ?: s1.createdAt?.seconds ?: 0L
                val t2 = s2.updatedAt?.seconds ?: s2.createdAt?.seconds ?: 0L
                if (t1 != t2) t2.compareTo(t1) else s1.title.compareTo(s2.title)
            }
        }
    }

    when {
        activeChapterForReading != null -> {
            val chapter = activeChapterForReading!!
            ChapterReaderScreen(
                chapter = chapter,
                onBack = { activeChapterForReading = null },
                savedScrollPx = scrollProgressPrefs.getInt(chapter.id, 0),
                onScrollChanged = { chapterId, scrollPx ->
                    scrollProgressPrefs.edit().putInt(chapterId, scrollPx).apply()
                }
            )
        }
        activeStoryForChapters != null -> {
            val story = activeStoryForChapters!!
            ChapterListScreen(
                story = story,
                onBack = { activeStoryForChapters = null },
                lastChapterId = chapterProgressPrefs.getString(story.id, null),
                onChapterClick = { chapter ->
                    chapterProgressPrefs.edit().putString(story.id, chapter.id).apply()
                    activeChapterForReading = chapter
                }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Thanh công cụ sắp xếp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSortByRecent) "Vừa đọc gần đây" else "Mặc định",
                                fontSize = 13.sp
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mặc định") },
                                onClick = {
                                    isSortByRecent = false
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Vừa đọc gần đây") },
                                onClick = {
                                    isSortByRecent = true
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Đang tải danh sách truyện...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }

                        errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMessage ?: "Đã xảy ra lỗi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        storyList.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Chưa có truyện nào trong kho",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Các truyện mới thêm trên Firestore sẽ xuất hiện ở đây.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sortedStoryList, key = { it.id }) { story ->
                                    StoryCard(
                                        story = story,
                                        onClick = {
                                            activeStoryForChapters = story
                                            updateStoryReadHistory(story.id)
                                            onStorySelected?.invoke(story)
                                        },
                                        onLongClick = {
                                            selectedStoryForDialog = story
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog chi tiết truyện khi nhấn giữ item
    selectedStoryForDialog?.let { story ->
        StoryDetailDialog(
            story = story,
            onDismiss = { selectedStoryForDialog = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title (Bắt buộc non-null)
            Text(
                text = story.title.ifBlank { "Truyện chưa đặt tên" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Author (Nullable)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = story.author?.let { "Tác giả: $it" } ?: "Tác giả: Đang cập nhật",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Thời gian cập nhật / tạo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cập nhật: ${story.getFormattedTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ChapterListScreen(
    story: Story,
    onBack: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    lastChapterId: String? = null
) {
    BackHandler(onBack = onBack)

    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    DisposableEffect(story.id) {
        val firestore = FirebaseFirestore.getInstance()
        val registration = firestore.collection("stories")
            .document(story.id)
            .collection("chapters")
            .orderBy("index")
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = "Lỗi tải danh sách chương: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    errorMessage = null
                    chapters = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chapter::class.java)?.copy(id = doc.id)
                    }
                }
            }
        onDispose { registration.remove() }
    }

    // Auto-scroll đến chương cuối đọc khi danh sách sẵn sàng
    LaunchedEffect(chapters, lastChapterId) {
        if (lastChapterId != null && chapters.isNotEmpty()) {
            val targetIndex = chapters.indexOfFirst { it.id == lastChapterId }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Quay lại kho truyện", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = story.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (!story.author.isNullOrBlank()) {
            Text(
                text = "Tác giả: ${story.author}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
            chapters.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Truyện chưa có chương nào.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        val isLastRead = chapter.id == lastChapterId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterClick(chapter) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLastRead)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.title.ifBlank { "Chương không tên" },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = if (isLastRead)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isLastRead) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Đọc tiếp",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterReaderScreen(
    chapter: Chapter,
    onBack: () -> Unit,
    savedScrollPx: Int = 0,
    onScrollChanged: (String, Int) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBack)

    // key(chapter.id) đảm bảo scrollState tự reset về 0 khi mở chương mới
    key(chapter.id) {
        val scrollState = rememberScrollState(initial = savedScrollPx)

        // Tính phần trăm đọc inline — không cần thêm State, Compose tự recompose khi scroll
        val readPercent = if (scrollState.maxValue > 0)
            (scrollState.value.toFloat() / scrollState.maxValue * 100).toInt().coerceIn(0, 100)
        else 0

        // Debounce 500ms rồi lưu vị trí scroll
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }
                .collectLatest { scrollPx ->
                    delay(500)
                    onScrollChanged(chapter.id, scrollPx)
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp)
            ) {
                // Thanh back + % đọc trên cùng một hàng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            "← Quay lại danh sách chương",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$readPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Progress bar mỏng ngay dưới header ──
                LinearProgressIndicator(
                    progress = { readPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = chapter.title.ifBlank { "Nội dung chương" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = chapter.content.ifBlank { "Không có nội dung cho chương này." },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Padding cuối để thanh progress không che text khi đọc đến cuối
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}


@Composable
private fun StoryDetailDialog(
    story: Story,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = story.title.ifBlank { "Chi tiết truyện" },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Document ID:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = story.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = story.author?.let { "Tác giả: $it" } ?: "Tác giả: Đang cập nhật",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!story.authorLink.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(story.authorLink))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở đường dẫn", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trang tác giả", fontSize = 13.sp)
                    }
                }

                Text(
                    text = "Thời gian: ${story.getFormattedTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!story.createdBy.isNullOrBlank()) {
                    Text(
                        text = "Người tạo: ${story.createdBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
