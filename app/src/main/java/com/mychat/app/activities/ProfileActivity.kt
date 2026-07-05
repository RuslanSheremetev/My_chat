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
        try {
            setContentView(R.layout.activity_profile_new)
            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val avatar = findViewById<TextView>(R.id.profileAvatar)
            val username = prefs.getString("username", "A") ?: "A"
            avatar.text = username.take(1).uppercase()
            findViewById<TextView>(R.id.profileName).text = prefs.getString("user_name", username) ?: username
            findViewById<TextView>(R.id.profileUsername).text = "@$username"
            val statusInput = findViewById<EditText>(R.id.profileStatus)
            statusInput.setText(prefs.getString("user_status", "В сети") ?: "В сети")
            val bioInput = findViewById<EditText>(R.id.profileBio)
            bioInput.setText(prefs.getString("user_bio", "") ?: "")
            findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
                prefs.edit().putString("user_status", statusInput.text.toString().trim())
                    .putString("user_bio", bioInput.text.toString().trim()).apply()
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }
            findViewById<Button>(R.id.btnClearCache).setOnClickListener {
                Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show()
            }
            findViewById<Button>(R.id.btnLogout).setOnClickListener {
                prefs.edit().clear().apply()
                finish()
            }
        } catch (e: Exception) {
            logToServer("CRASH: ProfileActivity - " + (e.message ?: "unknown"))
            Toast.makeText(this, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun logToServer(msg: String) {
        kotlin.concurrent.thread {
            try {
                val json = org.json.JSONObject().apply {
                    put("logs", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("timestamp", java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()))
                            put("message", msg); put("level", "ERROR")
                        })
                    })
                }
                val url = java.net.URL("http://2.26.71.102:8000/api/logs")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write(json.toString().toByteArray())
                conn.responseCode
            } catch (_: Exception) {}
        }
    }
}
