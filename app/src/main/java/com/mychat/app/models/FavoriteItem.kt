package com.mychat.app.models

data class FavoriteItem(
    val msgId: String,
    val text: String,
    val from: String,
    val time: String,
    val type: String = "text",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val isGroup: Boolean = false,
    val isFeed: Boolean = false,
    val avatarColor: String = "#2AABEE"
)
