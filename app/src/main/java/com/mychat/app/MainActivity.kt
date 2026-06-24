package com.mychat.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.activities.FavoritesActivity
import com.mychat.app.activities.ProfileActivity
import com.mychat.app.adapters.ChatAdapter
import com.mychat.app.adapters.MessageAdapter
import com.mychat.app.models.ChatMessage
import com.mychat.app.models.User
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var loginLayout: LinearLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var loginUser: EditText
    private lateinit var loginPass: EditText
    private lateinit var serverUrl: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var chatList: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var btnSearchClear: ImageButton
    private lateinit var navChats: LinearLayout
    private lateinit var navFavorites: LinearLayout
    private lateinit var navProfile: LinearLayout
    private lateinit var chatLayout: LinearLayout
    private lateinit var messagesList: RecyclerView
    private lateinit var msgInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttach: ImageButton
    private lateinit var chatTitle: TextView
    private lateinit var chatStatus: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var bottomNav: LinearLayout
    private lateinit var btnCreate: ImageButton

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var token = ""
    private var me = ""
    private var server = "http://2.26.71.102:8000"
    private var selId = ""
    private var chatAdapter = ChatAdapter { user -> openChat(user.username) }
    private var messageAdapter: MessageAdapter? = null
    private var ws: WebSocket? = null
    private var currentTab = 0

    companion object {
        private const val REQUEST_CODE_STORAGE = 1001
        private const val REQUEST_CODE_FILE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupNavigation()
        setupSearch()
        setupListeners()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val savedToken = prefs.getString("token", "")
        val savedUser = prefs.getString("username", "")
        if (!savedToken.isNullOrEmpty() && !savedUser.isNullOrEmpty()) {
            token = savedToken
            me = savedUser
            server = prefs.getString("server_url", "http://2.26.71.102:8000") ?: "http://2.26.71.102:8000"
            serverUrl.setText(server)
            loginLayout.visibility = View.GONE
            mainContainer.visibility = View.VISIBLE
            bottomNav.visibility = View.VISIBLE
            loadUsers()
            connectWebSocket()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mainContainer.isInitialized && mainContainer.visibility == View.VISIBLE) {
            showTab(0)
        }
    }

    private fun initViews() {
        loginLayout = findViewById(R.id.loginLayout)
        mainContainer = findViewById(R.id.mainContainer)
        loginUser = findViewById(R.id.loginUser)
        loginPass = findViewById(R.id.loginPass)
        serverUrl = findViewById(R.id.serverUrl)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        chatList = findViewById(R.id.chatList)
        searchInput = findViewById(R.id.searchInput)
        btnSearchClear = findViewById(R.id.btnSearchClear)
        navChats = findViewById(R.id.navChats)
        navFavorites = findViewById(R.id.navFavorites)
        navProfile = findViewById(R.id.navProfile)
        chatLayout = findViewById(R.id.chatLayout)
        messagesList = findViewById(R.id.messagesList)
        msgInput = findViewById(R.id.msgInput)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        chatTitle = findViewById(R.id.chatTitle)
        chatStatus = findViewById(R.id.chatStatus)
        btnBack = findViewById(R.id.btnBack)
        bottomNav = findViewById(R.id.bottomNav)
        btnCreate = findViewById(R.id.btnCreate)

        chatList.layoutManager = LinearLayoutManager(this)
        chatList.adapter = chatAdapter
        messagesList.layoutManager = LinearLayoutManager(this)
    }

    private fun setupNavigation() {
        navChats.setOnClickListener { showTab(0) }
        navFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java).apply {
                putExtra("token", token)
                putExtra("username", me)
            })
        }
        navProfile.setOnClickListener { showTab(2) }
        showTab(0)
    }

    private fun showTab(tab: Int) {
        currentTab = tab
        when (tab) {
            0 -> {
                findViewById<LinearLayout>(R.id.chatsScreen)?.visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.profileScreen)?.visibility = View.GONE
                mainContainer.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
                chatLayout.visibility = View.GONE
            }
            2 -> {
                findViewById<LinearLayout>(R.id.chatsScreen)?.visibility = View.GONE
                findViewById<LinearLayout>(R.id.profileScreen)?.visibility = View.VISIBLE
                loadProfile()
            }
        }
    }

    private fun loadProfile() {
        val avatar = findViewById<TextView>(R.id.profileAvatar)
        val name = findViewById<TextView>(R.id.profileName)
        val bio = findViewById<TextView>(R.id.profileBio)
        avatar.text = me.take(1).uppercase()
        name.text = me
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val status = prefs.getString("user_status", "No bio") ?: "No bio"
        bio.text = status
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                if (q.isNotEmpty()) {
                    btnSearchClear.visibility = View.VISIBLE
                    chatAdapter.filter(q)
                } else {
                    btnSearchClear.visibility = View.GONE
                    chatAdapter.filter("")
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSearchClear.setOnClickListener {
            searchInput.text.clear()
            btnSearchClear.visibility = View.GONE
            chatAdapter.filter("")
        }
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener { login() }
        btnRegister.setOnClickListener { register() }

        btnSend.setOnClickListener {
            val text = msgInput.text.toString().trim()
            if (text.isNotEmpty() && selId.isNotEmpty()) {
                sendMessage(selId, text)
                msgInput.text.clear()
            }
        }

        btnCreate.setOnClickListener { showCreateMenu() }
        btnBack.setOnClickListener { closeChat() }
        btnAttach.setOnClickListener { pickFile() }
        findViewById<Button>(R.id.btnSaveProfile)?.setOnClickListener { saveProfile() }
    }

    private fun login() {
        val user = loginUser.text.toString().trim()
        val pass = loginPass.text.toString().trim()
        val url = serverUrl.text.toString().trim()

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        server = url
        val json = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }

        val request = Request.Builder()
            .url("$server/login")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONObject(body.string())
                        val token = json.optString("access_token")
                        val username = json.optString("username")

                        if (token.isNotEmpty()) {
                            runOnUiThread {
                                this@MainActivity.token = token
                                this@MainActivity.me = username
                                val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                                prefs.edit().apply {
                                    putString("token", token)
                                    putString("username", username)
                                    putString("server_url", server)
                                }.apply()
                                loginLayout.visibility = View.GONE
                                mainContainer.visibility = View.VISIBLE
                                bottomNav.visibility = View.VISIBLE
                                loadUsers()
                                connectWebSocket()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun register() {
        val user = loginUser.text.toString().trim()
        val pass = loginPass.text.toString().trim()
        val url = serverUrl.text.toString().trim()

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        server = url
        val json = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }

        val request = Request.Builder()
            .url("$server/register")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONObject(body.string())
                        val token = json.optString("access_token")
                        val username = json.optString("username")

                        if (token.isNotEmpty()) {
                            runOnUiThread {
                                this@MainActivity.token = token
                                this@MainActivity.me = username
                                val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                                prefs.edit().apply {
                                    putString("token", token)
                                    putString("username", username)
                                    putString("server_url", server)
                                }.apply()
                                loginLayout.visibility = View.GONE
                                mainContainer.visibility = View.VISIBLE
                                bottomNav.visibility = View.VISIBLE
                                loadUsers()
                                connectWebSocket()
                                Toast.makeText(this@MainActivity, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun loadUsers() {
        val request = Request.Builder()
            .url("$server/users/$me?token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        val users = mutableListOf<User>()
                        for (i in 0 until json.length()) {
                            val obj = json.getJSONObject(i)
                            val username = obj.optString("username")
                            val isGroup = obj.optBoolean("is_group", false)
                            val isFeed = obj.optBoolean("is_feed", false)
                            val name = if (isGroup) obj.optString("name", username) else username
                            val user = User(
                                username = username,
                                avatarColor = obj.optString("avatar_color", "#2AABEE"),
                                online = obj.optBoolean("online", false),
                                lastSeen = obj.optString("last_seen", ""),
                                bio = obj.optString("bio", ""),
                                avatarUrl = obj.optString("avatar_url", ""),
                                isGroup = isGroup,
                                isFeed = isFeed,
                                name = name
                            )
                            users.add(user)
                        }
                        runOnUiThread {
                            chatAdapter.update(users)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://${server.replace("http://", "").replace("https://", "")}/ws/$me?token=$token")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")
                        when (type) {
                            "message" -> {
                                if (selId.isNotEmpty()) {
                                    val msg = ChatMessage(
                                        id = json.optString("id"),
                                        from = json.optString("from"),
                                        to = json.optString("to"),
                                        text = json.optString("text"),
                                        time = json.optString("time")
                                    )
                                    messageAdapter?.addMessage(msg)
                                    messagesList.scrollToPosition((messageAdapter?.itemCount ?: 0) - 1)
                                }
                            }
                            "typing" -> {
                                chatStatus.text = "печатает..."
                                handler.removeCallbacksAndMessages(null)
                                handler.postDelayed({ chatStatus.text = "online" }, 2000)
                            }
                            "users_update" -> {
                                loadUsers()
                            }
                            "status_update" -> {
                                loadUsers()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.postDelayed({ connectWebSocket() }, 3000)
            }
        })
    }

    private fun sendMessage(to: String, text: String) {
        val json = JSONObject().apply {
            put("type", "private")
            put("to", to)
            put("text", text)
        }
        ws?.send(json.toString())

        val msg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            from = me,
            to = to,
            text = text,
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
        messageAdapter?.addMessage(msg)
        messagesList.scrollToPosition((messageAdapter?.itemCount ?: 0) - 1)
    }

    private fun openChat(username: String) {
        selId = username
        chatTitle.text = username
        chatStatus.text = "online"
        chatLayout.visibility = View.VISIBLE
        mainContainer.visibility = View.GONE
        bottomNav.visibility = View.GONE

        loadMessages(username)
    }

    private fun closeChat() {
        selId = ""
        chatLayout.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        messageAdapter = null
    }

    private fun loadMessages(username: String) {
        val request = Request.Builder()
            .url("$server/messages/$username?me=$me&token=$token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        val messages = mutableListOf<ChatMessage>()
                        for (i in 0 until json.length()) {
                            val obj = json.getJSONObject(i)
                            val msg = ChatMessage(
                                id = obj.optString("id"),
                                from = obj.optString("from"),
                                to = obj.optString("to"),
                                text = obj.optString("text"),
                                time = obj.optString("time")
                            )
                            messages.add(msg)
                        }
                        runOnUiThread {
                            messageAdapter = MessageAdapter(messages, me)
                            messagesList.adapter = messageAdapter
                            messagesList.scrollToPosition(messages.size - 1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun showCreateMenu() {
        AlertDialog.Builder(this)
            .setTitle("Создать")
            .setItems(arrayOf("Группу", "Ленту")) { _, which ->
                when (which) {
                    0 -> createGroup()
                    1 -> createFeed()
                }
            }
            .show()
    }

    private fun createGroup() {
        val input = EditText(this)
        input.hint = "Название группы"
        AlertDialog.Builder(this)
            .setTitle("Создать группу")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val json = JSONObject().apply {
                        put("type", "create_group")
                        put("name", name)
                        put("members", JSONArray().apply { put(me) })
                        put("private", false)
                    }
                    ws?.send(json.toString())
                    Toast.makeText(this, "Группа создаётся", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createFeed() {
        val input = EditText(this)
        input.hint = "Название ленты"
        AlertDialog.Builder(this)
            .setTitle("Создать ленту")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val json = JSONObject().apply {
                        put("type", "create_feed")
                        put("name", name)
                        put("description", "")
                        put("private", false)
                    }
                    ws?.send(json.toString())
                    Toast.makeText(this, "Лента создаётся", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                uploadFile(uri)
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Ошибка чтения", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = getFileName(uri) ?: "file"
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, bytes.toRequestBody("*/*".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("$server/upload?token=$token")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.let { body ->
                        try {
                            val json = JSONObject(body.string())
                            val url = json.optString("url")
                            if (url.isNotEmpty()) {
                                runOnUiThread {
                                    val text = if (isImageFile(fileName)) "📷 Фото: $url" else "📎 $fileName: $url"
                                    sendMessage(selId, text)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }

    private fun isImageFile(fileName: String): Boolean {
        val extensions = listOf("jpg", "jpeg", "png", "gif", "webp")
        val ext = fileName.substringAfterLast('.').lowercase()
        return extensions.contains(ext)
    }

    private fun saveProfile() {
        val bio = findViewById<EditText>(R.id.editBio)?.text?.toString()?.trim()
        if (!bio.isNullOrEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().putString("user_status", bio).apply()
            Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ws?.close(1000, null)
    }
}
