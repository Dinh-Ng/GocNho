package com.dinh.gocnho.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

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
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, 1))
    ),
    TetrominoType.Z to listOf(
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(-1, 1), Pair(0, 0), Pair(0, 1), Pair(1, 0)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1)),
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

private fun spawnPiece(type: TetrominoType = TetrominoType.entries.random()): Piece =
    Piece(type = type, row = 1, col = BOARD_COLS / 2)

private fun isValid(board: Array<Array<Color?>>, piece: Piece): Boolean =
    piece.cells.all { (r, c) ->
        r in 0 until BOARD_ROWS && c in 0 until BOARD_COLS && board[r][c] == null
    }

private fun lockPiece(board: Array<Array<Color?>>, piece: Piece): Array<Array<Color?>> {
    val newBoard = board.map { it.copyOf() }.toTypedArray()
    piece.cells.forEach { (r, c) -> newBoard[r][c] = piece.type.color }
    return newBoard
}

private fun clearLines(board: Array<Array<Color?>>): Pair<Array<Array<Color?>>, Int> {
    val kept = board.filter { row -> row.any { it == null } }
    val cleared = BOARD_ROWS - kept.size
    if (cleared == 0) return Pair(board, 0)
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
// Canvas drawing helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun drawCell(
    canvas: androidx.compose.ui.graphics.drawscope.DrawScope,
    color: Color,
    left: Float,
    top: Float,
    size: Float,
    gap: Float = 2f
) {
    val l = left + gap; val t = top + gap; val s = size - gap * 2
    canvas.drawRect(color = color, topLeft = Offset(l, t), size = Size(s, s))
    canvas.drawRect(color = color.copy(alpha = 0.65f), topLeft = Offset(l, t), size = Size(s, 4f))
    canvas.drawRect(color = color.copy(alpha = 0.65f), topLeft = Offset(l, t), size = Size(4f, s))
    canvas.drawRect(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(l, t + s - 4f), size = Size(s, 4f))
    canvas.drawRect(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(l + s - 4f, t), size = Size(4f, s))
}

