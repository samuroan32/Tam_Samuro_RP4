package com.twinscalev4.notification

object ChatPresenceManager {
    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var isChatVisible: Boolean = false

    @Volatile
    var activeRoomId: String? = null
}
