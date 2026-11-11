package com.example.game

import com.example.data.model.PlayerColor
import com.example.data.model.TokenState

data class BoardCoord(val row: Int, val col: Int)

object LudoEngine {
    // 52 common track squares mapped to (row, col) in a 15x15 grid
    // Starting with Red's start square at (row 6, col 1) moving clockwise:
    val commonTrackCoords: List<BoardCoord> = listOf(
        // Red start and arm moving right towards top
        BoardCoord(6, 1), BoardCoord(6, 2), BoardCoord(6, 3), BoardCoord(6, 4), BoardCoord(6, 5),
        BoardCoord(5, 6), BoardCoord(4, 6), BoardCoord(3, 6), BoardCoord(2, 6), BoardCoord(1, 6), BoardCoord(0, 6),
        BoardCoord(0, 7), // top turn
        BoardCoord(0, 8), BoardCoord(1, 8), BoardCoord(2, 8), BoardCoord(3, 8), BoardCoord(4, 8), BoardCoord(5, 8),
        BoardCoord(6, 9), BoardCoord(6, 10), BoardCoord(6, 11), BoardCoord(6, 12), BoardCoord(6, 13), BoardCoord(6, 14),
        BoardCoord(7, 14), // right turn
        BoardCoord(8, 14), BoardCoord(8, 13), BoardCoord(8, 12), BoardCoord(8, 11), BoardCoord(8, 10), BoardCoord(8, 9),
        BoardCoord(9, 8), BoardCoord(10, 8), BoardCoord(11, 8), BoardCoord(12, 8), BoardCoord(13, 8), BoardCoord(14, 8),
        BoardCoord(14, 7), // bottom turn
        BoardCoord(14, 6), BoardCoord(13, 6), BoardCoord(12, 6), BoardCoord(11, 6), BoardCoord(10, 6), BoardCoord(9, 6),
        BoardCoord(8, 5), BoardCoord(8, 4), BoardCoord(8, 3), BoardCoord(8, 2), BoardCoord(8, 1), BoardCoord(8, 0),
        BoardCoord(7, 0) // left turn back to (6, 1)
    )

    // Track offsets for each color
    // Red starts at index 0 (row 6, col 1)
    // Green starts at index 12 (row 0, col 8)
    // Blue starts at index 26 (row 8, col 14)
    // Yellow starts at index 38 (row 14, col 6)
    fun getStartOffset(color: PlayerColor): Int = when (color) {
        PlayerColor.RED -> 0
        PlayerColor.GREEN -> 12
        PlayerColor.BLUE -> 26
        PlayerColor.YELLOW -> 38
    }

    // Home columns leading into the center (7, 7)
    val redHomeColumn = listOf(
        BoardCoord(7, 1), BoardCoord(7, 2), BoardCoord(7, 3), BoardCoord(7, 4), BoardCoord(7, 5), BoardCoord(7, 6)
    )
    val greenHomeColumn = listOf(
        BoardCoord(1, 7), BoardCoord(2, 7), BoardCoord(3, 7), BoardCoord(4, 7), BoardCoord(5, 7), BoardCoord(6, 7)
    )
    val blueHomeColumn = listOf(
        BoardCoord(7, 13), BoardCoord(7, 12), BoardCoord(7, 11), BoardCoord(7, 10), BoardCoord(7, 9), BoardCoord(7, 8)
    )
    val yellowHomeColumn = listOf(
        BoardCoord(13, 7), BoardCoord(12, 7), BoardCoord(11, 7), BoardCoord(10, 7), BoardCoord(9, 7), BoardCoord(8, 7)
    )

    // Base/Yard positions for 4 tokens in each corner
    fun getYardCoord(color: PlayerColor, tokenIndex: Int): BoardCoord = when (color) {
        PlayerColor.YELLOW -> when (tokenIndex) {
            0 -> BoardCoord(2, 2); 1 -> BoardCoord(2, 3); 2 -> BoardCoord(3, 2); else -> BoardCoord(3, 3)
        }
        PlayerColor.GREEN -> when (tokenIndex) {
            0 -> BoardCoord(2, 11); 1 -> BoardCoord(2, 12); 2 -> BoardCoord(3, 11); else -> BoardCoord(3, 12)
        }
        PlayerColor.RED -> when (tokenIndex) {
            0 -> BoardCoord(11, 2); 1 -> BoardCoord(11, 3); 2 -> BoardCoord(12, 2); else -> BoardCoord(12, 3)
        }
        PlayerColor.BLUE -> when (tokenIndex) {
            0 -> BoardCoord(11, 11); 1 -> BoardCoord(11, 12); 2 -> BoardCoord(12, 11); else -> BoardCoord(12, 12)
        }
    }

