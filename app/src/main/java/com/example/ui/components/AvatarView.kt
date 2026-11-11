package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed

data class AvatarOption(
    val id: String,
    val name: String,
    val emoji: String,
    val ringColor: Color
)

val BuiltInAvatars = listOf(
    AvatarOption("avatar_guy_smile", "Alex (Smile)", "👨‍💼", LudoRed),
    AvatarOption("avatar_girl_glasses", "Priya (Glasses)", "👩‍🏫", LudoGreen),
    AvatarOption("avatar_king", "Ludo King", "👑", GoldPrimary),
    AvatarOption("avatar_queen", "Ludo Queen", "👸", Color(0xFFE91E63)),
    AvatarOption("avatar_lion", "Golden Lion", "🦁", Color(0xFFFF9800)),
    AvatarOption("avatar_blitz", "Blitz Champion", "⚡", Color(0xFF00E5FF)),
    AvatarOption("avatar_guru", "Grandmaster", "🧙‍♂️", Color(0xFF9C27B0))
)

@Composable
fun PlayerAvatarView(
    avatarId: String,
    customUri: String? = null,
    borderColor: Color = GoldPrimary,
    size: Dp = 56.dp,
    borderWidth: Dp = 3.dp,
    showCrown: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(borderColor.copy(alpha = 0.8f), Color(0xFF1E0A38))
                    )
                )
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!customUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(customUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Player Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                val avatar = BuiltInAvatars.firstOrNull { it.id == avatarId }
                if (avatar != null) {
                    Text(
                        text = avatar.emoji,
                        fontSize = (size.value * 0.45f).sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }

        if (showCrown) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(size * 0.4f)
                    .clip(CircleShape)
                    .background(GoldPrimary)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = (size.value * 0.22f).sp)
            }
        }
    }
}
