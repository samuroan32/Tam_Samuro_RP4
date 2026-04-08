package com.twinscalev4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.twinscalev4.data.ChatMessage
import com.twinscalev4.data.FirebaseRepository
import com.twinscalev4.data.GrowthMode
import com.twinscalev4.data.RoomSnapshot
import com.twinscalev4.data.UserProfile
import com.twinscalev4.domain.RoomJoinValidator
import com.twinscalev4.notification.ChatPresenceManager
import com.twinscalev4.notification.TokenSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ChatUiState(
    val roomId: String = "",
    val suggestedRoomId: String = "",
    val selfId: String = "",
    val selfName: String = "",
    val selfSizeRaw: String = "1",
    val selfMode: GrowthMode = GrowthMode.BALANCED,
    val partner: UserProfile? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draftMessage: String = "",
    val isJoined: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun updateDraft(value: String) {
        _state.update { it.copy(draftMessage = value) }
    }

    fun setSuggestedRoom(roomId: String?) {
        if (roomId.isNullOrBlank()) return
        _state.update { it.copy(suggestedRoomId = roomId.trim()) }
    }

    fun joinRoom(roomId: String, name: String, sizeMeters: String, mode: GrowthMode) {
        if (!RoomJoinValidator.isValidRoom(roomId)) {
            _state.update { it.copy(error = "Введите корректный ID комнаты") }
            return
        }
        if (!RoomJoinValidator.isValidName(name)) {
            _state.update { it.copy(error = "Введите имя от 2 символов") }
            return
        }
        if (!RoomJoinValidator.isValidSize(sizeMeters)) {
            _state.update { it.copy(error = "Размер должен быть больше нуля") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val userId = UUID.randomUUID().toString()
            val token = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (_: Exception) {
                ""
            }

            runCatching {
                repository.joinRoom(
                    roomId = roomId.trim(),
                    userId = userId,
                    userName = name.trim(),
                    sizeMetersRaw = sizeMeters.trim(),
                    mode = mode,
                    fcmToken = token
                )
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Не удалось подключиться к комнате"
                    )
                }
                return@launch
            }

            TokenSyncManager.updateSession(
                userId = userId,
                roomId = roomId.trim(),
                userName = name.trim()
            )

            _state.update {
                it.copy(
                    roomId = roomId.trim(),
                    selfId = userId,
                    selfName = name.trim(),
                    selfSizeRaw = sizeMeters.trim(),
                    selfMode = mode,
                    isJoined = true,
                    isLoading = false
                )
            }

            observeRoom()
        }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            val roomId = state.value.roomId
            val selfId = state.value.selfId
            repository.observeRoom(roomId, selfId).collect { snapshot: RoomSnapshot ->
                val self = snapshot.self
                _state.update {
                    it.copy(
                        selfSizeRaw = self?.sizeMetersRaw ?: it.selfSizeRaw,
                        selfMode = GrowthMode.fromId(self?.mode ?: it.selfMode.id),
                        partner = snapshot.partner,
                        messages = snapshot.messages,
                        error = null
                    )
                }
            }
        }
    }

    fun setChatVisible(visible: Boolean) {
        ChatPresenceManager.isChatVisible = visible
        ChatPresenceManager.activeRoomId = if (visible) state.value.roomId else null
    }

    fun sendMessage() {
        val roomId = state.value.roomId
        val selfId = state.value.selfId
        val selfName = state.value.selfName
        val text = state.value.draftMessage.trim()
        if (roomId.isBlank() || selfId.isBlank() || text.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repository.sendMessage(roomId, selfId, selfName, text)
            }.onSuccess {
                _state.update { it.copy(draftMessage = "") }
            }.onFailure {
                _state.update { it.copy(error = "Не удалось отправить сообщение") }
            }
        }
    }

    fun applyGrowth(grow: Boolean) {
        val roomId = state.value.roomId
        val selfId = state.value.selfId
        val mode = state.value.selfMode
        if (roomId.isBlank() || selfId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repository.applyGrowth(roomId, selfId, mode, grow)
            }.onFailure {
                _state.update { it.copy(error = "Не удалось обновить размер") }
            }
        }
    }

    fun applyPartnerGrowth(grow: Boolean) {
        val roomId = state.value.roomId
        val partnerId = state.value.partner?.userId.orEmpty()
        val mode = state.value.selfMode
        if (roomId.isBlank() || partnerId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repository.applyGrowth(roomId, partnerId, mode, grow)
            }.onFailure {
                _state.update { it.copy(error = "Не удалось изменить размер партнёра") }
            }
        }
    }

    fun switchMode(mode: GrowthMode) {
        _state.update { it.copy(selfMode = mode) }
    }

    fun syncLatestToken() {
        val roomId = state.value.roomId
        val userId = state.value.selfId
        if (roomId.isBlank() || userId.isBlank()) return

        viewModelScope.launch {
            val token = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (_: Exception) {
                ""
            }
            if (token.isBlank()) return@launch
            runCatching {
                repository.updateFcmToken(userId = userId, roomId = roomId, token = token)
            }
        }
    }
}
