package com.mychat.app.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)

        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)

        // Аватар
        val avatar = findViewById<TextView>(R.id.profileAvatarLarge)
        val username = prefs.getString("username", "A") ?: "A"
        avatar.text = username.take(1).uppercase()

        // Статус
        val statusInput = findViewById<EditText>(R.id.profileStatus)
        statusInput.setText(prefs.getString("user_status", "В сети") ?: "В сети")

        // О себе
        val bioInput = findViewById<EditText>(R.id.profileBio)
        bioInput.setText(prefs.getString("user_bio", "") ?: "")

        // Сохранить
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val newStatus = statusInput.text.toString().trim()
            val newBio = bioInput.text.toString().trim()
            prefs.edit().putString("user_status", newStatus).putString("user_bio", newBio).apply()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Очистить кэш
        findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            // TODO: реальная очистка кэша
            Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show()
        }

        // Выйти
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            finish()
        }
    }
}
