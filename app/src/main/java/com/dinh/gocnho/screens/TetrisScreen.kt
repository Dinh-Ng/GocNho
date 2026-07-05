package com.dinh.gocnho.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ──────────────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────────────

private const val BOARD_COLS = 10
private const val BOARD_ROWS = 20

// ──────────────────────────────────────────────────────────────────────────────
// Tetromino definitions  (shape → list of (row, col) offsets from pivot)
// ──────────────────────────────────────────────────────────────────────────────

enum class TetrominoType(val color: Color) {
    I(Color(0xFF00BCD4)),
    O(Color(0xFFFFEB3B)),
    T(Color(0xFF9C27B0)),
    S(Color(0xFF4CAF50)),
    Z(Color(0xFFF44336)),
    J(Color(0xFF2196F3)),
    L(Color(0xFFFF9800))
}

// Rotation states: each entry is the 4-rotation list of cell offsets
private val SHAPES: Map<TetrominoType, List<List<Pair<Int, Int>>>> = mapOf(
    TetrominoType.I to listOf(
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0))
    ),
    TetrominoType.O to listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))
    ),
    TetrominoType.T to listOf(
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, 1)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1))
    ),
    TetrominoType.S to listOf(
        // State 0: . S S / S S .  (horizontal)
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 0)),
        // State 1: S . / S S / . S  (vertical)
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, 1)),
        // State 2: same as 0
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 0)),
        // State 3: same as 1
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, 1))
    ),
    TetrominoType.Z to listOf(
        // State 0: Z Z . / . Z Z  (horizontal)
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1)),
        // State 1: . Z / Z Z / Z .  (vertical)
        listOf(Pair(-1, 1), Pair(0, 0), Pair(0, 1), Pair(1, 0)),
        // State 2: same as 0
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1)),
        // State 3: same as 1
        listOf(Pair(-1, 1), Pair(0, 0), Pair(0, 1), Pair(1, 0))
    ),
    TetrominoType.J to listOf(
        listOf(Pair(-1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, -1))
    ),
    TetrominoType.L to listOf(
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1)),
        listOf(Pair(-1, 0), Pair(-1, -1), Pair(0, 0), Pair(1, 0))
    )
)

// ──────────────────────────────────────────────────────────────────────────────
// Game State
// ──────────────────────────────────────────────────────────────────────────────

data class Piece(
    val type: TetrominoType,
    val row: Int,
    val col: Int,
    val rotation: Int = 0
) {
    val cells: List<Pair<Int, Int>>
        get() = SHAPES[type]!![rotation].map { (dr, dc) -> Pair(row + dr, col + dc) }
}

/** Returns a new random tetromino spawned at the top-center of the board */
private fun spawnPiece(type: TetrominoType = TetrominoType.entries.random()): Piece =
    Piece(type = type, row = 1, col = BOARD_COLS / 2)

/** Returns true if `piece` is in a valid position on `board` */
private fun isValid(board: Array<Array<Color?>>, piece: Piece): Boolean {
    return piece.cells.all { (r, c) ->
        r in 0 until BOARD_ROWS && c in 0 until BOARD_COLS && board[r][c] == null
    }
}

/** Lock `piece` into `board`, returning new board */
private fun lockPiece(board: Array<Array<Color?>>, piece: Piece): Array<Array<Color?>> {
    val newBoard = board.map { it.copyOf() }.toTypedArray()
    piece.cells.forEach { (r, c) -> newBoard[r][c] = piece.type.color }
    return newBoard
}

/** Clear full lines; returns (newBoard, linesCleared) */
private fun clearLines(board: Array<Array<Color?>>): Pair<Array<Array<Color?>>, Int> {
    val kept = board.filter { row -> row.any { it == null } }
    val cleared = BOARD_ROWS - kept.size
    if (cleared == 0) return Pair(board, 0)
    // Build new board: `cleared` empty rows on top, then the surviving rows below
    val newBoard = Array(BOARD_ROWS) { r ->
        if (r < cleared) arrayOfNulls<Color?>(BOARD_COLS) else kept[r - cleared]
    }
    return Pair(newBoard, cleared)
}

