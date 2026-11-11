package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatMessage
import com.example.data.model.PlayerColor
import com.example.data.model.TokenState
import com.example.data.model.UserProfile
import com.example.data.repository.AiBotRepository
import com.example.game.LudoEngine
import com.example.ui.components.AnimatedDiceView
import com.example.ui.components.LudoBoardView
import com.example.ui.components.PlayerAvatarView
import com.example.ui.theme.GoldBanner
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoRed
import com.example.ui.theme.RoyalPurpleDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    userProfile: UserProfile,
    gameMode: String,
    botPersonality: String = "Aggressive",
    prizePool: Long = 90L,
    onBackClicked: () -> Unit,
    onRewardEarned: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val aiBotRepo = remember { AiBotRepository() }

    // Tokens state for RED (Player) and GREEN (Bot / Opponent)
    var tokens by remember {
        mutableStateOf(
            mapOf(
                PlayerColor.RED to listOf(
                    TokenState(0, PlayerColor.RED, -1),
                    TokenState(1, PlayerColor.RED, -1),
                    TokenState(2, PlayerColor.RED, -1),
                    TokenState(3, PlayerColor.RED, -1)
                ),
                PlayerColor.GREEN to listOf(
                    TokenState(0, PlayerColor.GREEN, -1),
                    TokenState(1, PlayerColor.GREEN, -1),
                    TokenState(2, PlayerColor.GREEN, -1),
                    TokenState(3, PlayerColor.GREEN, -1)
                )
            )
        )
    }

    var currentTurn by remember { mutableStateOf(PlayerColor.RED) }
    var diceRoll by remember { mutableIntStateOf(0) }
    var isRollingDice by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<PlayerColor?>(null) }
    var aiCommentary by remember { mutableStateOf("Match started! Let's see who claims the prize! 👑") }
    var floatingReaction by remember { mutableStateOf<String?>(null) }

    // In-game Chat state
    var showChatDialog by remember { mutableStateOf(false) }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    messageId = "1",
                    senderId = "system",
                    senderName = "Host",
                    avatarId = "avatar_king",
                    text = "Welcome to Ludo Royale! Win ₹$prizePool!"
                )
            )
        )
    }
    var chatInput by remember { mutableStateOf("") }

    val isLocalTurn = currentTurn == PlayerColor.RED && winner == null
    val playerTokens = tokens[PlayerColor.RED] ?: emptyList()
    val botTokens = tokens[PlayerColor.GREEN] ?: emptyList()

    // Can roll dice when it's local turn and dice hasn't been rolled yet, or after turn switch
    var hasRolledForCurrentTurn by remember { mutableStateOf(false) }
    val canRollDice = isLocalTurn && !hasRolledForCurrentTurn && !isRollingDice

    // Handle Bot turn automatically
    LaunchedEffect(currentTurn) {
        if (currentTurn == PlayerColor.GREEN && winner == null) {
            hasRolledForCurrentTurn = false
            delay(900)
            isRollingDice = true
            delay(500)
            val roll = (1..6).random()
            diceRoll = roll
            isRollingDice = false

            delay(600)
            val botChosenToken = aiBotRepo.selectBestMove(
                botTokens = tokens[PlayerColor.GREEN] ?: emptyList(),
                allTokens = tokens,
                diceRoll = roll,
                personality = botPersonality
            )

            if (botChosenToken != null) {
                val moveResult = LudoEngine.executeMove(tokens, botChosenToken, roll)
                tokens = moveResult.updatedTokens

                // Trigger commentary if capture or home
                if (moveResult.capturedToken != null) {
                    coroutineScope.launch {
                        aiCommentary = aiBotRepo.generateBotCommentary("Bot", botPersonality, "captured player pawn")
                    }
                } else if (roll == 6) {
                    coroutineScope.launch {
                        aiCommentary = aiBotRepo.generateBotCommentary("Bot", botPersonality, "rolled 6")
                    }
                }

                // Check Bot win
                if (LudoEngine.hasPlayerWon(tokens[PlayerColor.GREEN] ?: emptyList())) {
                    winner = PlayerColor.GREEN
                    return@LaunchedEffect
                }

                if (!moveResult.extraTurn) {
                    currentTurn = PlayerColor.RED
                    diceRoll = 0
                }
            } else {
                // No valid moves for bot
                delay(400)
                currentTurn = PlayerColor.RED
                diceRoll = 0
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF280B54), Color(0xFF140328), Color(0xFF0C0119))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Prize Pool Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFFE5A600)))
                        )
                        .border(1.5.dp, GoldPrimary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prize: ₹$prizePool",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                // Chat button with badge
                IconButton(onClick = { showChatDialog = true }) {
                    Box {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // AI Opponent Status (Green Player - Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatarView(
                        avatarId = "avatar_girl_glasses",
                        borderColor = if (currentTurn == PlayerColor.GREEN) GoldPrimary else LudoGreen,
                        size = 52.dp,
                        borderWidth = if (currentTurn == PlayerColor.GREEN) 4.dp else 2.dp,
                        showCrown = currentTurn == PlayerColor.GREEN
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (gameMode == "bot") "AI $botPersonality" else "Opponent (Priya)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LudoGreen)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("GREEN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = if (currentTurn == PlayerColor.GREEN) "Thinking move..." else "Waiting turn",
                            color = if (currentTurn == PlayerColor.GREEN) GoldLight else Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                if (currentTurn == PlayerColor.GREEN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (diceRoll > 0) "Rolled $diceRoll" else "Rolling...",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // AI Commentary Bubble
            AnimatedVisibility(
                visible = aiCommentary.isNotBlank(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF38146E))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiCommentary,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Central Ludo Board View
            LudoBoardView(
                tokens = tokens,
                currentTurnColor = currentTurn,
                isLocalPlayerTurn = isLocalTurn,
                diceRoll = diceRoll,
                canRollDice = canRollDice,
                isRollingDice = isRollingDice,
                onDiceRollClicked = {
                    if (canRollDice) {
                        hasRolledForCurrentTurn = true
                        isRollingDice = true
                        coroutineScope.launch {
                            delay(500)
                            val roll = (1..6).random()
                            diceRoll = roll
                            isRollingDice = false

                            // Check if player has any valid moves
                            val validMoves = playerTokens.any { LudoEngine.canMoveToken(it, roll) }
                            if (!validMoves) {
                                delay(900)
                                currentTurn = PlayerColor.GREEN
                                diceRoll = 0
                                hasRolledForCurrentTurn = false
                            }
                        }
                    }
                },
                onTokenClicked = { selectedToken ->
                    if (isLocalTurn && diceRoll > 0 && LudoEngine.canMoveToken(selectedToken, diceRoll)) {
                        val moveResult = LudoEngine.executeMove(tokens, selectedToken, diceRoll)
                        tokens = moveResult.updatedTokens

                        // Check Win
                        if (LudoEngine.hasPlayerWon(tokens[PlayerColor.RED] ?: emptyList())) {
                            winner = PlayerColor.RED
                            onRewardEarned(prizePool)
                            return@LudoBoardView
                        }

                        if (!moveResult.extraTurn) {
                            currentTurn = PlayerColor.GREEN
                            diceRoll = 0
                            hasRolledForCurrentTurn = false
                        } else {
                            // Extra turn for 6 or capture!
                            diceRoll = 0
                            hasRolledForCurrentTurn = false
                        }
                    }
                },
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Local Player Status (Red Player - Bottom) & Dice Controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Local player avatar & name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatarView(
                        avatarId = userProfile.avatarId,
                        customUri = userProfile.customAvatarUri.takeIf { it.isNotBlank() },
                        borderColor = if (isLocalTurn) GoldPrimary else LudoRed,
                        size = 54.dp,
                        borderWidth = if (isLocalTurn) 4.dp else 2.dp,
                        showCrown = isLocalTurn
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${userProfile.displayName} (You)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LudoRed)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("RED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = if (isLocalTurn) {
                                if (diceRoll == 0) "Your Turn! Tap Dice to Roll" else "Tap a glowing token to move!"
                            } else "Waiting for opponent...",
                            color = if (isLocalTurn) GoldLight else Color.LightGray,
                            fontWeight = if (isLocalTurn) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }

                // Interactive 3D Animated Dice
                AnimatedDiceView(
                    diceRoll = diceRoll,
                    isRolling = isRollingDice,
                    enabled = canRollDice,
                    onClick = {
                        if (canRollDice) {
                            hasRolledForCurrentTurn = true
                            isRollingDice = true
                            coroutineScope.launch {
                                delay(500)
                                val roll = (1..6).random()
                                diceRoll = roll
                                isRollingDice = false

                                val validMoves = playerTokens.any { LudoEngine.canMoveToken(it, roll) }
                                if (!validMoves) {
                                    delay(900)
                                    currentTurn = PlayerColor.GREEN
                                    diceRoll = 0
                                    hasRolledForCurrentTurn = false
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick emoji reaction bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val emojis = listOf("👑", "🔥", "🎲", "😂", "🏆", "👏")
                emojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                floatingReaction = emoji
                                chatMessages = chatMessages + ChatMessage(
                                    messageId = "${System.currentTimeMillis()}",
                                    senderName = userProfile.displayName,
                                    text = emoji
                                )
                            }
                            .padding(8.dp)
                    ) {
                        Text(emoji, fontSize = 18.sp)
                    }
                }
            }
        }

        // Floating reaction popup
        floatingReaction?.let { reaction ->
            LaunchedEffect(reaction) {
                delay(1400)
                floatingReaction = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Text(reaction, fontSize = 48.sp)
            }
        }

        // In-game chat popup dialog
        if (showChatDialog) {
            Dialog(onDismissRequest = { showChatDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0A3C))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Match Live Chat",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            IconButton(onClick = { showChatDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        // Quick canned phrases
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickPhrases = listOf("Good luck!", "Well played!", "Hurry up!", "Nice move!")
                            quickPhrases.forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF38146E))
                                        .clickable {
                                            chatMessages = chatMessages + ChatMessage(
                                                messageId = "${System.currentTimeMillis()}",
                                                senderName = userProfile.displayName,
                                                text = phrase
                                            )
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(phrase, color = GoldLight, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Message list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(chatMessages) { msg ->
                                val isMe = msg.senderName == userProfile.displayName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isMe) GoldPrimary else Color(0xFF31155A))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = msg.senderName,
                                                color = if (isMe) Color.Black else GoldLight,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = msg.text,
                                                color = if (isMe) Color.Black else Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Message input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Type message...", color = Color.Gray, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        chatMessages = chatMessages + ChatMessage(
                                            messageId = "${System.currentTimeMillis()}",
                                            senderName = userProfile.displayName,
                                            text = chatInput
                                        )
                                        chatInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = GoldPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Win Celebratory Modal
        winner?.let { winColor ->
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0A3C)),
                    border = androidx.compose.foundation.BorderStroke(3.dp, GoldPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏆", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (winColor == PlayerColor.RED) "VICTORY!" else "GAME OVER",
                            color = if (winColor == PlayerColor.RED) GoldPrimary else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (winColor == PlayerColor.RED)
                                "Congratulations! You defeated the opponent!"
                            else
                                "Better luck next time! Keep challenging the AI!",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (winColor == PlayerColor.RED) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFFE65100), GoldBanner))
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "+₹$prizePool Real Cash Rewards Credited!",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onBackClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Collect & Return to Lobby", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
