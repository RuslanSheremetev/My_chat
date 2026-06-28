package com.mychat.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_settings")
data class ChatSettings(
    @PrimaryKey val chatKey: String,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false
)
