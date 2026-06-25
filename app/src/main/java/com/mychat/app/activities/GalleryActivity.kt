package com.mychat.app.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R
import com.mychat.app.utils.FileCache
import kotlin.concurrent.thread

class GalleryActivity : AppCompatActivity() {
    private var photos = mutableListOf<String>()
    private var currentIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        
        photos = intent.getStringArrayListExtra("photos") ?: mutableListOf()
        currentIndex = intent.getIntExtra("index", 0)
        
        val imageView = findViewById<ImageView>(R.id.galleryImage)
        val counter = findViewById<TextView>(R.id.galleryCounter)
        val prevBtn = findViewById<ImageButton>(R.id.galleryPrev)
        val nextBtn = findViewById<ImageButton>(R.id.galleryNext)
        
        findViewById<ImageButton>(R.id.galleryBack).setOnClickListener { finish() }
        
        fun showPhoto(index: Int) {
            if (index < 0 || index >= photos.size) return
            currentIndex = index
            counter.text = "${index + 1}/${photos.size}"
            
            var url = photos[index]
            if (!url.startsWith("http")) url = "http://2.26.71.102:8000$url"
            
            val cached = FileCache.getBitmap(url)
            if (cached != null) {
                imageView.setImageBitmap(cached)
            } else {
                thread {
                    try {
                        val bytes = java.net.URL(url).readBytes()
                        FileCache.saveToCache(url, bytes)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        runOnUiThread { imageView.setImageBitmap(bmp) }
                    } catch (e: Exception) {}
                }
            }
        }
        
        prevBtn.setOnClickListener { showPhoto(currentIndex - 1) }
        nextBtn.setOnClickListener { showPhoto(currentIndex + 1) }
        
        showPhoto(currentIndex)
    }
}
