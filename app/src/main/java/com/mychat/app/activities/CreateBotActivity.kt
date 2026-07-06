package com.mychat.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateBotActivity : AppCompatActivity() {
    private var isAI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_create_bot)

            val radioAI = findViewById<LinearLayout>(R.id.radioAI)
            val radioSimple = findViewById<LinearLayout>(R.id.radioSimple)

            radioAI.setOnClickListener {
                isAI = true
                (radioAI.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_selected_bot)
                (radioSimple.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_unselected)
            }

            radioSimple.setOnClickListener {
                isAI = false
                (radioAI.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_unselected)
                (radioSimple.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_selected_bot)
            }

            findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

            findViewById<Button>(R.id.btnCreate).setOnClickListener {
                val name = findViewById<EditText>(R.id.botName).text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите имя бота", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val desc = findViewById<EditText>(R.id.botDesc).text.toString().trim()
                val helpCmd = findViewById<EditText>(R.id.botHelpCommand).text.toString().trim()
                setResult(RESULT_OK, intent.apply {
                    putExtra("botName", name)
                    putExtra("botDesc", desc)
                    putExtra("botType", if (isAI) "ai" else "simple")
                    putExtra("botHelp", helpCmd)
                })
                Toast.makeText(this, "Бот создан", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            logToServer("CRASH: CreateBotActivity - " + (e.message ?: "unknown"))
            Toast.makeText(this, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun logToServer(msg: String) {
        Thread {
            try {
                val json = org.json.JSONObject().apply {
                    put("logs", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("timestamp", java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()))
                            put("message", msg)
                            put("level", "ERROR")
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
        }.start()
    }
}
