package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        val username = intent.getStringExtra("username") ?: ""
        val prefs = getSharedPreferences("mychat_prefs", MODE_PRIVATE)
        val displayName = prefs.getString("display_name_$username", username) ?: username
        
        findViewById<TextView>(R.id.settingsName).text = displayName
        findViewById<TextView>(R.id.settingsAvatar).text = displayName.take(1).uppercase()
        findViewById<TextView>(R.id.settingsUsername).text = "@$username"
        
        findViewById<LinearLayout>(R.id.settingsAccount).setOnClickListener {
            Toast.makeText(this, "Аккаунт", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.settingsChats).setOnClickListener {
            Toast.makeText(this, "Настройки чатов", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.settingsPrivacy).setOnClickListener {
            Toast.makeText(this, "Конфиденциальность", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.settingsNotifications).setOnClickListener {
            Toast.makeText(this, "Уведомления", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.settingsData).setOnClickListener {
            Toast.makeText(this, "Данные и память", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.settingsLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Выйти из аккаунта")
                .setMessage("Вы уверены? Все локальные данные будут удалены.")
                .setPositiveButton("Выйти") { _, _ ->
                    prefs.edit().clear().apply()
                    // Очищаем Room
                    thread {
                        try {
                            val db = androidx.room.Room.databaseBuilder(applicationContext, com.mychat.app.data.AppDatabase::class.java, "mychat_db").build()
                            db.clearAllTables()
                            db.close()
                        } catch (e: Exception) {}
                    }
                    finishAffinity()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}