private fun scoreForLines(lines: Int, level: Int): Int = when (lines) {
    1 -> 100 * (level + 1)
    2 -> 300 * (level + 1)
    3 -> 500 * (level + 1)
    4 -> 800 * (level + 1)
    else -> 0
}

private fun gravityMs(level: Int): Long = maxOf(80L, 600L - level * 50L)

// ──────────────────────────────────────────────────────────────────────────────
// Drawing helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun drawCell(
    canvas: androidx.compose.ui.graphics.drawscope.DrawScope,
    color: Color,
    left: Float,
    top: Float,
    size: Float,
    gap: Float = 2f
) {
    val l = left + gap
    val t = top + gap
    val s = size - gap * 2
    // Face
    canvas.drawRect(color = color, topLeft = Offset(l, t), size = Size(s, s))
    // Top highlight
    canvas.drawRect(color = color.copy(alpha = 0.65f), topLeft = Offset(l, t), size = Size(s, 4f))
    // Left highlight
    canvas.drawRect(color = color.copy(alpha = 0.65f), topLeft = Offset(l, t), size = Size(4f, s))
    // Bottom shadow
    canvas.drawRect(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(l, t + s - 4f), size = Size(s, 4f))
    // Right shadow
    canvas.drawRect(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(l + s - 4f, t), size = Size(4f, s))
}

