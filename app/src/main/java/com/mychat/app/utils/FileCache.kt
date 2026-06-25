package com.mychat.app.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object FileCache {
    private lateinit var cacheDir: File
    
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "media")
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }
    
    fun getCachedFile(url: String): File? {
        val name = url.substringAfterLast("/")
        val file = File(cacheDir, name)
        return if (file.exists()) file else null
    }
    
    fun saveToCache(url: String, bytes: ByteArray): File {
        val name = url.substringAfterLast("/")
        val file = File(cacheDir, name)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
}
