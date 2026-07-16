package com.mychat.app.repository

import com.mychat.app.data.*
import com.mychat.app.models.*
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.*
import java.io.IOException

class ChatRepository(
    private val db: AppDatabase,
    private val client: OkHttpClient,
    private val server: String
) {
    private val me: String get() = currentUser ?: ""
    private val token: String get() = currentToken ?: ""
    
    var currentUser: String? = null
    var currentToken: String? = null
    
    fun chatKey(u1: String, u2: String) = listOf(u1, u2).sorted().joinToString("_")
    
    // Загрузка пользователей
    suspend fun loadUsers(): List<User> {
        val url = "$server/users/$me?token=$token"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val json = JSONArray(response.body!!.string())
        val users = mutableListOf<User>()
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            val username = o.optString("username")
            val name = o.optString("name", "")
            val displayName = if (name.isNotEmpty()) name else username
            users.add(User(
                username = username,
                name = displayName,
                avatarColor = o.optString("avatar_color", "#2AABEE"),
                online = o.optBoolean("online", false),
                isBot = o.optBoolean("is_bot", false),
                isGroup = o.optBoolean("is_group", false),
                isFeed = o.optBoolean("is_feed", false),
                unread = o.optInt("unread", 0)
            ))
        }
        return users
    }
    
    // Получить chat_settings с сервера
    suspend fun syncChatSettings(users: List<User>) {
        try {
            val url = "$server/api/chat_settings/all?me=$me&token=$token"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body!!.string())
                for (u in users) {
                    val ck = chatKey(me, u.username)
                    if (json.has(ck)) {
                        val s = json.getJSONObject(ck)
                        u.isMuted = s.optBoolean("is_muted", false)
                        u.unread = s.optInt("unread", 0)
                        // Сохраняем в Room
                        val settings = db.messageDao().getChatSettings(ck) ?: ChatSettings(chatKey = ck)
                        db.messageDao().saveChatSettings(settings.copy(isMuted = u.isMuted, unread = u.unread))
                    }
                }
            }
        } catch (e: Exception) {}
    }
    
    // Сохранить mute
    fun saveMute(chatKey: String, isMuted: Boolean) {
        val settings = db.messageDao().getChatSettings(chatKey) ?: ChatSettings(chatKey = chatKey)
        db.messageDao().saveChatSettings(settings.copy(isMuted = isMuted))
        // Отправка на сервер в фоне
        Thread {
            try {
                val json = JSONObject().apply {
                    put("chat_key", chatKey)
                    put("is_muted", isMuted)
                }
                val body = RequestBody.create(okhttp3.MediaType.parse("application/json"), json.toString())
                val request = Request.Builder().url("$server/chat_settings?token=$token").post(body).build()
                client.newCall(request).execute()
            } catch (e: Exception) {}
        }.start()
    }
}
