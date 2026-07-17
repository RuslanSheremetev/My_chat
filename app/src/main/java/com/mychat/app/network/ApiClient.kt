package com.mychat.app.network

import okhttp3.*
import java.util.concurrent.TimeUnit

object ApiClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(url: String): Response {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute()
    }

    fun post(url: String, body: RequestBody): Response {
        val request = Request.Builder().url(url).post(body).build()
        return client.newCall(request).execute()
    }
}
