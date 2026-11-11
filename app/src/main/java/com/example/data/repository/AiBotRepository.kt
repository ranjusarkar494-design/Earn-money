package com.example.data.repository

import android.util.Log
import com.example.data.model.PlayerColor
import com.example.data.model.TokenState
import com.example.game.LudoEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

class AiBotRepository {
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    // Strategic move selection based on heuristic priorities
    fun selectBestMove(
        botTokens: List<TokenState>,
        allTokens: Map<PlayerColor, List<TokenState>>,
        diceRoll: Int,
        personality: String = "Aggressive"
    ): TokenState? {
        val movable = botTokens.filter { LudoEngine.canMoveToken(it, diceRoll) }
        if (movable.isEmpty()) return null
        if (movable.size == 1) return movable.first()

        // 1. Check if any move captures an opponent
        for (token in movable) {
            val simMove = LudoEngine.executeMove(allTokens, token, diceRoll)
            if (simMove.capturedToken != null) {
                return token
            }
        }

        // 2. Check if any move reaches Home Finish (step 57)
        val homeReaching = movable.firstOrNull { it.step + diceRoll == 57 }
        if (homeReaching != null) return homeReaching

        // 3. If rolled 6, prioritize bringing token out of yard
        if (diceRoll == 6) {
            val yardToken = movable.firstOrNull { it.isInYard }
            if (yardToken != null) return yardToken
        }

        // 4. Move token into safe star tile
        for (token in movable) {
            val nextStep = if (token.isInYard) 0 else token.step + diceRoll
            val simToken = token.copy(step = nextStep)
            if (LudoEngine.isSafeSquare(simToken)) {
                return token
            }
        }

        // 5. Default by personality:
        return if (personality == "SpeedRunner") {
            // Pick token closest to finish
            movable.maxByOrNull { it.step } ?: movable.first()
        } else {
            // Aggressive or Balanced: Advance tokens on track
            movable.filter { it.isOnTrack }.maxByOrNull { it.step }
                ?: movable.first()
        }
    }

    // Dynamic AI Opponent banter via Firebase AI Logic
    suspend fun generateBotCommentary(
        botName: String,
        personality: String,
        eventDescription: String
    ): String {
        return try {
            withTimeout(15_000L) {
                val prompt = "You are $botName, a charismatic, witty, $personality Ludo AI champion. " +
                        "The game event just happened: '$eventDescription'. " +
                        "Give a short, entertaining 1-sentence in-game chat reaction (under 12 words) to the player."
                val response = model.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) text else getDefaultBanter(eventDescription)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AiBot", "AI banter fallback: ${e.message}")
            getDefaultBanter(eventDescription)
        }
    }

    private fun getDefaultBanter(event: String): String {
        return when {
            event.contains("rolled 6", ignoreCase = true) -> "A 6?! Fortune favors the bold! 🎲"
            event.contains("captured", ignoreCase = true) -> "Ouch! That pawn had a family! 👑"
            event.contains("reached home", ignoreCase = true) -> "One step closer to victory! 🏆"
            event.contains("won", ignoreCase = true) -> "What a legendary match! GG! 💰"
            else -> "The game is on! Let's roll!"
        }
    }
}
