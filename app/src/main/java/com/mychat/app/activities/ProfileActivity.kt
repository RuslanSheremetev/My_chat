package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)
        
        val token = intent.getStringExtra("token") ?: ""
        val username = intent.getStringExtra("username") ?: ""
        val me = username
        
        val prefs = getSharedPreferences("mychat_prefs", MODE_PRIVATE)
        val displayName = prefs.getString("display_name_$username", username) ?: username
        
        findViewById<TextView>(R.id.profileName).text = displayName
        findViewById<TextView>(R.id.profileAvatar).text = displayName.take(1).uppercase()
        
        findViewById<ImageView>(R.id.profileBackBtn).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.actionCreate).setOnClickListener { 
            Toast.makeText(this, "Создание истории", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.actionGallery).setOnClickListener { 
            Toast.makeText(this, "Галерея", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.actionDrafts).setOnClickListener { 
            Toast.makeText(this, "Черновики", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.fabAddMedia).setOnClickListener { 
            Toast.makeText(this, "Добавить медиа", Toast.LENGTH_SHORT).show()
        }
        
        // Нижнее меню
        findViewById<LinearLayout>(R.id.navChats2).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.navSettings2).setOnClickListener { finish() }
    }
}
