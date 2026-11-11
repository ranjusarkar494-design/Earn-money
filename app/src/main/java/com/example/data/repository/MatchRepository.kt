package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.GameMatch
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MatchRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    fun observeMatch(matchId: String): Flow<GameMatch?> {
        val path = "matches/$matchId"
        return db.collection("matches").document(matchId)
            .snapshots()
            .map { it.toObject(GameMatch::class.java) }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun createMatch(
        gameMode: String,
        entryFee: Long,
        prizePool: Long,
        initialBoardJson: String = "{}"
    ): String {
        val uid = requireUserId()
        val matchId = "match_${UUID.randomUUID().toString().take(10)}"
        val docRef = db.collection("matches").document(matchId)

        val payload = mapOf(
            "matchId" to matchId,
            "creatorId" to uid,
            "status" to if (gameMode == "bot") "in_progress" else "waiting",
            "gameMode" to gameMode,
            "entryFee" to entryFee,
            "prizePool" to prizePool,
            "playerIds" to listOf(uid),
            "currentTurnPlayerId" to uid,
            "lastDiceRoll" to 0L,
            "boardStateJson" to initialBoardJson,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return matchId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMatchState(
        matchId: String,
        currentTurnPlayerId: String,
        lastDiceRoll: Long,
        boardStateJson: String,
        status: String,
        winnerId: String? = null
    ) {
        val docRef = db.collection("matches").document(matchId)
        val updates = mutableMapOf<String, Any>(
            "currentTurnPlayerId" to currentTurnPlayerId,
            "lastDiceRoll" to lastDiceRoll,
            "boardStateJson" to boardStateJson,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (winnerId != null) {
            updates["winnerId"] = winnerId
        }

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendChatMessage(
        matchId: String,
        senderName: String,
        avatarId: String,
        text: String
    ) {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().take(10)}"
        val docRef = db.collection("matches").document(matchId).collection("messages").document(msgId)

        val payload = mapOf(
            "messageId" to msgId,
            "matchId" to matchId,
            "senderId" to uid,
            "senderName" to senderName,
            "avatarId" to avatarId,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeChatMessages(matchId: String): Flow<List<ChatMessage>> {
        val path = "matches/$matchId/messages"
        return db.collection("matches").document(matchId).collection("messages")
            .orderBy("createdAt")
            .snapshots()
            .map { it.toObjects(ChatMessage::class.java) }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }
}