    // Safe track positions where captures cannot happen (star tiles + start tiles)
    val safeTrackIndices = setOf(0, 8, 12, 20, 26, 34, 38, 46)

    fun getBoardCoord(token: TokenState): BoardCoord {
        if (token.step == -1) {
            return getYardCoord(token.color, token.id)
        }
        if (token.step in 0..50) {
            val globalIdx = (getStartOffset(token.color) + token.step) % commonTrackCoords.size
            return commonTrackCoords[globalIdx]
        }
        if (token.step in 51..56) {
            val homeIdx = token.step - 51
            return when (token.color) {
                PlayerColor.RED -> redHomeColumn.getOrElse(homeIdx) { BoardCoord(7, 6) }
                PlayerColor.GREEN -> greenHomeColumn.getOrElse(homeIdx) { BoardCoord(6, 7) }
                PlayerColor.BLUE -> blueHomeColumn.getOrElse(homeIdx) { BoardCoord(7, 8) }
                PlayerColor.YELLOW -> yellowHomeColumn.getOrElse(homeIdx) { BoardCoord(8, 7) }
            }
        }
        // Home Finished
        return BoardCoord(7, 7)
    }

    fun isSafeSquare(token: TokenState): Boolean {
        if (token.step in 51..57) return true
        if (token.step < 0) return true
        val globalIdx = (getStartOffset(token.color) + token.step) % commonTrackCoords.size
        return globalIdx in safeTrackIndices
    }

    // Check if token can move with dice roll
    fun canMoveToken(token: TokenState, diceRoll: Int): Boolean {
        if (token.isHome) return false
        if (token.isInYard) return diceRoll == 6
        return token.step + diceRoll <= 57
    }

    // Move token and check captures
    data class MoveResult(
        val updatedTokens: Map<PlayerColor, List<TokenState>>,
        val capturedToken: TokenState? = null,
        val reachedHome: Boolean = false,
        val extraTurn: Boolean = false
    )

    fun executeMove(
        allTokens: Map<PlayerColor, List<TokenState>>,
        movingToken: TokenState,
        diceRoll: Int
    ): MoveResult {
        val color = movingToken.color
        val currentTokens = allTokens[color]?.toMutableList() ?: return MoveResult(allTokens)
        val tokenIdx = currentTokens.indexOfFirst { it.id == movingToken.id }
        if (tokenIdx == -1) return MoveResult(allTokens)

        val newStep = if (movingToken.isInYard) {
            0
        } else {
            movingToken.step + diceRoll
        }

        val updatedToken = movingToken.copy(step = newStep)
        currentTokens[tokenIdx] = updatedToken

        val updatedAll = allTokens.toMutableMap()
        updatedAll[color] = currentTokens

        val reachedHome = updatedToken.step == 57
        var captured: TokenState? = null

        // Check capture if on common track
        if (updatedToken.step in 0..50 && !isSafeSquare(updatedToken)) {
            val landingCoord = getBoardCoord(updatedToken)
            for ((oppColor, oppTokens) in allTokens) {
                if (oppColor == color) continue
                val oppList = oppTokens.toMutableList()
                var capturedInOpp = false
                for (i in oppList.indices) {
                    val oppToken = oppList[i]
                    if (oppToken.step in 0..50 && !isSafeSquare(oppToken)) {
                        if (getBoardCoord(oppToken) == landingCoord) {
                            captured = oppToken
                            oppList[i] = oppToken.copy(step = -1) // Send back to yard
                            capturedInOpp = true
                            break
                        }
                    }
                }
                if (capturedInOpp) {
                    updatedAll[oppColor] = oppList
                    break
                }
            }
        }

        val extraTurn = diceRoll == 6 || captured != null || reachedHome
        return MoveResult(
            updatedTokens = updatedAll,
            capturedToken = captured,
            reachedHome = reachedHome,
            extraTurn = extraTurn
        )
    }

    // Check if player has won (all 4 tokens at step 57)
    fun hasPlayerWon(tokens: List<TokenState>): Boolean {
        return tokens.size == 4 && tokens.all { it.step == 57 }
    }
}