private fun drawEmptyCell(
    canvas: androidx.compose.ui.graphics.drawscope.DrawScope,
    left: Float, top: Float, size: Float
) {
    canvas.drawRect(
        color = Color(0xFF1A2744),
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f)
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Design tokens
// ──────────────────────────────────────────────────────────────────────────────

private val ConsoleBody      = Color(0xFF1E2127)   // charcoal console shell
private val ConsoleMid       = Color(0xFF252830)   // slightly lighter areas
private val ScreenBezel      = Color(0xFF0A0B0F)   // black LCD surround
private val ScreenBorder     = Color(0xFF2A2D3A)   // bezel ring
private val LcdBg            = Color(0xFF0D1117)   // LCD content background
private val SidebarBg        = Color(0xFF080C14)   // right-panel background
private val DPadColor        = Color(0xFF1A1D25)   // D-pad base
private val DPadArm          = Color(0xFF252B38)   // D-pad arms
private val RotateBtn        = Color(0xFFB71C1C)   // classic action-button red
private val RotateBtnHigh    = Color(0xFFE53935)
private val PillBg           = Color(0xFF2A2D3A)   // START / SELECT style
private val StatLabel        = Color(0xFF5A6070)
private val StatValue        = Color(0xFFE0E8F0)
private val AccentCyan       = Color(0xFF00BCD4)

// ──────────────────────────────────────────────────────────────────────────────
// TetrisScreen — Classic Handheld Console Layout
// ──────────────────────────────────────────────────────────────────────────────
//
//  ┌──────────────────────────────────────────────┐
//  │  ←          T E T R I S                      │   ← thin title strip
//  │  ┌────────────────────────────────────────┐  │
//  │  │  [BOARD 10×20 – left 65%] │ [SIDEBAR]  │  │   ← black screen frame
//  │  │                           │  NEXT       │  │
//  │  │                           │  SCORE      │  │
//  │  │                           │  LEVEL      │  │
//  │  │                           │  LINES      │  │
//  │  └────────────────────────────────────────┘  │
//  │  ─────────────── CONTROLS ──────────────────  │
//  │   ┌─ D-PAD ─┐              ┌─ ROTATE ─┐      │
//  │   │  ▲      │              │           │      │
//  │   │◄ · ►   │              │    ↺      │      │
//  │   │  ▼      │              │           │      │
//  │   └─────────┘              └───────────┘      │
//  │       ┌──── RESET ────┐  ┌──── PAUSE ────┐   │
//  └──────────────────────────────────────────────┘

@Composable
fun TetrisScreen(onBack: () -> Unit) {

    // ── Game state (unchanged) ──────────────────────────────────────────────
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

    fun resetGame() {
        board = Array(BOARD_ROWS) { arrayOfNulls<Color?>(BOARD_COLS) }
        current = spawnPiece(); next = spawnPiece()
        score = 0; lines = 0; level = 0
        isPaused = false; isGameOver = false; isFastDrop = false; showPauseDialog = false
    }

    fun lockAndAdvance() {
        val locked = lockPiece(board, current)
        val (clearedBoard, numCleared) = clearLines(locked)
        board = clearedBoard; lines += numCleared
        score += scoreForLines(numCleared, level); level = lines / 10
        val candidate = next; next = spawnPiece()
        if (!isValid(board, candidate)) isGameOver = true else current = candidate
    }

    fun moveLeft()  { if (!isPaused && !isGameOver) { val m = current.copy(col = current.col - 1); if (isValid(board, m)) current = m } }
    fun moveRight() { if (!isPaused && !isGameOver) { val m = current.copy(col = current.col + 1); if (isValid(board, m)) current = m } }
    fun rotate()    {
        if (isPaused || isGameOver) return
        val r = current.copy(rotation = (current.rotation + 1) % 4)
        val k = listOf(0, -1, 1, -2, 2).map { r.copy(col = r.col + it) }.firstOrNull { isValid(board, it) }
        if (k != null) current = k
    }
    fun dropOne()   {
        if (isPaused || isGameOver) return
        val d = current.copy(row = current.row + 1)
        if (isValid(board, d)) current = d else lockAndAdvance()
    }

    LaunchedEffect(isPaused, isGameOver, level, isFastDrop) {
        while (!isPaused && !isGameOver) {
            delay(if (isFastDrop) 50L else gravityMs(level))
            dropOne()
        }
    }

    // Ghost piece
    val ghost: Piece = run {
        var g = current
        while (isValid(board, g.copy(row = g.row + 1))) g = g.copy(row = g.row + 1)
        g
    }

    // ── Root: full-screen console body ──────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(ConsoleBody)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Title strip ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181F))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF8899AA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "T E T R I S",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
                // Spacer to balance the back button
                Spacer(modifier = Modifier.size(36.dp))
            }

            // ── Screen Frame ─────────────────────────────────────────────────
            // Outer bezel (raised-edge effect using nested boxes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)           // ~52% of remaining height
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScreenBezel)
                    .border(2.dp, ScreenBorder, RoundedCornerShape(12.dp))
            ) {
                // Inner screen padding (bezel inset)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    // ── BOARD (left, ~65% of screen width) ──────────────────
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(BOARD_COLS.toFloat() / BOARD_ROWS.toFloat())
                            .background(LcdBg)
                    ) {
                        val cs = size.width / BOARD_COLS
                        for (r in 0 until BOARD_ROWS) {
                            for (c in 0 until BOARD_COLS) {
                                val col = board[r][c]
                                if (col != null) drawCell(this, col, c * cs, r * cs, cs)
                                else drawEmptyCell(this, c * cs, r * cs, cs)
                            }
                        }
                        if (!isGameOver) {
                            ghost.cells.forEach { (r, c) ->
                                if (r >= 0) drawRect(
                                    color = current.type.color.copy(alpha = 0.18f),
                                    topLeft = Offset(c * cs + 2f, r * cs + 2f),
                                    size = Size(cs - 4f, cs - 4f)
                                )
                            }
                            current.cells.forEach { (r, c) ->
                                if (r >= 0) drawCell(this, current.type.color, c * cs, r * cs, cs)
                            }
                        }
                    }

                    // ── Vertical divider ─────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, ScreenBorder, Color.Transparent)
                                )
                            )
                    )

                    // ── SIDEBAR (right panel) ─────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(SidebarBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // NEXT preview
                        Text(
                            text = "NEXT",
                            color = StatLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LcdBg)
                                .border(1.dp, ScreenBorder, RoundedCornerShape(4.dp))
                        ) {
                            val cells = SHAPES[next.type]!![0]
                            val minR = cells.minOf { it.first }; val minC = cells.minOf { it.second }
                            val maxR = cells.maxOf { it.first }; val maxC = cells.maxOf { it.second }
                            val spanR = (maxR - minR + 1).coerceAtLeast(1)
                            val spanC = (maxC - minC + 1).coerceAtLeast(1)
                            val cellSz = minOf(size.width / (spanC + 1f), size.height / (spanR + 1f))
                            val ox = (size.width - spanC * cellSz) / 2f
                            val oy = (size.height - spanR * cellSz) / 2f
                            cells.forEach { (dr, dc) ->
                                drawCell(this, next.type.color, ox + (dc - minC) * cellSz, oy + (dr - minR) * cellSz, cellSz, 1.5f)
                            }
                        }

                        // Divider line
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ScreenBorder))

                        // SCORE
                        SidebarStat(label = "SCORE", value = String.format(Locale.US, "%,d", score), valueColor = Color(0xFF4ADE80))

                        // LEVEL
                        SidebarStat(label = "LEVEL", value = (level + 1).toString(), valueColor = Color(0xFF60A5FA))

                        // LINES
                        SidebarStat(label = "LINES", value = lines.toString(), valueColor = Color(0xFFFBBF24))
                    }
                }
            }

            // ── CONTROL PANEL ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF191C23), ConsoleBody)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // D-Pad — left side, centered vertically
                DPad(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(bottom = 36.dp),
                    onLeft  = { moveLeft() },
                    onRight = { moveRight() },
                    onUp    = { rotate() },       // D-pad Up = alternate rotate
                    onFastDropStart = { isFastDrop = true },
                    onFastDropEnd   = { isFastDrop = false }
                )

                // Rotate action button — right side, centered vertically
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 36.dp, end = 8.dp)
                ) {
                    // Outer shadow ring
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6A0000))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(RotateBtnHigh, RotateBtn, Color(0xFF7B1010))
                                    )
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(onPress = {
                                        rotate()
                                        tryAwaitRelease()
                                    })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↺",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Label below button
                    Text(
                        text = "ROTATE",
                        color = StatLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(top = 84.dp)
                    )
                }

                // ── Pill buttons: RESET + PAUSE ── centered at bottom ────────
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(label = "RESET", onClick = { resetGame() })
                    PillButton(
                        label = "PAUSE",
                        onClick = {
                            if (!isGameOver) {
                                isPaused = !isPaused
                                if (isPaused) showPauseDialog = true
                            }
                        }
                    )
                }
            }
        }

        // ── Game Over Overlay ────────────────────────────────────────────────
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, ScreenBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 40.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        color = Color(0xFFF44336),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", score),
                        color = Color(0xFF4ADE80),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(text = "Lines: $lines  ·  Level: ${level + 1}", color = Color(0xFF64748B), fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restart", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onBack) {
                            Text("Quit", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Pause Dialog ─────────────────────────────────────────────────────
        if (showPauseDialog) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1E293B),
                title = { Text("Paused", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                text = { Text("Game paused. Press Resume to continue.", color = Color(0xFF94A3B8)) },
                confirmButton = {
                    Button(
                        onClick = { showPauseDialog = false; isPaused = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("▶  Resume", color = Color.White, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showPauseDialog = false; onBack() }) {
                        Text("Quit", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// D-Pad component (cross shape)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DPad(
    modifier: Modifier = Modifier,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onUp: () -> Unit,
    onFastDropStart: () -> Unit,
    onFastDropEnd: () -> Unit,
    armSize: Dp = 44.dp,
    centerSize: Dp = 40.dp
) {
    val totalSize = armSize * 3

    Column(
        modifier = modifier.size(totalSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Up
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            DPadArm(size = armSize, shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)) {
                onUp()
            }
        }
        // Row 2: Left – Center – Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadArm(size = armSize, shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)) {
                onLeft()
            }
            // Center non-interactive piece
            Box(
                modifier = Modifier
                    .size(centerSize)
                    .background(DPadArm)
            )
            DPadArm(size = armSize, shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)) {
                onRight()
            }
        }
        // Row 3: Down (hold = fast drop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(armSize)
                    .background(
                        color = DPadArm,
                        shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            onFastDropStart()
                            tryAwaitRelease()
                            onFastDropEnd()
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▼", color = Color(0xFF8899AA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Label below D-pad (rendered outside the size-constrained Column via Box parent)
}

@Composable
private fun DPadArm(size: Dp, shape: RoundedCornerShape, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = DPadArm, shape = shape)
            .clip(shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onClick()
                    tryAwaitRelease()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        // Arrow glyphs: ▲ ▼ ◄ ► determined by caller context — blank here, shape implies direction
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sidebar stat row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SidebarStat(label: String, value: String, valueColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = StatLabel,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Pill button (RESET / PAUSE)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2A2D3A), Color(0xFF1E2128), Color(0xFF2A2D3A))
                )
            )
            .border(1.dp, Color(0xFF3A3D4A), RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onClick()
                    tryAwaitRelease()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF8899AA),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
