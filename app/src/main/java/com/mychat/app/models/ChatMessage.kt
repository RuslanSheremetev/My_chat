package com.mychat.app.models

data class ChatMessage(
    val read: Boolean = false,
    val id: String,
    val from: String,
    val to: String,
    val text: String = "",
    val time: String = "",
    val file: FileInfo? = null,
    val isGroup: Boolean = false
)
