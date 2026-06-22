package com.mychat.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.mychat.app.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileStatus: TextView
    private lateinit var statusInput: EditText
    private lateinit var editStatusSection: LinearLayout
    private val client = OkHttpClient()
    private var token = ""
    private var username = ""
    private var serverUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        token = intent.getStringExtra("token") ?: prefs.getString("token", "") ?: ""
        username = intent.getStringExtra("username") ?: prefs.getString("username", "User") ?: "User"
        serverUrl = prefs.getString("server_url", "http://2.26.71.102:8000") ?: "http://2.26.71.102:8000"

        // Имя
        findViewById<TextView>(R.id.profileName).text = username

        // Аватар
        findViewById<TextView>(R.id.profileAvatar).text = username.take(1).uppercase()

        // Статус
        profileStatus = findViewById(R.id.profileStatus)
        statusInput = findViewById(R.id.statusInput)
        editStatusSection = findViewById(R.id.editStatusSection)

        // Загружаем статус с сервера
        loadStatusFromServer()

        // Кнопка назад
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // Клик по статусу - открыть редактирование
        profileStatus.setOnClickListener {
            if (editStatusSection.visibility == View.VISIBLE) {
                editStatusSection.visibility = View.GONE
            } else {
                statusInput.setText(profileStatus.text)
                editStatusSection.visibility = View.VISIBLE
                statusInput.requestFocus()
            }
        }

        // Сохранить статус
        findViewById<Button>(R.id.saveStatusBtn).setOnClickListener {
            val newStatus = statusInput.text.toString().trim()
            if (newStatus.isNotEmpty()) {
                saveStatusToServer(newStatus)
            } else {
                Toast.makeText(this, "Введите статус", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка выхода
        findViewById<Button>(R.id.logoutBtn).setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadStatusFromServer() {
        if (token.isEmpty()) return

        val request = Request.Builder()
            .url("$serverUrl/users/$username?token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this@ProfileActivity)
                    val status = prefs.getString("user_status", "Живу в облаках ☁️") ?: "Живу в облаках ☁️"
                    profileStatus.text = status
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        var userStatus = "Живу в облаках ☁️"
                        for (i in 0 until json.length()) {
                            val user = json.getJSONObject(i)
                            if (user.optString("username") == username) {
                                userStatus = user.optString("status_text", "Живу в облаках ☁️")
                                break
                            }
                        }
                        val finalStatus = userStatus
                        runOnUiThread {
                            profileStatus.text = finalStatus
                            val prefs = PreferenceManager.getDefaultSharedPreferences(this@ProfileActivity)
                            prefs.edit().putString("user_status", finalStatus).apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun saveStatusToServer(newStatus: String) {
        if (token.isEmpty()) {
            Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Сохранение...", Toast.LENGTH_SHORT).show()

        val json = JSONObject().apply {
            put("bio", newStatus)
            put("status_text", newStatus)
            put("avatar_url", "")
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$serverUrl/update_profile?token=$token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        profileStatus.text = newStatus
                        editStatusSection.visibility = View.GONE
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@ProfileActivity)
                        prefs.edit().putString("user_status", newStatus).apply()
                        Toast.makeText(this@ProfileActivity, "Статус обновлён!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
