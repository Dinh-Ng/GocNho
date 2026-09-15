package com.dinh.gocnho.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.dinh.gocnho.model.Story
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

@Composable
fun StoryScreen(
    onStorySelected: ((Story) -> Unit)? = null
) {
    val context = LocalContext.current
    var storyList by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedStoryForDialog by remember { mutableStateOf<Story?>(null) }

    // Lắng nghe dữ liệu realtime từ Firestore collection "stories"
    DisposableEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        var registration: ListenerRegistration? = null

        try {
            registration = firestore.collection("stories")
//                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        errorMessage = "Lỗi khi tải dữ liệu: ${error.localizedMessage}"
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        errorMessage = null
                        Log.d("StoryScreen", "Tổng số documents nhận được: ${snapshot.size()}")
                        
                        // Tự động map dữ liệu và lấy Document ID vào story.id
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    items(storyList, key = { it.id }) { story ->
                        StoryCard(
                            story = story,
                            onClick = {
                                selectedStoryForDialog = story
                                onStorySelected?.invoke(story)
                            }
                        )
                    }
                }
            }
        }

        // Dialog chi tiết truyện khi click vào item
        selectedStoryForDialog?.let { story ->
            StoryDetailDialog(
                story = story,
                onDismiss = { selectedStoryForDialog = null }
            )
        }
    }
}

@Composable
private fun StoryCard(
    story: Story,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
