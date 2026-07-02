package com.mychat.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.MainActivity
import com.mychat.app.R

class QrLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_login)

        findViewById<android.widget.TextView>(R.id.btnGoToLogin).setOnClickListener {
            // Открываем обычный экран логина
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<android.widget.Button>(R.id.btnScanQr).setOnClickListener {
            // TODO: сканирование QR
            android.widget.Toast.makeText(this, "QR-сканер будет позже", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
