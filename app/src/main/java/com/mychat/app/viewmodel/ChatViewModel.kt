package com.mychat.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mychat.app.data.AppDatabase
import com.mychat.app.models.User
import com.mychat.app.network.WebSocketManager
import com.mychat.app.repository.ChatRepository
import com.mychat.app.utils.chatKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private lateinit var chatRepo: ChatRepository
    private var wsManager: WebSocketManager? = null

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    private val _me = MutableStateFlow("")
    val me: StateFlow<String> = _me

    private val _server = MutableStateFlow("http://2.26.71.102:8000")
    val server: StateFlow<String> = _server

    private val _selectedChat = MutableStateFlow<String?>(null)
    val selectedChat: StateFlow<String?> = _selectedChat

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    var onSignalingMessage: ((String) -> Unit)? = null

    fun init(db: AppDatabase, serverUrl: String, username: String, userToken: String) {
        _server.value = serverUrl
        _me.value = username
        _token.value = userToken
        chatRepo = ChatRepository(db, serverUrl)
        chatRepo.currentUser = username
        chatRepo.currentToken = userToken
    }

    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userList = chatRepo.loadUsers()
                _users.value = userList
                // Синхронизация mute/unread
                chatRepo.syncChatSettings(userList)
                _users.value = userList
            } catch (e: Exception) {}
        }
    }

    fun toggleMute(selId: String) {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        // Обновляем в списке
        val updated = _users.value.map { u ->
            if (u.username == selId) u.copy(isMuted = newMuted) else u
        }
        _users.value = updated
        // Сохраняем
        chatRepo.currentUser = _me.value
        chatRepo.currentToken = _token.value
        chatRepo.saveMute(chatKey(_me.value, selId), newMuted)
    }

    fun connectWebSocket() {
        wsManager?.disconnect()
        wsManager = WebSocketManager(_server.value, _me.value, _token.value)
        wsManager?.onMessage = { text -> handleWsMessage(text) }
        wsManager?.onReconnect = { wsManager?.connect() }
        wsManager?.connect()
    }

    fun sendWsMessage(json: String) {
        wsManager?.send(json)
    }

    fun selectChat(chatId: String?) {
        _selectedChat.value = chatId
    }

    private fun handleWsMessage(text: String) {
        try {
            val j = org.json.JSONObject(text)
            val jtype = j.optString("type")
            when {
                jtype == "call_offer" -> {
                    val from = j.optString("from", "")
                    if (from.isNotEmpty() && from != _me.value) {
                        onSignalingMessage?.invoke(text)
                    }
                }
                jtype in listOf("call_answer", "ice_candidate", "call_end") -> {
                    onSignalingMessage?.invoke(text)
                }
                jtype == "ping" -> wsManager?.send("{\"type\":\"pong\"}")
                jtype == "delivered" -> {
                    val to = j.optString("to", "")
                    val updated = _users.value.map { u ->
                        if (u.username == to) u.copy(lastMsgStatus = "delivered") else u
                    }
                    _users.value = updated
                }
                jtype == "read" -> {
                    val from = j.optString("from", "")
                    val updated = _users.value.map { u ->
                        if (u.username == from) u.copy(lastMsgStatus = "read") else u
                    }
                    _users.value = updated
                }
                jtype == "reaction_added" || jtype == "reaction_removed" -> {
                    // Будет обработано в адаптере
                }
                else -> {
                    // Новое сообщение — обновить список
                    loadUsers()
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        wsManager?.disconnect()
    }
}
