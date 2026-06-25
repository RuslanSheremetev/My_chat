package com.mychat.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object FileCache {
    private lateinit var cacheDir: File
    
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "media")
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }
    
    fun getBitmap(url: String): Bitmap? {
        val file = getFile(url)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    
    fun saveToCache(url: String, bytes: ByteArray): File? {
        return try {
            val file = getFile(url)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) { null }
    }
    
    fun getCachedFile(url: String): File? {
        val file = getFile(url)
        return if (file.exists()) file else null
    }
    
    fun isCached(url: String): Boolean = getFile(url).exists()
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
    
    private fun getFile(url: String): File {
        val name = url.substringAfterLast("/").ifEmpty { "file_${System.currentTimeMillis()}" }
        return File(cacheDir, name)
    }
}
