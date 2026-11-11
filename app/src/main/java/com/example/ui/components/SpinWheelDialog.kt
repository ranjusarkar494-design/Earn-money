package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.GoldBanner
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WheelPrize(val text: String, val amount: Long, val color: Color)

val DefaultPrizes = listOf(
    WheelPrize("₹20", 20L, LudoRed),
    WheelPrize("₹50", 50L, LudoGreen),
    WheelPrize("₹10", 10L, LudoBlue),
    WheelPrize("₹100", 100L, GoldPrimary),
    WheelPrize("₹25", 25L, Color(0xFF9C27B0)),
    WheelPrize("₹5", 5L, LudoYellow),
    WheelPrize("₹200", 200L, Color(0xFFE91E63)),
    WheelPrize("₹15", 15L, Color(0xFF009688))
)

@Composable
fun SpinWheelDialog(
    onDismiss: () -> Unit,
    onRewardWon: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var wonPrize by remember { mutableStateOf<WheelPrize?>(null) }
    val rotation = remember { Animatable(0f) }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0A3C)),
            border = androidx.compose.foundation.BorderStroke(3.dp, GoldPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Lucky Wheel 🎡",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Spin daily to win real cash rewards & bonus tickets!",
                    color = GoldLight,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wheel Container with Pointer
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation.value)
                    ) {
                        drawWheelSegments(DefaultPrizes)
                    }

                    // Center Hub Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(GoldPrimary, Color(0xFF7B1FA2)))
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SPIN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    // Top Pointer
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, Color.White)
                        drawPath(path, GoldPrimary, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                wonPrize?.let { prize ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFE65100), GoldBanner))
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🎉 You Won ${prize.text} Cash Reward!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        if (!isSpinning && wonPrize == null) {
                            isSpinning = true
                            val targetIndex = (DefaultPrizes.indices).random()
                            val segmentAngle = 360f / DefaultPrizes.size
                            // Target angle brings segment to top (pointer at 270 deg or top)
                            val extraSpins = (5..8).random() * 360f
                            val targetAngle = extraSpins + (360f - (targetIndex * segmentAngle + segmentAngle / 2f))

                            coroutineScope.launch {
                                rotation.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = tween(3500, easing = FastOutSlowInEasing)
                                )
                                isSpinning = false
                                val selected = DefaultPrizes[targetIndex]
                                wonPrize = selected
                                onRewardWon(selected.amount)
                            }
                        } else if (wonPrize != null) {
                            onDismiss()
                        }
                    },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (wonPrize != null) "Claim Reward" else if (isSpinning) "Spinning..." else "Spin the Wheel Free!",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

fun DrawScope.drawWheelSegments(prizes: List<WheelPrize>) {
    val count = prizes.size
    val sweepAngle = 360f / count
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    for (i in prizes.indices) {
        val startAngle = i * sweepAngle - 90f
        drawArc(
            color = prizes[i].color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.4f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
    }

    // Outer border ring
    drawCircle(
        color = GoldPrimary,
        radius = radius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(6f),
        center = center
    )
}
