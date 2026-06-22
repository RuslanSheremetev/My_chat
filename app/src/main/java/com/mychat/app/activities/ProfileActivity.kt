package com.mychat.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
    private lateinit var profileAvatar: TextView
    private lateinit var onlineIndicator: View
    private lateinit var onlineText: TextView
    private val client = OkHttpClient()
    private var token = ""
    private var username = ""
    private var serverUrl = ""

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        token = intent.getStringExtra("token") ?: prefs.getString("token", "") ?: ""
        username = intent.getStringExtra("username") ?: prefs.getString("username", "User") ?: "User"
        serverUrl = prefs.getString("server_url", "http://2.26.71.102:8000") ?: "http://2.26.71.102:8000"

        findViewById<TextView>(R.id.profileName).text = username

        // Аватар
        profileAvatar = findViewById(R.id.profileAvatar)
        profileAvatar.text = username.take(1).uppercase()
        profileAvatar.setOnClickListener {
            pickImage()
        }

        // Онлайн индикатор
        onlineIndicator = findViewById(R.id.onlineIndicator)
        onlineText = findViewById(R.id.onlineText)
        // Показываем онлайн всегда (потом можно будет получать с сервера)
        onlineIndicator.setBackgroundResource(R.drawable.bg_online_dot)
        onlineText.text = "Онлайн"

        // Статус
        profileStatus = findViewById(R.id.profileStatus)
        statusInput = findViewById(R.id.statusInput)
        editStatusSection = findViewById(R.id.editStatusSection)

        val savedStatus = prefs.getString("user_status", "Живу в облаках ☁️") ?: "Живу в облаках ☁️"
        profileStatus.text = savedStatus

        // Загружаем данные с сервера (аватар, статус, онлайн)
        loadUserData()

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        profileStatus.setOnClickListener {
            if (editStatusSection.visibility == View.VISIBLE) {
                editStatusSection.visibility = View.GONE
            } else {
                statusInput.setText(profileStatus.text)
                editStatusSection.visibility = View.VISIBLE
                statusInput.requestFocus()
            }
        }

        findViewById<Button>(R.id.saveStatusBtn).setOnClickListener {
            val newStatus = statusInput.text.toString().trim()
            if (newStatus.isNotEmpty()) {
                saveStatus(newStatus)
            } else {
                Toast.makeText(this, "Введите статус", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.logoutBtn).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        if (token.isEmpty()) return

        val request = Request.Builder()
            .url("$serverUrl/users/$username?token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // Используем локальные данные
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this@ProfileActivity)
                    val status = prefs.getString("user_status", "Живу в облаках ☁️") ?: "Живу в облаках ☁️"
                    profileStatus.text = status
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        for (i in 0 until json.length()) {
                            val user = json.getJSONObject(i)
                            if (user.optString("username") == username) {
                                val statusText = user.optString("status_text", "")
                                val avatarUrl = user.optString("avatar_url", "")
                                val isOnline = user.optBoolean("online", true)

                                runOnUiThread {
                                    if (statusText.isNotEmpty()) {
                                        profileStatus.text = statusText
                                    }
                                    if (avatarUrl.isNotEmpty()) {
                                        profileAvatar.text = username.take(1).uppercase()
                                        // Можно загрузить реальное фото
                                    }
                                    // Обновляем онлайн статус
                                    if (isOnline) {
                                        onlineIndicator.setBackgroundResource(R.drawable.bg_online_dot)
                                        onlineText.text = "Онлайн"
                                    } else {
                                        onlineIndicator.setBackgroundResource(R.drawable.bg_offline_dot)
                                        onlineText.text = "Офлайн"
                                    }
                                }
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                uploadAvatar(uri)
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        if (token.isEmpty()) {
            Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show()

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show()
                return
            }

            var fileName = "avatar.jpg"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "avatar.jpg"
                    }
                }
            }

            // Загружаем через upload
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/upload?token=$token")
                .post(requestBody)
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
                            try {
                                val json = JSONObject(response.body?.string() ?: "{}")
                                val fileUrl = json.optString("url", "")
                                if (fileUrl.isNotEmpty()) {
                                    // Обновляем аватар (пока инициалы)
                                    profileAvatar.text = username.take(1).uppercase()
                                    Toast.makeText(this@ProfileActivity, "Аватар обновлён!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@ProfileActivity, "Ошибка обработки", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@ProfileActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveStatus(newStatus: String) {
        if (token.isEmpty()) {
            Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        profileStatus.text = newStatus
        editStatusSection.visibility = View.GONE
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("user_status", newStatus).apply()
        
        Toast.makeText(this, "Статус обновлён!", Toast.LENGTH_SHORT).show()

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
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }
}
