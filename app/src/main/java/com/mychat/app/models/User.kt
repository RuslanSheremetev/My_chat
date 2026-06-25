package com.mychat.app.models

data class User(
    val username: String,
    val avatarColor: String = "#2AABEE",
    val online: Boolean = false,
    val lastSeen: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val isGroup: Boolean = false,
    val isFeed: Boolean = false,
    val name: String = "",
    var lastMsg: String = "",
    var lastTime: String = "",
    var lastMsgType: String = "",
    var lastFileUrl: String = ""
)
