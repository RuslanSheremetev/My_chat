package com.mychat.app.models

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val text: String = "",
    val time: String = "",
    val file: FileInfo? = null,
    val isGroup: Boolean = false,
    val read: Boolean = false
)
