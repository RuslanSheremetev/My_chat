package com.mychat.app.network

import android.util.Log
import okhttp3.*

class WebSocketManager(
    private val server: String,
    private val username: String,
    private val token: String
) {
    private var ws: WebSocket? = null
    var onMessage: ((String) -> Unit)? = null
    var onReconnect: (() -> Unit)? = null
    var onOpen: ((WebSocket) -> Unit)? = null

    fun connect() {
        val url = server.replace("http://", "ws://") + "/ws/$username?token=$token"
        ws = ApiClient.client.newWebSocket(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onOpen?.invoke(webSocket)
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessage?.invoke(text)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onReconnect?.invoke()
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onReconnect?.invoke()
                }
            }
        )
    }

    fun send(json: String) {
        ws?.send(json)
    }

    fun disconnect() {
        ws?.close(1000, "user closed")
    }
}
