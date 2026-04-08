package com.twinscalev4.data

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val sizeMetersRaw: String = "1",
    val mode: String = GrowthMode.BALANCED.id,
    val fcmToken: String = "",
    val online: Boolean = false
)

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class RoomState(
    val roomId: String = "",
    val userAId: String = "",
    val userBId: String = "",
    val lastUpdated: Long = 0L
)

enum class GrowthMode(val id: String, val displayName: String, val basePercent: Double) {
    GENTLE("gentle", "Мягкий", 0.015),
    BALANCED("balanced", "Сбалансированный", 0.045),
    EXTREME("extreme", "Экстрим", 0.09);

    companion object {
        fun fromId(value: String): GrowthMode {
            return entries.firstOrNull { it.id == value } ?: BALANCED
        }
    }
}

data class RoomSnapshot(
    val self: UserProfile? = null,
    val partner: UserProfile? = null,
    val state: RoomState? = null,
    val messages: List<ChatMessage> = emptyList()
)
