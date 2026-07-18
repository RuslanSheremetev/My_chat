package com.mychat.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.adapters.FederChatAdapter
import com.mychat.app.models.User
import com.mychat.app.network.ApiClient
import com.mychat.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FederMainActivity : AppCompatActivity() {
    private lateinit var chatList: RecyclerView
    private var chatAdapter: FederChatAdapter? = null
    private lateinit var loginLayout: LinearLayout
    private lateinit var mainContainer: FrameLayout
    private lateinit var loginUser: EditText
    private lateinit var loginPass: EditText
    private val users = mutableListOf<User>()
    private val server = Constants.SERVER_URL
    private var token = ""
    private var me = ""
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_feder_main)
            chatList = findViewById(R.id.chatList)
            loginLayout = findViewById(R.id.loginLayout)
            mainContainer = findViewById(R.id.mainContainer)
            loginUser = findViewById(R.id.loginUser)
            loginPass = findViewById(R.id.loginPass)
            chatList.layoutManager = LinearLayoutManager(this)
            findViewById<Button>(R.id.btnLogin).setOnClickListener { login() }
            
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            token = prefs.getString("token", "") ?: ""
            me = prefs.getString("username", "") ?: ""
            
            if (token.isEmpty()) {
                Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            loginLayout.visibility = View.GONE
            mainContainer.visibility = View.VISIBLE
            loadUsers()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun login() {
        val u = loginUser.text.toString().trim()
        val p = loginPass.text.toString().trim()
        if (u.isEmpty() || p.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply { put("username", u); put("password", p) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val r = client.newCall(Request.Builder().url("$server/login").post(body).build()).execute()
                if (r.isSuccessful) {
                    val d = JSONObject(r.body!!.string())
                    token = d.optString("access_token", "")
                    me = d.optString("username", u)
                    PreferenceManager.getDefaultSharedPreferences(this@FederMainActivity).edit()
                        .putString("token", token).putString("username", me).putString("server_url", server).apply()
                    loadUsersFromApi()
                    runOnUiThread { loginLayout.visibility = View.GONE; mainContainer.visibility = View.VISIBLE }
                } else {
                    runOnUiThread { Toast.makeText(this@FederMainActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@FederMainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun loadUsers() { CoroutineScope(Dispatchers.IO).launch { loadUsersFromApi() } }

    private fun loadUsersFromApi() {
        try {
            val r = ApiClient.get("$server/users/$me", token)
            if (r.isSuccessful) {
                val arr = JSONArray(r.body!!.string())
                val list = mutableListOf<User>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(User(username = o.optString("username"), name = o.optString("name", "").ifEmpty { o.optString("username") },
                        avatarColor = o.optString("avatar_color", "#2AABEE"), online = o.optBoolean("online"),
                        isBot = o.optBoolean("is_bot"), isGroup = o.optBoolean("is_group"), isFeed = o.optBoolean("is_feed"), unread = o.optInt("unread")))
                }
                runOnUiThread {
                    users.clear(); users.addAll(list)
                    if (chatAdapter == null) { chatAdapter = FederChatAdapter { }; chatList.adapter = chatAdapter }
                    chatAdapter?.update(users)
                }
            }
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show() }
        }
    }
}
