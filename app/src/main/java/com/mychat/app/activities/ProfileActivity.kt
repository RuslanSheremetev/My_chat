package com.mychat.app.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)

        // Просто показываем имя пользователя
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val username = prefs.getString("username", "User") ?: "User"

        // Устанавливаем имя
        val nameView = findViewById<TextView>(R.id.profileName)
        if (nameView != null) {
            nameView.text = username
        }

        // Устанавливаем аватар
        val avatarView = findViewById<TextView>(R.id.profileAvatar)
        if (avatarView != null) {
            avatarView.text = username.take(1).uppercase()
        }

        // Кнопка назад
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        if (backBtn != null) {
            backBtn.setOnClickListener {
                finish()
            }
        }

        // Кнопка выхода
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener {
                prefs.edit().clear().apply()
                Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Статус (если есть)
        try {
            val statusView = findViewById<TextView>(R.id.profileStatus)
            if (statusView != null) {
                statusView.text = "Живу в облаках ☁️"
            }
        } catch (e: Exception) {
            // Игнорируем
        }
    }
}
