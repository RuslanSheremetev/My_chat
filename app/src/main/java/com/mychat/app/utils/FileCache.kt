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
    
    // Для фото
    fun getBitmap(url: String): Bitmap? {
        val file = getFile(url)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    
    // Для любых файлов
    fun getFile(url: String): File? {
        val file = getCacheFile(url)
        return if (file.exists()) file else null
    }
    
    // Сохранение в кеш
    fun saveToCache(url: String, bytes: ByteArray): File? {
        return try {
            val file = getCacheFile(url)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) { null }
    }
    
    // Совместимость со старым кодом
    fun getCachedFile(url: String): File? = getFile(url)
    
    fun isCached(url: String): Boolean = getCacheFile(url).exists()
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
    
    fun getCacheSize(): Long {
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0
    }
    
    private fun getCacheFile(url: String): File {
        val name = url.substringAfterLast("/").ifEmpty { "file_${System.currentTimeMillis()}" }
        return File(cacheDir, name)
    }
}
