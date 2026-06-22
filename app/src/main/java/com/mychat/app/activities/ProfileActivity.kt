package com.mychat.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileName: TextView
    private lateinit var profileStatus: TextView
    private lateinit var profileBio: TextView
    private lateinit var statusInput: EditText
    private lateinit var editStatusSection: LinearLayout
    private lateinit var themeSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var themeDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDark = prefs.getBoolean("dark_theme", true)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_new)

        initViews()
        setupListeners()
        loadUserData()
    }

    private fun initViews() {
        profileName = findViewById(R.id.profileName)
        profileStatus = findViewById(R.id.profileStatus)
        profileBio = findViewById(R.id.profileBio)
        statusInput = findViewById(R.id.statusInput)
        editStatusSection = findViewById(R.id.editStatusSection)
        themeSwitch = findViewById(R.id.themeSwitch)
        themeDesc = findViewById(R.id.themeDesc)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDark = prefs.getBoolean("dark_theme", true)
        themeSwitch.isChecked = isDark
        updateThemeDesc(isDark)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        profileStatus.setOnClickListener {
            editStatusSection.visibility = if (editStatusSection.visibility == View.VISIBLE) {
                View.GONE
            } else {
                statusInput.setText(profileStatus.text)
                View.VISIBLE
            }
        }

        findViewById<Button>(R.id.saveStatusBtn).setOnClickListener {
            val newStatus = statusInput.text.toString().trim()
            if (newStatus.isNotEmpty()) {
                profileStatus.text = newStatus
                editStatusSection.visibility = View.GONE
                Toast.makeText(this, "Статус обновлён!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Введите статус", Toast.LENGTH_SHORT).show()
            }
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            updateThemeDesc(isChecked)
            recreate()
        }

        findViewById<LinearLayout>(R.id.menuLanguage).setOnClickListener {
            Toast.makeText(this, "Язык: Русский", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuHelp).setOnClickListener {
            Toast.makeText(this, "Помощь и поддержка", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.logoutBtn).setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().clear().apply()
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<LinearLayout>(R.id.navChats).setOnClickListener {
            Toast.makeText(this, "Чаты", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navFavorites).setOnClickListener {
            Toast.makeText(this, "Избранное", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Профиль", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val username = prefs.getString("username", "Ruslan") ?: "Ruslan"
        profileName.text = username
        findViewById<TextView>(R.id.profileAvatar).text = username.take(1).uppercase()
    }

    private fun updateThemeDesc(isDark: Boolean) {
        themeDesc.text = if (isDark) {
            "Тёмная тема (активна)"
        } else {
            "Светлая тема (активна)"
        }
    }
}
