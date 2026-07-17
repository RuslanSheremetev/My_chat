package com.mychat.app.repository

import com.mychat.app.data.AppDatabase
import com.mychat.app.models.User
import com.mychat.app.network.ApiClient
import org.json.JSONArray

class UserRepository(
    private val db: AppDatabase,
    private val server: String
) {
    fun loadUsers(me: String, token: String): List<User> {
        val response = ApiClient.get("$server/users/$me?token=$token")
        val json = JSONArray(response.body!!.string())
        val users = mutableListOf<User>()
        
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            val username = o.optString("username")
            val displayName = o.optString("name", "")
            val finalName = if (displayName.isNotEmpty()) displayName else username
            
            users.add(User(
                username = username,
                name = finalName,
                avatarColor = o.optString("avatar_color", "#2AABEE"),
                online = o.optBoolean("online", false),
                lastSeen = o.optString("last_seen", ""),
                bio = o.optString("bio", ""),
                avatarUrl = o.optString("avatar_url", ""),
                isGroup = o.optBoolean("is_group", false),
                isFeed = o.optBoolean("is_feed", false),
                isBot = o.optBoolean("is_bot", false),
                unread = o.optInt("unread", 0)
            ))
        }
        return users
    }
}
