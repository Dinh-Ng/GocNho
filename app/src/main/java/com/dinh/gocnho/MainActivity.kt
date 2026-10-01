package com.dinh.gocnho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinh.gocnho.screens.GameScreen
import com.dinh.gocnho.screens.SettingsScreen
import com.dinh.gocnho.screens.StoryScreen
import com.dinh.gocnho.screens.TetrisScreen
import com.dinh.gocnho.ui.theme.AppThemeMode
import com.dinh.gocnho.ui.theme.GócNhỏTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
            GócNhỏTheme(themeMode = themeMode) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeChange = { themeMode = it }
                )
            }
        }
    }
}

enum class Screen {
    HOME, STORY, GAME, SETTINGS
}

// ──────────────────────────────────────────────────────────
//  ROOT – Quản lý navigation stack đơn giản không dùng Drawer
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    var selectedScreen by remember { mutableStateOf(Screen.HOME) }
    // currentGameId: null = danh sách game, "tetris" = đang chơi Tetris
    var currentGameId by remember { mutableStateOf<String?>(null) }
    // Admin
    var isAdmin by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAdminLogoutDialog by remember { mutableStateOf(false) }

    // Back: Khi đang ở game cụ thể → quay về danh sách game
    BackHandler(enabled = selectedScreen == Screen.GAME && currentGameId != null) {
        currentGameId = null
    }
    // Back: Khi đang ở màn hình con (Story/Game/Settings) → quay về Home
    BackHandler(enabled = selectedScreen != Screen.HOME && currentGameId == null) {
        selectedScreen = Screen.HOME
    }

    when {
        // ── Màn hình Story (full-screen, không toolbar) ──
        selectedScreen == Screen.STORY -> {
            StoryScreen(
                onBack = { selectedScreen = Screen.HOME }
            )
        }

        // ── Màn hình Game (full-screen, không toolbar) ──
        selectedScreen == Screen.GAME -> {
            if (currentGameId == "tetris") {
                TetrisScreen(onBack = { currentGameId = null })
            } else {
                GameScreen(
                    onPlayClick = { game ->
                        if (game.id == "tetris") currentGameId = "tetris"
                    },
                    onBack = { selectedScreen = Screen.HOME }
                )
            }
        }

        // ── Màn hình Cài đặt (có Scaffold + TopAppBar riêng) ──
        selectedScreen == Screen.SETTINGS -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Cài đặt") },
                        navigationIcon = {
                            TextButton(onClick = { selectedScreen = Screen.HOME }) {
                                Text("← Quay lại", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    SettingsScreen(
                        currentThemeMode = themeMode,
                        onThemeChange = onThemeChange
                    )
                }
            }
        }

        // ── Trang chủ ──
        else -> {
            HomeScreen(
                isAdmin = isAdmin,
                onNavigateToStory = { selectedScreen = Screen.STORY },
                onNavigateToGame = { selectedScreen = Screen.GAME },
                onNavigateToSettings = { selectedScreen = Screen.SETTINGS },
                onAvatarLongClick = {
                    if (isAdmin) showAdminLogoutDialog = true
                    else showAdminLoginDialog = true
                }
            )
        }
    }

    // ── Admin Dialogs ──
    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminLoginDialog = false },
            onLoginSuccess = {
                isAdmin = true
                showAdminLoginDialog = false
            }
        )
    }
    if (showAdminLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLogoutDialog = false },
            title = { Text("Đăng xuất Admin") },
            text = { Text("Bạn có chắc chắn muốn thoát quyền quản trị viên?") },
            confirmButton = {
                TextButton(onClick = {
                    isAdmin = false
                    showAdminLogoutDialog = false
                }) { Text("Đồng ý") }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
//  HOME SCREEN – Giao diện trang chủ với navigation cards
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isAdmin: Boolean,
    onNavigateToStory: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAvatarLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Goc Nhỏ 👋",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chọn hoạt động bạn muốn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Avatar (giữ chức năng Admin login bằng long press)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAdmin) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                    .combinedClickable(
                        onClick = { onNavigateToSettings() },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAvatarLongClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Cài đặt / Admin",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Navigation Cards ──
        NavCard(
            title = "Kho Truyện",
            subtitle = "Đọc truyện mọi lúc mọi nơi",
            icon = Icons.Default.AutoStories,
            gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
            onClick = onNavigateToStory
        )

        Spacer(modifier = Modifier.height(16.dp))

        NavCard(
            title = "Minigame",
            subtitle = "Giải trí với các trò chơi nhỏ",
            icon = Icons.Default.SportsEsports,
            gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
            badge = "HOT",
            onClick = onNavigateToGame
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Quick Action: Settings ──
        TextButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Cài đặt",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Phiên bản 2.4.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────
//  NAV CARD – Card điều hướng dùng trên HomeScreen
// ──────────────────────────────────────────────────────────
@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    badge: String? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon nền tròn mờ
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Arrow indicator
                Button(
                    onClick = onClick,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.25f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("→", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  ADMIN LOGIN DIALOG
// ──────────────────────────────────────────────────────────
@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác thực Admin") },
        text = {
            Column {
                Text("Nhập mã xác thực:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text("Mã xác thực") },
                    isError = isError,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Mã xác thực không đúng!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == "115197") {
                        onLoginSuccess()
                    } else {
                        isError = true
                    }
                }
            ) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
