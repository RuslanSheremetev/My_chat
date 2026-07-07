package com.mychat.app.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {
    private val PICK_AVATAR = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile_new)

            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val username = prefs.getString("username", "A") ?: "A"

            val avatar = findViewById<TextView>(R.id.profileAvatar)
            avatar.text = username.take(1).uppercase()

            // Загружаем сохранённый аватар
            val savedAvatarPath = prefs.getString("avatar_path", "")
            if (savedAvatarPath.isNotEmpty()) {
                try {
                    val bmp = BitmapFactory.decodeFile(savedAvatarPath)
                    if (bmp != null) {
                        val roundedBmp = android.graphics.drawable.BitmapDrawable(resources, getRoundedBitmap(bmp))
                        avatar.background = roundedBmp
                        avatar.text = ""
                    }
                } catch (_: Exception) {}
            }

            // Клик по аватарке — выбор фото
            avatar.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_AVATAR)
            }

            findViewById<TextView>(R.id.profileName).text = prefs.getString("user_name", username) ?: username
            findViewById<TextView>(R.id.profileUsername).text = "@$username"

            val statusInput = findViewById<EditText>(R.id.profileStatus)
            statusInput.setText(prefs.getString("user_status", "В сети") ?: "В сети")

            val bioInput = findViewById<EditText>(R.id.profileBio)
            bioInput.setText(prefs.getString("user_bio", "") ?: "")

            findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
                prefs.edit()
                    .putString("user_status", statusInput.text.toString().trim())
                    .putString("user_bio", bioInput.text.toString().trim())
                    .apply()
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }

            findViewById<Button>(R.id.btnLogout).setOnClickListener {
                prefs.edit().clear().apply()
                val intent = Intent(this, com.mychat.app.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("logout", true)
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    val avatar = findViewById<TextView>(R.id.profileAvatar)
                    val roundedBmp = android.graphics.drawable.BitmapDrawable(resources, getRoundedBitmap(bmp))
                    avatar.background = roundedBmp
                    avatar.text = ""

                    // Сохраняем путь к аватарке локально
                    val path = saveBitmapToCache(bmp)
                    android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putString("avatar_path", path).apply()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getRoundedBitmap(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply { isAntiAlias = true }
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, null, rect, paint)
        return output
    }

    private fun saveBitmapToCache(bitmap: android.graphics.Bitmap): String {
        val file = java.io.File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
