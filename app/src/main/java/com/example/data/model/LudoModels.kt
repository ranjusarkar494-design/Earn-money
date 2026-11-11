package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "Player",
    val avatarId: String = "avatar_guy_smile",
    val customAvatarUri: String = "",
    val coinBalance: Long = 500L,
    val winningsBalance: Long = 100L,
    val gamesWon: Long = 0L,
    val gamesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GameMatch(
    val matchId: String = "",
    val creatorId: String = "",
    val status: String = "waiting", // 'waiting', 'in_progress', 'completed', 'cancelled'
    val gameMode: String = "bot", // 'bot', 'multiplayer_quick', 'room_code'
    val entryFee: Long = 50L,
    val prizePool: Long = 90L,
    val playerIds: List<String> = emptyList(),
    val winnerId: String? = null,
    val currentTurnPlayerId: String = "",
    val lastDiceRoll: Long = 0L,
    val boardStateJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    val messageId: String = "",
    val matchId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val avatarId: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WalletTransaction(
    val transactionId: String = "",
    val userId: String = "",
    val type: String = "deposit", // 'deposit', 'withdrawal', 'reward_win', 'match_entry'
    val amount: Long = 0L,
    val status: String = "success", // 'success', 'pending', 'processing'
    val paymentMethod: String = "UPI",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class PlayerColor(val displayName: String) {
    RED("Red"),
    GREEN("Green"),
    YELLOW("Yellow"),
    BLUE("Blue")
}

data class TokenState(
    val id: Int = 0,
    val color: PlayerColor = PlayerColor.RED,
    val step: Int = -1 // -1 = Yard/Base, 0..50 = Common track, 51..56 = Home column, 57 = Home finish
) {
    val isHome: Boolean get() = step >= 57
    val isInYard: Boolean get() = step == -1
    val isOnTrack: Boolean get() = step in 0..56
}

data class PlayerSlot(
    val playerId: String,
    val displayName: String,
    val avatarId: String,
    val color: PlayerColor,
    val isBot: Boolean = false,
    val botPersonality: String = "Balanced"
)
