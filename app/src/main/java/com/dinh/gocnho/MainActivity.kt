package com.dinh.gocnho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinh.gocnho.ui.theme.GócNhỏTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Quản lý trạng thái dark mode ở cấp cao nhất
            var isDarkTheme by remember { mutableStateOf(false) }
            
            GócNhỏTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDarkTheme = it }
                )
            }
        }
    }
}

enum class Screen {
    HOME, STORY, GAME, PROFILE, SETTINGS, SCHEDULE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf(Screen.HOME) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                selectedScreen = selectedScreen,
                onScreenSelected = { screen ->
                    selectedScreen = screen
                    scope.launch { drawerState.close() }
                },
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = when (selectedScreen) {
                            Screen.HOME -> "Trang chủ"
                            Screen.STORY -> "Kho Truyện"
                            Screen.GAME -> "Minigame"
                            Screen.PROFILE -> "Hồ sơ cá nhân"
                            Screen.SETTINGS -> "Cài đặt"
                            Screen.SCHEDULE -> "Lịch học"
                        })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedScreen) {
                    Screen.HOME -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Màn hình Trang chủ") }
                    Screen.STORY -> StoryScreen()
                    Screen.GAME -> GameScreen()
                    Screen.PROFILE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Màn hình Hồ sơ") }
                    Screen.SETTINGS -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Màn hình Cài đặt") }
                    Screen.SCHEDULE -> ScheduleScreen()
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White, // Dark blue-ish for dark mode
        drawerContentColor = if (isDarkTheme) Color.White else Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                // Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Minh Hoàng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    // Online status dot (green)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MENU CHÍNH
            Text(
                text = "MENU CHÍNH",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            NavigationDrawerItem(
                label = { Text("Trang chủ") },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                selected = selectedScreen == Screen.HOME,
                onClick = { onScreenSelected(Screen.HOME) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            NavigationDrawerItem(
                label = { Text("Kho Truyện") },
                icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                selected = selectedScreen == Screen.STORY,
                onClick = { onScreenSelected(Screen.STORY) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            NavigationDrawerItem(
                label = { Text("Minigame") },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                selected = selectedScreen == Screen.GAME,
                onClick = { onScreenSelected(Screen.GAME) },
                badge = {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("HOT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

             NavigationDrawerItem(
                label = { Text("Lịch học") },
                icon = { Icon(Icons.Filled.Star, contentDescription = null) }, // Using Star as placeholder
                selected = selectedScreen == Screen.SCHEDULE,
                onClick = { onScreenSelected(Screen.SCHEDULE) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CÁ NHÂN
            Text(
                text = "CÁ NHÂN",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            NavigationDrawerItem(
                label = { Text("Hồ sơ cá nhân") },
                icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                selected = selectedScreen == Screen.PROFILE,
                onClick = { onScreenSelected(Screen.PROFILE) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            NavigationDrawerItem(
                label = { Text("Cài đặt") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = selectedScreen == Screen.SETTINGS,
                onClick = { onScreenSelected(Screen.SETTINGS) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Dark Mode Toggle Item
            NavigationDrawerItem(
                label = { Text("Chế độ tối") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) }, // Use Settings as placeholder for DarkMode icon
                selected = false,
                onClick = { onThemeChange(!isDarkTheme) },
                badge = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2196F3),
                            checkedTrackColor = Color(0xFFBBDEFB),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    unselectedTextColor = if (isDarkTheme) Color.LightGray else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Button(
                onClick = { /* Logout */ },
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = if (isDarkTheme) Color(0xFFFF8A80) else Color.Red
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                 Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Đăng xuất")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Phiên bản 2.4.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
