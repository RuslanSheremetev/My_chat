package com.mychat.app.network

import okhttp3.*
import java.util.concurrent.TimeUnit

object ApiClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(url: String, token: String): Response {
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        return client.newCall(request).execute()
    }

    fun post(url: String, token: String, body: RequestBody): Response {
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        return client.newCall(request).execute()
    }
}
