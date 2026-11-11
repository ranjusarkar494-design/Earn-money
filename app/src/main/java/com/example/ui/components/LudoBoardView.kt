package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerColor
import com.example.data.model.TokenState
import com.example.game.BoardCoord
import com.example.game.LudoEngine
import com.example.ui.theme.GoldBanner
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalPurpleDark
import kotlinx.coroutines.launch

fun getColorValue(color: PlayerColor): Color = when (color) {
    PlayerColor.RED -> LudoRed
    PlayerColor.GREEN -> LudoGreen
    PlayerColor.YELLOW -> LudoYellow
    PlayerColor.BLUE -> LudoBlue
}

@Composable
fun LudoBoardView(
    tokens: Map<PlayerColor, List<TokenState>>,
    currentTurnColor: PlayerColor,
    isLocalPlayerTurn: Boolean,
    diceRoll: Int,
    canRollDice: Boolean,
    isRollingDice: Boolean,
    onDiceRollClicked: () -> Unit,
    onTokenClicked: (TokenState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEDE8D9))
            .border(4.dp, Brush.verticalGradient(listOf(GoldPrimary, Color(0xFF6B21A8))), RoundedCornerShape(20.dp))
    ) {
        val boardSize = maxWidth
        val cellSize = boardSize / 15f

        // 1. Draw static board (Yards, paths, center triangles, stars)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLudoBoard(size.width, size.height)
        }

        // 2. Render Tokens on board
        tokens.forEach { (color, tokenList) ->
            tokenList.forEach { token ->
                val coord = LudoEngine.getBoardCoord(token)
                val canMove = isLocalPlayerTurn && currentTurnColor == color &&
                        diceRoll > 0 && LudoEngine.canMoveToken(token, diceRoll)

                val pulseScale = remember { Animatable(1f) }
                LaunchedEffect(canMove) {
                    if (canMove) {
                        pulseScale.animateTo(
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                    } else {
                        pulseScale.snapTo(1f)
                    }
                }

                // Multiple tokens in the same cell offset slightly
                val cellTokens = tokens.values.flatten().filter {
                    LudoEngine.getBoardCoord(it) == coord && it.step == token.step
                }
                val tokenIndexInCell = cellTokens.indexOf(token).coerceAtLeast(0)
                val offsetDx = if (cellTokens.size > 1) ((tokenIndexInCell % 2) * 6 - 3).dp else 0.dp
                val offsetDy = if (cellTokens.size > 1) ((tokenIndexInCell / 2) * 6 - 3).dp else 0.dp

                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .offset(
                            x = cellSize * coord.col + offsetDx,
                            y = cellSize * coord.row + offsetDy
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TokenPiece(
                        token = token,
                        isMovable = canMove,
                        pulseScale = pulseScale.value,
                        onClick = {
                            if (canMove) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(30)
                                }
                                onTokenClicked(token)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TokenPiece(
    token: TokenState,
    isMovable: Boolean,
    pulseScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = getColorValue(token.color)

    Box(
        modifier = modifier
            .size(26.dp)
            .scale(if (isMovable) pulseScale else 1f)
            .shadow(if (isMovable) 8.dp else 3.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, color, Color(0xFF100518))
                )
            )
            .border(
                width = if (isMovable) 2.5.dp else 1.5.dp,
                color = if (isMovable) GoldPrimary else Color.White,
                shape = CircleShape
            )
            .clickable(enabled = isMovable, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Crown insignia on pawn
        Text(
            text = "👑",
            fontSize = 11.sp
        )
    }
}

// Canvas drawing of standard classic 15x15 board matching screenshot
fun DrawScope.drawLudoBoard(width: Float, height: Float) {
    val cellW = width / 15f
    val cellH = height / 15f

    // 1. Four corner yards
    // Top-Left: Yellow
    drawYard(0f, 0f, cellW * 6, cellH * 6, LudoYellow, "YELLOW")
    // Top-Right: Green
    drawYard(cellW * 9, 0f, cellW * 6, cellH * 6, LudoGreen, "GREEN")
    // Bottom-Left: Red
    drawYard(0f, cellH * 9, cellW * 6, cellH * 6, LudoRed, "RED")
    // Bottom-Right: Blue
    drawYard(cellW * 9, cellH * 9, cellW * 6, cellH * 6, LudoBlue, "BLUE")

    // 2. Track cells grid
    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val inYard = (r < 6 && c < 6) || (r < 6 && c >= 9) || (r >= 9 && c < 6) || (r >= 9 && c >= 9)
            val inCenter = r in 6..8 && c in 6..8
            if (!inYard && !inCenter) {
                val left = c * cellW
                val top = r * cellH

                // Determine cell color:
                var fillColor = Color.White
                var hasStar = false
                var hasArrow = false
                var arrowColor = Color.Transparent

                // Home columns:
                if (r == 7 && c in 1..5) fillColor = LudoRed
                if (c == 7 && r in 1..5) fillColor = LudoGreen
                if (r == 7 && c in 9..13) fillColor = LudoBlue
                if (c == 7 && r in 9..13) fillColor = LudoYellow

                // Starting squares:
                if (r == 6 && c == 1) { fillColor = LudoRed; hasArrow = true; arrowColor = LudoRed }
                if (r == 1 && c == 8) { fillColor = LudoGreen; hasArrow = true; arrowColor = LudoGreen }
                if (r == 8 && c == 13) { fillColor = LudoBlue; hasArrow = true; arrowColor = LudoBlue }
                if (r == 13 && c == 6) { fillColor = LudoYellow; hasArrow = true; arrowColor = LudoYellow }

                // Safe star spots:
                if ((r == 6 && c == 2) || (r == 2 && c == 8) || (r == 8 && c == 12) || (r == 12 && c == 6) ||
                    (r == 8 && c == 2) || (r == 2 && c == 6) || (r == 6 && c == 12) || (r == 12 && c == 8)) {
                    hasStar = true
                }

                drawRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(cellW, cellH)
                )
                drawRect(
                    color = Color(0xFFD4C8B0),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(cellW, cellH),
                    style = Stroke(width = 1f)
                )

                if (hasStar) {
                    drawStar(left + cellW / 2, top + cellH / 2, cellW * 0.35f, Color(0xFFE6A100))
                }
            }
        }
    }

    // 3. Center home triangles (6..8, 6..8)
    val cx = cellW * 7.5f
    val cy = cellH * 7.5f
    val centerLeft = cellW * 6
    val centerTop = cellH * 6
    val centerRight = cellW * 9
    val centerBottom = cellH * 9

    // Green triangle (top)
    val greenPath = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(centerRight, centerTop)
        lineTo(cx, cy)
        close()
    }
    drawPath(greenPath, LudoGreen)

    // Blue triangle (right)
    val bluePath = Path().apply {
        moveTo(centerRight, centerTop)
        lineTo(centerRight, centerBottom)
        lineTo(cx, cy)
        close()
    }
    drawPath(bluePath, LudoBlue)

    // Yellow triangle (bottom)
    val yellowPath = Path().apply {
        moveTo(centerLeft, centerBottom)
        lineTo(centerRight, centerBottom)
        lineTo(cx, cy)
        close()
    }
    drawPath(yellowPath, LudoYellow)

    // Red triangle (left)
    val redPath = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(centerLeft, centerBottom)
        lineTo(cx, cy)
        close()
    }
    drawPath(redPath, LudoRed)
}

