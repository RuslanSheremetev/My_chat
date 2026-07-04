package com.mychat.app.activities

import android.os.Bundle
import android.widget.*
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateFeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_feed)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            val name = findViewById<EditText>(R.id.feedName).text.toString().trim()
            val desc = findViewById<EditText>(R.id.feedDesc).text.toString().trim()
            val isPrivate = findViewById<RadioButton>(R.id.radioPrivate).isChecked

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название ленты", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = android.content.Intent().apply {
                putExtra("feedName", name)
                putExtra("feedDesc", desc)
                putExtra("isPrivate", isPrivate)
            }
            setResult(RESULT_OK, data)
            Toast.makeText(this, "Лента «$name» создана", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnAddSubscriber).setOnClickListener {
            Toast.makeText(this, "Добавление подписчиков", Toast.LENGTH_SHORT).show()
        }
    }
}