private fun drawEmptyCell(
    canvas: androidx.compose.ui.graphics.drawscope.DrawScope,
    left: Float,
    top: Float,
    size: Float
) {
    canvas.drawRect(
        color = Color(0xFF1A2744),
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f)
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// TetrisScreen composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TetrisScreen(onBack: () -> Unit) {
    // ── State ──
    var board by remember { mutableStateOf(Array(BOARD_ROWS) { arrayOfNulls<Color?>(BOARD_COLS) }) }
    var current by remember { mutableStateOf(spawnPiece()) }
    var next by remember { mutableStateOf(spawnPiece()) }
    var score by remember { mutableIntStateOf(0) }
    var lines by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isFastDrop by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }

    // ── Reset function ──
    fun resetGame() {
        board = Array(BOARD_ROWS) { arrayOfNulls<Color?>(BOARD_COLS) }
        current = spawnPiece()
        next = spawnPiece()
        score = 0
        lines = 0
        level = 0
        isPaused = false
        isGameOver = false
        isFastDrop = false
        showPauseDialog = false
    }

    // ── Lock & advance helper ──
    fun lockAndAdvance() {
        val locked = lockPiece(board, current)
        val (clearedBoard, numCleared) = clearLines(locked)
        board = clearedBoard
        lines += numCleared
        score += scoreForLines(numCleared, level)
        level = lines / 10
        val candidate = next
        next = spawnPiece()
        if (!isValid(board, candidate)) {
            isGameOver = true
        } else {
            current = candidate
        }
    }

    // ── Move helpers ──
    fun moveLeft() {
        if (isPaused || isGameOver) return
        val moved = current.copy(col = current.col - 1)
        if (isValid(board, moved)) current = moved
    }

    fun moveRight() {
        if (isPaused || isGameOver) return
        val moved = current.copy(col = current.col + 1)
        if (isValid(board, moved)) current = moved
    }

    fun rotate() {
        if (isPaused || isGameOver) return
        val rotated = current.copy(rotation = (current.rotation + 1) % 4)
        // Wall-kick: try original, then shift ±1, ±2
        val kicked = listOf(0, -1, 1, -2, 2).map { rotated.copy(col = rotated.col + it) }.firstOrNull { isValid(board, it) }
        if (kicked != null) current = kicked
    }

    fun dropOne() {
        if (isPaused || isGameOver) return
        val dropped = current.copy(row = current.row + 1)
        if (isValid(board, dropped)) {
            current = dropped
        } else {
            lockAndAdvance()
        }
    }

    // ── Gravity game loop ──
    LaunchedEffect(isPaused, isGameOver, level, isFastDrop) {
        while (!isPaused && !isGameOver) {
            delay(if (isFastDrop) 50L else gravityMs(level))
            dropOne()
        }
    }

    // ── Ghost piece (shows where piece will land) ──
    val ghost: Piece = run {
        var g = current
        while (isValid(board, g.copy(row = g.row + 1))) g = g.copy(row = g.row + 1)
        g
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI
    // ──────────────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "TETRIS",
                    color = Color(0xFF00BCD4),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                IconButton(onClick = {
                    if (!isGameOver) {
                        isPaused = !isPaused
                        if (isPaused) showPauseDialog = true
                    }
                }) {
                    Text(
                        text = if (isPaused) "▶" else "⏸",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Main area: Board + Sidebar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // ── Board Canvas ──
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(BOARD_COLS.toFloat() / BOARD_ROWS.toFloat())
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1B2A))
                ) {
                    val cellSize = size.width / BOARD_COLS

                    // Draw locked cells
                    for (r in 0 until BOARD_ROWS) {
                        for (c in 0 until BOARD_COLS) {
                            val cellColor = board[r][c]
                            if (cellColor != null) {
                                drawCell(this, cellColor, c * cellSize, r * cellSize, cellSize)
                            } else {
                                drawEmptyCell(this, c * cellSize, r * cellSize, cellSize)
                            }
                        }
                    }

                    // Draw ghost piece
                    if (!isGameOver) {
                        ghost.cells.forEach { (r, c) ->
                            if (r >= 0) {
                                drawRect(
                                    color = current.type.color.copy(alpha = 0.2f),
                                    topLeft = Offset(c * cellSize + 2f, r * cellSize + 2f),
                                    size = Size(cellSize - 4f, cellSize - 4f)
                                )
                            }
                        }
                    }

                    // Draw active piece
                    if (!isGameOver) {
                        current.cells.forEach { (r, c) ->
                            if (r >= 0) {
                                drawCell(this, current.type.color, c * cellSize, r * cellSize, cellSize)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // ── Sidebar ──
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Score
                    SidePanel(label = "SCORE") {
                        Text(
                            text = score.toString(),
                            color = Color(0xFF4ADE80),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SidePanel(label = "LEVEL") {
                        Text(
                            text = (level + 1).toString(),
                            color = Color(0xFF60A5FA),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SidePanel(label = "LINES") {
                        Text(
                            text = lines.toString(),
                            color = Color(0xFFFBBF24),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next piece preview
                    SidePanel(label = "NEXT") {
                        Canvas(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF0D1B2A), RoundedCornerShape(4.dp))
                        ) {
                            val cs = size.width / 4f
                            val cells = SHAPES[next.type]!![0]
                            val minR = cells.minOf { it.first }
                            val minC = cells.minOf { it.second }
                            cells.forEach { (dr, dc) ->
                                val r = dr - minR
                                val c = dc - minC
                                drawCell(this, next.type.color, c * cs, r * cs, cs, 1.5f)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Controls ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(label = "◀", icon = null, onClick = { moveLeft() })
                    ControlButton(label = "↺", icon = null, onClick = { rotate() })
                    ControlButton(label = "▶", icon = null, onClick = { moveRight() })
                }

                // Fast Drop — hold to drop fast
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E3A5F))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isFastDrop = true
                                    tryAwaitRelease()
                                    isFastDrop = false
                                }
                            )
                        }
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⬇  FAST DROP",
                        color = Color(0xFF60A5FA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Game Over Overlay ──
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .padding(36.dp)
                ) {
                    Text("GAME OVER", color = Color(0xFFF44336), fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("Score: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Lines: $lines", color = Color(0xFF94A3B8), fontSize = 16.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restart", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onBack) {
                            Text("Quit", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Pause Dialog ──
        if (showPauseDialog) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1E293B),
                title = {
                    Text("Game Paused", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                text = {
                    Text("Take a breather — your game is saved.", color = Color(0xFF94A3B8))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPauseDialog = false
                            isPaused = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("▶  Resume", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPauseDialog = false
                        onBack()
                    }) {
                        Text("Quit", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Reusable UI components
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SidePanel(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E3A5F))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onClick()
                    tryAwaitRelease()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(30.dp))
        } else {
            Text(text = label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
