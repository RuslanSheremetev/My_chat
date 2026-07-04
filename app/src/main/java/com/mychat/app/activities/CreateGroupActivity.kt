package com.mychat.app.activities

import android.os.Bundle
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class CreateGroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            val name = findViewById<EditText>(R.id.groupName).text.toString().trim()
            val desc = findViewById<EditText>(R.id.groupDesc).text.toString().trim()
            val isPrivate = findViewById<SwitchMaterial>(R.id.switchPrivate).isChecked

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Отправляем через Intent обратно в MainActivity
            val data = android.content.Intent().apply {
                putExtra("groupName", name)
                putExtra("groupDesc", desc)
                putExtra("isPrivate", isPrivate)
            }
            setResult(RESULT_OK, data)
            Toast.makeText(this, "Группа «$name» создана", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnAddMember).setOnClickListener {
            Toast.makeText(this, "Добавление участников", Toast.LENGTH_SHORT).show()
        }
    }
}
