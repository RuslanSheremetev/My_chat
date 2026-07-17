package com.mychat.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateGroupActivity : AppCompatActivity() {
    private var isPrivate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_create_group)

            val radioPublic = findViewById<LinearLayout>(R.id.radioPublic)
            val radioPrivate = findViewById<LinearLayout>(R.id.radioPrivate)

            radioPublic.setOnClickListener {
                isPrivate = false
                (radioPublic.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_selected_group)
                (radioPrivate.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_unselected)
            }

            radioPrivate.setOnClickListener {
                isPrivate = true
                (radioPublic.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_unselected)
                (radioPrivate.getChildAt(0) as View).setBackgroundResource(R.drawable.radio_selected_group)
            }

            findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

            findViewById<Button>(R.id.btnCreate).setOnClickListener {
                val name = findViewById<EditText>(R.id.groupName).text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val desc = findViewById<EditText>(R.id.groupDesc).text.toString().trim()
                setResult(RESULT_OK, intent.apply {
                    putExtra("groupName", name)
                    putExtra("groupDesc", desc)
                    putExtra("isPrivate", isPrivate)
                })
                Toast.makeText(this, "Группа создана", Toast.LENGTH_SHORT).show()
                finish()
            }

            findViewById<Button>(R.id.btnAddMember).setOnClickListener {
                Toast.makeText(this, "Добавление участников", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            logToServer("CRASH: CreateGroupActivity - " + (e.message ?: "unknown"))
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
                val url = java.net.URL("http://2.26.71.102:8001/api/logs")
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
