package com.mychat.app.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
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
    private lateinit var imageView: ImageView
    private lateinit var counter: TextView
    private lateinit var gestureDetector: GestureDetector
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        
        photos = intent.getStringArrayListExtra("photos") ?: mutableListOf()
        currentIndex = intent.getIntExtra("index", 0)
        
        imageView = findViewById(R.id.galleryImage)
        counter = findViewById(R.id.galleryCounter)
        
        findViewById<ImageButton>(R.id.galleryBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.galleryPrev).setOnClickListener { showPhoto(currentIndex - 1) }
        findViewById<ImageButton>(R.id.galleryNext).setOnClickListener { showPhoto(currentIndex + 1) }
        
        // Жесты для свайпа
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 200) {
                    if (diffX > 0) showPhoto(currentIndex - 1)
                    else showPhoto(currentIndex + 1)
                    return true
                }
                return false
            }
        })
        
        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        
        showPhoto(currentIndex)
    }
    
    private fun showPhoto(index: Int) {
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
}