fun DrawScope.drawYard(x: Float, y: Float, w: Float, h: Float, color: Color, label: String) {
    // Outer colored block
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )

    // Inner lighter inset with 4 slots
    val pad = w * 0.16f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(x + pad, y + pad),
        size = androidx.compose.ui.geometry.Size(w - pad * 2, h - pad * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
    )

    // 4 circular pawn slots
    val slotRadius = w * 0.12f
    val innerW = w - pad * 2
    val innerH = h - pad * 2
    val slotOffsets = listOf(
        Offset(x + pad + innerW * 0.3f, y + pad + innerH * 0.3f),
        Offset(x + pad + innerW * 0.7f, y + pad + innerH * 0.3f),
        Offset(x + pad + innerW * 0.3f, y + pad + innerH * 0.7f),
        Offset(x + pad + innerW * 0.7f, y + pad + innerH * 0.7f)
    )

    slotOffsets.forEach { pos ->
        drawCircle(color = color, radius = slotRadius, center = pos)
        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = slotRadius * 0.5f, center = pos)
    }
}

fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    val innerRadius = radius * 0.45f
    val step = (Math.PI / points).toFloat()

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * step - (Math.PI / 2).toFloat()
        val x = cx + r * kotlin.math.cos(angle)
        val y = cy + r * kotlin.math.sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun AnimatedDiceView(
    diceRoll: Int,
    isRolling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            rotation.animateTo(
                targetValue = 720f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            rotation.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = if (enabled) listOf(Color.White, Color(0xFFF0F0F0)) else listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))
                )
            )
            .border(2.dp, if (enabled) GoldPrimary else Color.Gray, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !isRolling) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.rotate(rotation.value),
            contentAlignment = Alignment.Center
        ) {
            if (diceRoll in 1..6 && !isRolling) {
                DiceDotsView(diceRoll)
            } else {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Roll Dice",
                    tint = RoyalPurple,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
fun DiceDotsView(roll: Int) {
    val dotColor = Color(0xFF1B0733)
    val dotSize = 10.dp

    when (roll) {
        1 -> Box(Modifier.size(dotSize).clip(CircleShape).background(LudoRed))
        2 -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
        }
        3 -> Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
        }
        4 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            }
        }
        5 -> Box(contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                }
            }
            Box(Modifier.size(dotSize).clip(CircleShape).background(LudoRed))
        }
        6 -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            }
        }
    }
}
