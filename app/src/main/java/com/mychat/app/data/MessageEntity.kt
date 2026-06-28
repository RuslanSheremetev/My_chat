package com.mychat.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatKey: String,
    val fromUser: String,
    val toUser: String,
    val text: String,
    val time: String,
    val fileUrl: String = "",
    val fileName: String = "",
    val isRead: Boolean = false,
    val status: String = "sent",
    val delivered: Boolean = false,
    val reactionsJson: String = "{}"
)
