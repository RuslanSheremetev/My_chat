package com.mychat.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mychat.app.fragments.ChatListFragment
import com.mychat.app.fragments.ProfileFragment

class FederMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feder_main)

        // Загружаем чаты по умолчанию
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, ChatListFragment())
                .commit()
        }

        // Нижнее меню
        findViewById<LinearLayout>(R.id.navChats).setOnClickListener {
            showFragment(ChatListFragment())
        }
        findViewById<LinearLayout>(R.id.navStories).setOnClickListener {
            Toast.makeText(this, "Stories — скоро", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.navContacts).setOnClickListener {
            Toast.makeText(this, "Contacts — скоро", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            showFragment(ProfileFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
