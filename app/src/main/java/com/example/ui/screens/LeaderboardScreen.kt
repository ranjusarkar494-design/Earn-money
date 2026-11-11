package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.PlayerAvatarView
import com.example.ui.theme.GoldBanner
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val avatarId: String,
    val winningsEarned: Long,
    val gamesWon: Long
)

val SampleLeaderboard = listOf(
    LeaderboardEntry(1, "Rajesh Kumar", "avatar_king", 18450L, 84L),
    LeaderboardEntry(2, "Priya Sharma", "avatar_girl_glasses", 14200L, 62L),
    LeaderboardEntry(3, "Vikram Blitz", "avatar_blitz", 11950L, 53L),
    LeaderboardEntry(4, "Amit Verma", "avatar_guy_smile", 9400L, 41L),
    LeaderboardEntry(5, "Sunita Queen", "avatar_queen", 8150L, 38L),
    LeaderboardEntry(6, "Rahul Roy", "avatar_lion", 6900L, 32L),
    LeaderboardEntry(7, "Sneha Guru", "avatar_guru", 5400L, 25L),
    LeaderboardEntry(8, "Karan Singh", "avatar_guy_smile", 4800L, 22L)
)

@Composable
fun LeaderboardScreen(
    userProfile: UserProfile,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily, 1: Weekly, 2: All Time

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF260B4E), Color(0xFF14032C), Color(0xFF0C011B))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Champions Leaderboard 🏆",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daily", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Weekly", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("All Time", fontWeight = FontWeight.Bold) })
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Top 3 Podium
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank 2 (Silver)
                PodiumColumn(
                    entry = SampleLeaderboard[1],
                    medalEmoji = "🥈",
                    ringColor = Color(0xFFC0C0C0),
                    columnHeight = 90.dp
                )

                // Rank 1 (Gold - Taller)
                PodiumColumn(
                    entry = SampleLeaderboard[0],
                    medalEmoji = "👑",
                    ringColor = GoldPrimary,
                    columnHeight = 115.dp
                )

                // Rank 3 (Bronze)
                PodiumColumn(
                    entry = SampleLeaderboard[2],
                    medalEmoji = "🥉",
                    ringColor = Color(0xFFCD7F32),
                    columnHeight = 75.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Other champions list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(SampleLeaderboard.drop(3)) { idx, entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF220C47))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${entry.rank}",
                                color = GoldLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.width(36.dp)
                            )
                            PlayerAvatarView(
                                avatarId = entry.avatarId,
                                borderColor = GoldPrimary,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${entry.gamesWon} Games Won", color = Color.Gray, fontSize = 11.sp)
                            }
                            Text(
                                text = "₹${entry.winningsEarned}",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Your Rank sticky bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B156E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatarView(
                            avatarId = userProfile.avatarId,
                            customUri = userProfile.customAvatarUri.takeIf { it.isNotBlank() },
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("${userProfile.displayName} (You)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Won: ${userProfile.gamesWon} matches", color = GoldLight, fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rank #12", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("₹${userProfile.winningsBalance}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    entry: LeaderboardEntry,
    medalEmoji: String,
    ringColor: Color,
    columnHeight: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            PlayerAvatarView(
                avatarId = entry.avatarId,
                borderColor = ringColor,
                size = 54.dp,
                borderWidth = 3.dp
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(medalEmoji, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(entry.name.split(" ").first(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("₹${entry.winningsEarned}", color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(85.dp)
                .height(columnHeight)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(
                    Brush.verticalGradient(listOf(ringColor.copy(alpha = 0.6f), Color(0xFF1E0738)))
                )
                .border(1.dp, ringColor.copy(alpha = 0.5f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("#${entry.rank}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}
