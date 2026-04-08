package com.twinscalev4.notification

object TokenSyncManager {
    @Volatile
    var userId: String? = null

    @Volatile
    var roomId: String? = null

    @Volatile
    var userName: String? = null

    fun updateSession(userId: String, roomId: String, userName: String) {
        this.userId = userId
        this.roomId = roomId
        this.userName = userName
    }
}
