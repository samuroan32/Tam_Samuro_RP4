package com.twinscalev4.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.twinscalev4.domain.SizeMath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class FirebaseRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun joinRoom(
        roomId: String,
        userId: String,
        userName: String,
        sizeMetersRaw: String,
        mode: GrowthMode,
        fcmToken: String
    ) = withContext(ioDispatcher) {
        val roomRef = database.reference.child("rooms").child(roomId)
        val userRef = roomRef.child("users").child(userId)

        val profileMap = mapOf(
            "name" to userName,
            "size" to sizeMetersRaw,
            "mode" to mode.id,
            "fcmToken" to fcmToken,
            "online" to true
        )

        userRef.updateChildren(profileMap).await()

        val globalUserRef = database.reference.child("users").child(userId)
        globalUserRef.updateChildren(profileMap).await()

        val stateRef = roomRef.child("state")
        val currentState = stateRef.get().await()
        val userAId = currentState.child("userAId").getValue(String::class.java).orEmpty()
        val userBId = currentState.child("userBId").getValue(String::class.java).orEmpty()

        val updatedState = mutableMapOf<String, Any>("lastUpdated" to System.currentTimeMillis())
        when {
            userAId.isBlank() -> updatedState["userAId"] = userId
            userAId != userId && userBId.isBlank() -> updatedState["userBId"] = userId
        }
        stateRef.updateChildren(updatedState).await()
    }

    suspend fun setOnline(roomId: String, userId: String, online: Boolean) = withContext(ioDispatcher) {
        database.reference.child("rooms").child(roomId)
            .child("users").child(userId).child("online").setValue(online).await()
    }

    fun observeRoom(roomId: String, selfUserId: String): Flow<RoomSnapshot> = callbackFlow {
        val roomRef = database.reference.child("rooms").child(roomId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.child("users")
                val messagesNode = snapshot.child("messages")
                val stateNode = snapshot.child("state")

                val selfSnapshot = users.child(selfUserId)
                val self = selfSnapshot.toUser(selfUserId)

                val partnerNode = users.children.firstOrNull { it.key != selfUserId }
                val partner = partnerNode?.toUser(partnerNode.key.orEmpty())

                val messages = messagesNode.children.mapNotNull { messageSnap ->
                    val id = messageSnap.key ?: return@mapNotNull null
                    val sender = messageSnap.child("senderId").getValue(String::class.java).orEmpty()
                    val text = messageSnap.child("text").getValue(String::class.java).orEmpty()
                    val timestamp = messageSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                    ChatMessage(id, sender, text, timestamp)
                }.sortedBy { it.timestamp }

                val state = RoomState(
                    roomId = roomId,
                    userAId = stateNode.child("userAId").getValue(String::class.java).orEmpty(),
                    userBId = stateNode.child("userBId").getValue(String::class.java).orEmpty(),
                    lastUpdated = stateNode.child("lastUpdated").getValue(Long::class.java) ?: 0L
                )

                trySend(RoomSnapshot(self = self, partner = partner, state = state, messages = messages))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomRef.addValueEventListener(listener)
        awaitClose { roomRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(roomId: String, senderId: String, text: String): String = withContext(ioDispatcher) {
        val msgRef = database.reference.child("rooms").child(roomId).child("messages").push()
        val msgId = msgRef.key.orEmpty()

        msgRef.setValue(
            mapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
        msgId
    }

    suspend fun applyGrowth(roomId: String, userId: String, mode: GrowthMode, grow: Boolean) = withContext(ioDispatcher) {
        val userRef = database.reference.child("rooms").child(roomId).child("users").child(userId)
        val currentSizeRaw = userRef.child("size").get().await().getValue(String::class.java) ?: "1"
        val current = currentSizeRaw.toBigDecimalOrNull() ?: BigDecimal.ONE

        val updated = SizeMath.nextSize(current, mode, grow).setScale(30, RoundingMode.HALF_UP)
        userRef.child("size").setValue(updated.toPlainString()).await()
        userRef.child("mode").setValue(mode.id).await()

        database.reference.child("users").child(userId).updateChildren(
            mapOf(
                "size" to updated.toPlainString(),
                "mode" to mode.id
            )
        ).await()
    }

    private fun DataSnapshot.toUser(id: String): UserProfile {
        return UserProfile(
            userId = id,
            name = child("name").getValue(String::class.java).orEmpty(),
            sizeMetersRaw = child("size").getValue(String::class.java).orEmpty().ifBlank { "1" },
            mode = child("mode").getValue(String::class.java).orEmpty().ifBlank { GrowthMode.BALANCED.id },
            fcmToken = child("fcmToken").getValue(String::class.java).orEmpty(),
            online = child("online").getValue(Boolean::class.java) ?: false
        )
    }
}
