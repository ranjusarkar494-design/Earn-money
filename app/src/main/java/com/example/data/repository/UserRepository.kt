package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
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

class UserRepository(private val db: FirebaseFirestore) {

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

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun getOrCreateProfile(displayName: String, avatarId: String = "avatar_guy_smile"): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid, displayName = displayName)
            } else {
                val newProfile = UserProfile(
                    userId = uid,
                    displayName = if (displayName.isNotBlank()) displayName else "Player",
                    avatarId = avatarId,
                    coinBalance = 1000L,
                    winningsBalance = 250L,
                    gamesWon = 0L,
                    gamesPlayed = 0L
                )
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to newProfile.displayName,
                    "avatarId" to newProfile.avatarId,
                    "coinBalance" to newProfile.coinBalance,
                    "winningsBalance" to newProfile.winningsBalance,
                    "gamesWon" to newProfile.gamesWon,
                    "gamesPlayed" to newProfile.gamesPlayed,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                newProfile
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            UserProfile(userId = uid, displayName = displayName)
        }
    }

    suspend fun updateAvatar(avatarId: String, customUri: String = "") {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "avatarId" to avatarId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (customUri.isNotBlank()) {
            updates["customAvatarUri"] = customUri
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

    suspend fun addDeposit(amount: Long, paymentMethod: String) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val txId = "tx_${UUID.randomUUID().toString().take(12)}"
        val txRef = userRef.collection("transactions").document(txId)

        val txPayload = mapOf(
            "transactionId" to txId,
            "userId" to uid,
            "type" to "deposit",
            "amount" to amount,
            "status" to "success",
            "paymentMethod" to paymentMethod,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runBatch { batch ->
                batch.set(txRef, txPayload)
                batch.update(userRef, "coinBalance", FieldValue.increment(amount))
                batch.update(userRef, "updatedAt", FieldValue.serverTimestamp())
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            throw e
        }
    }

    suspend fun withdrawRewards(amount: Long, paymentMethod: String) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val txId = "tx_${UUID.randomUUID().toString().take(12)}"
        val txRef = userRef.collection("transactions").document(txId)

        val txPayload = mapOf(
            "transactionId" to txId,
            "userId" to uid,
            "type" to "withdrawal",
            "amount" to amount,
            "status" to "success",
            "paymentMethod" to paymentMethod,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runBatch { batch ->
                batch.set(txRef, txPayload)
                batch.update(userRef, "winningsBalance", FieldValue.increment(-amount))
                batch.update(userRef, "updatedAt", FieldValue.serverTimestamp())
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            throw e
        }
    }

    suspend fun recordGameReward(won: Boolean, rewardAmount: Long) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "gamesPlayed" to FieldValue.increment(1),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (won && rewardAmount > 0) {
            updates["gamesWon"] = FieldValue.increment(1)
            updates["winningsBalance"] = FieldValue.increment(rewardAmount)

            val txId = "tx_${UUID.randomUUID().toString().take(12)}"
            val txRef = userRef.collection("transactions").document(txId)
            val txPayload = mapOf(
                "transactionId" to txId,
                "userId" to uid,
                "type" to "reward_win",
                "amount" to rewardAmount,
                "status" to "success",
                "paymentMethod" to "InstantReward",
                "createdAt" to FieldValue.serverTimestamp()
            )
            try {
                db.runBatch { batch ->
                    batch.set(txRef, txPayload)
                    batch.update(userRef, updates)
                }.await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, txRef.path)
            }
        } else {
            try {
                userRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            }
        }
    }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { it.toObjects(WalletTransaction::class.java) }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }
}
