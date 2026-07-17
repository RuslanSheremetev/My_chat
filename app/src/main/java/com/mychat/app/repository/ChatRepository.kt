package com.mychat.app.repository

import com.mychat.app.data.*
import com.mychat.app.models.*
import com.mychat.app.network.ApiClient
import com.mychat.app.utils.chatKey
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

class ChatRepository(
    private val db: AppDatabase,
    private val server: String
) {
    var currentUser: String? = null
    var currentToken: String? = null

    private val me: String get() = currentUser ?: ""
    private val token: String get() = currentToken ?: ""

    // Загрузка пользователей через ApiClient (Bearer)
    suspend fun loadUsers(): List<User> {
        android.util.Log.d("ChatRepo", "loadUsers: me=$me, token=${token.take(10)}...")
        val url = "$server/users/$me"
        android.util.Log.d("ChatRepo", "GET $url")
        val response = ApiClient.get(url, token)
        val json = JSONArray(response.body!!.string())
        val users = mutableListOf<User>()
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            val username = o.optString("username")
            val name = o.optString("name", "")
            val displayName = if (name.isNotEmpty()) name else username
            val lastMsgFrom = o.optString("lastMsgFrom", "")
            val lastMsgText = o.optString("lastMsg", "")
            val lastMsgTime = o.optString("last_msg_time", "")
            val lastMsgStatus = o.optString("lastMsgStatus", "sent")
            users.add(User(
                username = username,
                name = displayName,
                avatarColor = o.optString("avatar_color", "#2AABEE"),
                online = o.optBoolean("online", false),
                lastSeen = o.optString("last_seen", ""),
                bio = o.optString("bio", ""),
                avatarUrl = o.optString("avatar_url", ""),
                isBot = o.optBoolean("is_bot", false),
                isGroup = o.optBoolean("is_group", false),
                isFeed = o.optBoolean("is_feed", false),
                unread = o.optInt("unread", 0),
                lastMsgFromMe = (lastMsgFrom == me),
                lastMsg = lastMsgText,
                lastTime = lastMsgTime,
                lastMsgStatus = lastMsgStatus
            ))
        }
        return users
    }

    // Синхронизация chat_settings через ApiClient
    suspend fun syncChatSettings(users: List<User>) {
        try {
            val response = ApiClient.get("$server/api/chat_settings/all?me=$me", token)
            if (response.isSuccessful) {
                val json = JSONObject(response.body!!.string())
                for (u in users) {
                    val ck = chatKey(me, u.username)
                    if (json.has(ck)) {
                        val s = json.getJSONObject(ck)
                        u.isMuted = s.optBoolean("is_muted", false)
                        u.unread = s.optInt("unread", 0)
                        val settings = db.messageDao().getChatSettings(ck) ?: ChatSettings(chatKey = ck)
                        db.messageDao().saveChatSettings(settings.copy(isMuted = u.isMuted, unread = u.unread))
                    }
                }
            }
        } catch (e: Exception) {}
    }

    // Сохранение mute через ApiClient (Bearer) — все операции с БД в фоне
    fun saveMute(chatKey: String, isMuted: Boolean) {
        Thread {
            try {
                // Сохраняем в Room
                val settings = db.messageDao().getChatSettings(chatKey) ?: ChatSettings(chatKey = chatKey)
                db.messageDao().saveChatSettings(settings.copy(isMuted = isMuted))
                // Отправляем на сервер
                val json = JSONObject().apply {
                    put("chat_key", chatKey)
                    put("is_muted", isMuted)
                }
                val body = RequestBody.create("application/json".toMediaType(), json.toString())
                ApiClient.post("$server/chat_settings", token, body).close()
            } catch (e: Exception) {}
        }.start()
    }
}
