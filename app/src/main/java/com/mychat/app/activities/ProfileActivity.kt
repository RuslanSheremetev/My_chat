package com.mychat.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mychat.app.R

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile_feder)
        } catch (e: Exception) {
            // Если новая разметка не грузится — показываем старую
            setContentView(R.layout.activity_feder_main)
        }
    }
}
