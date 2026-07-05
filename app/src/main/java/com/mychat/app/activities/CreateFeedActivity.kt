package com.mychat.app.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateFeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_create_feed)
            var isPrivate = false
            val pb = findViewById<Button>(R.id.radioPublic)
            val prb = findViewById<Button>(R.id.radioPrivate)
            pb.setOnClickListener { isPrivate = false; pb.setTextColor(0xffFF9500.toInt()); prb.setTextColor(0xff777777.toInt()) }
            prb.setOnClickListener { isPrivate = true; prb.setTextColor(0xffFF9500.toInt()); pb.setTextColor(0xff777777.toInt()) }
            findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
            findViewById<Button>(R.id.btnCreate).setOnClickListener {
                val name = findViewById<EditText>(R.id.feedName).text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                setResult(RESULT_OK, android.content.Intent().apply {
                    putExtra("feedName", name); putExtra("feedDesc", findViewById<EditText>(R.id.feedDesc).text.toString().trim())
                    putExtra("isPrivate", isPrivate)
                })
                Toast.makeText(this, "Лента создана", Toast.LENGTH_SHORT).show(); finish()
            }
            findViewById<Button>(R.id.btnAddSubscriber).setOnClickListener {
                Toast.makeText(this, "Добавление подписчиков", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            logToServer("CRASH: CreateFeedActivity - " + (e.message ?: "unknown"))
            Toast.makeText(this, "Ошибка: " + e.message, Toast.LENGTH_LONG).show(); finish()
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
                conn.requestMethod = "POST"; conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true; conn.outputStream.write(json.toString().toByteArray()); conn.responseCode
            } catch (_: Exception) {}
        }
    }
}
