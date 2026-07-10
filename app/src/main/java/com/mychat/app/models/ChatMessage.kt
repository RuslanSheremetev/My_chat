package com.mychat.app.models
data class LocationData(val lat: Double = 0.0, val lon: Double = 0.0)
data class ForwardData(val from: String = "", val text: String = "", val file: FileInfo? = null)
data class ChatMessage(
    val id: String, val from: String, val to: String, val text: String = "", val time: String = "",
    val file: FileInfo? = null, val location: LocationData? = null, val isGroup: Boolean = false,
    var read: Boolean = false, var delivered: Boolean = false,
    var reactions: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val forward: ForwardData? = null
)
