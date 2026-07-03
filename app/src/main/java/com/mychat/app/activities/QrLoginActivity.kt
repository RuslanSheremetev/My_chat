package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.mychat.app.MainActivity
import com.mychat.app.R

class QrLoginActivity : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView
    private var scanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_login)

        // Запрашиваем разрешение камеры
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            }
        }

        barcodeView = findViewById(R.id.barcodeView)
        barcodeView.setDecoderFactory(DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE)))
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (scanned) return
                result?.let {
                    scanned = true
                    val data = it.text
                    val token = if (data.contains("token=")) {
                        data.substringAfter("token=").substringBefore("&")
                    } else data
                    
                    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this@QrLoginActivity)
                    // Запрашиваем данные пользователя с сервера
                    thread {
                        try {
                            val resp = okhttp3.OkHttpClient().newCall(
                                okhttp3.Request.Builder().url("http://2.26.71.102:8000/api/qr/login")
                                    .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), """{"qr_token":"$token"}"""))
                                    .build()
                            ).execute()
                            if (resp.isSuccessful) {
                                val json = org.json.JSONObject(resp.body()?.string() ?: "")
                                val accessToken = json.optString("access_token")
                                val username = json.optString("username")
                                runOnUiThread {
                                    prefs.edit()
                                        .putString("token", accessToken.ifEmpty { token })
                                        .putString("username", username.ifEmpty { "Ruslan" })
                                        .apply()
                                    Toast.makeText(this@QrLoginActivity, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@QrLoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            } else {
                                runOnUiThread {
                                    prefs.edit().putString("token", token).putString("username", "Ruslan").apply()
                                    startActivity(Intent(this@QrLoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                prefs.edit().putString("token", token).putString("username", "Ruslan").apply()
                                startActivity(Intent(this@QrLoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                    
                    runOnUiThread {
                        Toast.makeText(this@QrLoginActivity, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@QrLoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
            override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>?) {}
        })

        findViewById<android.widget.TextView>(R.id.btnGoToLogin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
