package com.mychat.app.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateFeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_feed)
        
        var isPrivate = false
        val publicBtn = findViewById<Button>(R.id.radioPublic)
        val privateBtn = findViewById<Button>(R.id.radioPrivate)
        
        publicBtn.setOnClickListener { isPrivate = false; publicBtn.setTextColor(0xffFF9500.toInt()); privateBtn.setTextColor(0xff777777.toInt()) }
        privateBtn.setOnClickListener { isPrivate = true; privateBtn.setTextColor(0xffFF9500.toInt()); publicBtn.setTextColor(0xff777777.toInt()) }
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            val name = findViewById<EditText>(R.id.feedName).text.toString().trim()
            val desc = findViewById<EditText>(R.id.feedDesc).text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название ленты", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            setResult(RESULT_OK, android.content.Intent().apply {
                putExtra("feedName", name); putExtra("feedDesc", desc); putExtra("isPrivate", isPrivate)
            })
            Toast.makeText(this, "Лента «$name» создана", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        findViewById<Button>(R.id.btnAddSubscriber).setOnClickListener {
            Toast.makeText(this, "Добавление подписчиков", Toast.LENGTH_SHORT).show()
        }
    }
}