package com.mychat.app.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CallActivity : AppCompatActivity() {
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                findViewById<TextView>(R.id.callTimer).text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        val name = intent.getStringExtra("name") ?: "Пользователь"
        val avatar = intent.getStringExtra("avatar") ?: "U"
        val isIncoming = intent.getBooleanExtra("incoming", false)
        
        findViewById<TextView>(R.id.callName).text = name
        findViewById<TextView>(R.id.callAvatar).text = avatar.take(1).uppercase()
        
        if (isIncoming) {
            findViewById<TextView>(R.id.callStatus).text = "Входящий звонок..."
        }
        
        // Начать звонок
        startCall()
        
        findViewById<ImageButton>(R.id.btnEndCall).setOnClickListener {
            endCall()
        }
        
        findViewById<ImageButton>(R.id.btnSpeaker).setOnClickListener {
            Toast.makeText(this, "Динамик", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<ImageButton>(R.id.btnMic).setOnClickListener {
            Toast.makeText(this, "Микрофон", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startCall() {
        findViewById<TextView>(R.id.callStatus).text = "Соединение..."
        // WebRTC логика будет здесь
        handler.postDelayed({
            findViewById<TextView>(R.id.callStatus).visibility = android.view.View.GONE
            findViewById<TextView>(R.id.callTimer).visibility = android.view.View.VISIBLE
            isRunning = true
            handler.post(timerRunnable)
        }, 2000)
    }
    
    private fun endCall() {
        isRunning = false
        finish()
    }
    
    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
