package com.dinh.gocnho.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

data class GameItem(
    val id: String,
    val title: String,
    val description: String,
    val highScore: Int,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null
)

@Composable
fun GameScreen(onPlayClick: (GameItem) -> Unit = {}) {
    var games by remember {
        mutableStateOf(
            listOf(
                GameItem(
                    id = "1",
                    title = "Block Puzzle",
                    description = "Strategically fit blocks into the grid to clear lines and rows.",
                    highScore = 4120
                ),
                GameItem(
                    id = "2",
                    title = "Sudoku Classic",
                    description = "Challenge your mind with classic Sudoku puzzles of varying difficulty.",
                    highScore = 1250
                ),
                GameItem(
                    id = "3",
                    title = "Word Master",
                    description = "Find hidden words and expand your vocabulary in this addictive game.",
                    highScore = 850
                ),
                GameItem(
                    id = "tetris",
                    title = "Tetris",
                    description = "Classic block falling puzzle game. Stack the pieces, clear the lines!",
                    highScore = 0
                )
            )
        )
    }

    val tabs = listOf("All Games", "Favorites")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Color(0xFF0284C7)
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            val filteredGames = when (page) {
                0 -> games
                1 -> games.filter { it.isFavorite }
                else -> games
            }

            GameList(
                games = filteredGames,
                onFavoriteToggle = { game ->
                    games = games.map {
                        if (it.id == game.id) it.copy(isFavorite = !it.isFavorite) else it
                    }
                },
                onPlayClick = onPlayClick
            )
        }
    }
}

@Composable
fun GameList(
    games: List<GameItem>,
    onFavoriteToggle: (GameItem) -> Unit,
    onPlayClick: (GameItem) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(games, key = { it.id }) { game ->
            GameCard(
                game = game,
                onFavoriteToggle = { onFavoriteToggle(game) },
                onPlayClick = { onPlayClick(game) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    GameScreen()
}

// ---------------------------------------------------------------------------
// Tetris thumbnail: a tiny decorative canvas of stacked colored blocks
// ---------------------------------------------------------------------------
@Composable
fun TetrisPreviewCanvas(modifier: Modifier = Modifier) {
    // A hand-crafted 10×8 mini-board for visual appeal
    val previewColors = listOf(
        Color(0xFF00BCD4), // I - cyan
        Color(0xFFFFEB3B), // O - yellow
        Color(0xFF9C27B0), // T - purple
        Color(0xFF4CAF50), // S - green
        Color(0xFFF44336), // Z - red
        Color(0xFF2196F3), // J - blue
        Color(0xFFFF9800), // L - orange
    )
    // Each row: list of colorIndex or -1 for empty
    val miniBoard = listOf(
        listOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        listOf(-1, -1, -1, 0,  0,  0,  0,  -1, -1, -1),
        listOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
        listOf(-1, 6,  -1, 2,  2,  2,  -1, 1,  1,  -1),
        listOf(-1, 6,  6,  -1, 2,  -1, -1, 1,  1,  -1),
        listOf(4,  4,  6,  -1, 3,  3,  -1, -1, 5,  5),
        listOf(-1, 4,  5,  5,  -1, 3,  -1, -1, -1, 5),
        listOf(6,  6,  6,  5,  0,  0,  0,  0,  3,  3),
    )
    Canvas(modifier = modifier) {
        val cols = 10
        val rows = miniBoard.size
        val cellW = size.width / cols
        val cellH = size.height / rows
        val gap = 2f

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val colorIdx = miniBoard[row][col]
                val left = col * cellW + gap
                val top = row * cellH + gap
                val w = cellW - gap * 2
                val h = cellH - gap * 2
                if (colorIdx < 0) {
                    // empty cell — subtle grid dot
                    drawRect(
                        color = Color(0xFF1E3A5F),
                        topLeft = Offset(left, top),
                        size = Size(w, h)
                    )
                } else {
                    val base = previewColors[colorIdx]
                    // face
                    drawRect(
                        color = base,
                        topLeft = Offset(left, top),
                        size = Size(w, h)
                    )
                    // highlight (top-left bevel)
                    drawRect(
                        color = base.copy(alpha = 0.6f),
                        topLeft = Offset(left, top),
                        size = Size(w, 3f)
                    )
                    drawRect(
                        color = base.copy(alpha = 0.6f),
                        topLeft = Offset(left, top),
                        size = Size(3f, h)
                    )
                    // shadow (bottom-right bevel)
                    drawRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(left, top + h - 3f),
                        size = Size(w, 3f)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(left + w - 3f, top),
                        size = Size(3f, h)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCard(
    game: GameItem,
    onFavoriteToggle: () -> Unit,
    onPlayClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B) // Slightly lighter dark for card
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Game Preview Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF334155))
            ) {
                // Tetris-specific thumbnail
                if (game.id == "tetris") {
                    TetrisPreviewCanvas(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Generic placeholder for other games
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }

                // Favorite Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onFavoriteToggle() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title and High Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "HIGH SCORE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", game.highScore),
                        color = Color(0xFF4ADE80),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = game.description,
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Play Button
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                )
            ) {
                Text(
                    text = "PLAY NOW",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
