package com.mychat.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.activities.FavoritesActivity
import com.mychat.app.activities.ProfileActivity
import com.mychat.app.adapters.ChatAdapter
import com.mychat.app.adapters.MessageAdapter
import com.mychat.app.adapters.circleBg
import com.mychat.app.models.ChatMessage
import com.mychat.app.models.FileInfo
import com.mychat.app.models.User
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var loginLayout: LinearLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private lateinit var chatsScreen: LinearLayout
    private lateinit var profileScreen: ScrollView
    private lateinit var chatLayout: LinearLayout
    private lateinit var loginUser: EditText
    private lateinit var loginPass: EditText
    private lateinit var serverUrl: EditText
    private lateinit var searchInput: EditText
    private lateinit var chatList: RecyclerView
    private lateinit var messagesList: RecyclerView
    private lateinit var msgInput: EditText
    private lateinit var chatTitle: TextView
    private lateinit var chatAvatar: TextView
    private lateinit var chatStatus: TextView
    private lateinit var btnSearchClear: ImageButton
    private lateinit var profileAvatar: TextView
    private lateinit var profileName: TextView
    private lateinit var profileBio: TextView
    private lateinit var editBio: EditText
    private lateinit var navChats: LinearLayout
    private lateinit var navFavorites: LinearLayout
    private lateinit var navProfile: LinearLayout
    private var server = "http://2.26.71.102:8000"
    private var token = ""
    private var me = ""
    private var selId = ""
    private var ws: WebSocket? = null
    private val users = mutableListOf<User>()
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var msgAdapter: MessageAdapter
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        loginLayout = findViewById(R.id.loginLayout)
        mainContainer = findViewById(R.id.mainContainer)
        bottomNav = findViewById(R.id.bottomNav)
        chatsScreen = findViewById(R.id.chatsScreen)
        profileScreen = findViewById(R.id.profileScreen)
        chatLayout = findViewById(R.id.chatLayout)
        loginUser = findViewById(R.id.loginUser)
        loginPass = findViewById(R.id.loginPass)
        serverUrl = findViewById(R.id.serverUrl)
        searchInput = findViewById(R.id.searchInput)
        chatList = findViewById(R.id.chatList)
        messagesList = findViewById(R.id.messagesList)
        msgInput = findViewById(R.id.msgInput)
        chatTitle = findViewById(R.id.chatTitle)
        chatAvatar = findViewById(R.id.chatAvatar)
        chatStatus = findViewById(R.id.chatStatus)
        btnSearchClear = findViewById(R.id.btnSearchClear)
        profileAvatar = findViewById(R.id.profileAvatar)
        profileName = findViewById(R.id.profileName)
        profileBio = findViewById(R.id.profileBio)
        editBio = findViewById(R.id.editBio)
        navChats = findViewById(R.id.navChats)
        navFavorites = findViewById(R.id.navFavorites)
        navProfile = findViewById(R.id.navProfile)
        
        serverUrl.setText(server)
        chatAdapter = ChatAdapter { user -> openChat(user.username) }
        chatList.layoutManager = LinearLayoutManager(this)
        chatList.adapter = chatAdapter
        
        msgAdapter = MessageAdapter(me) { url, name -> downloadFile(url, name) }
        messagesList.layoutManager = LinearLayoutManager(this)
        messagesList.adapter = msgAdapter
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener { login() }
        findViewById<Button>(R.id.btnRegister).setOnClickListener { register() }
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener { sendMessage() }
        findViewById<ImageButton>(R.id.btnAttach).setOnClickListener { pickFile() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { closeChat() }
        findViewById<ImageButton>(R.id.btnCreate).setOnClickListener { showCreateMenu() }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        
        navChats.setOnClickListener { showTab(0) }
        navFavorites.setOnClickListener { openFavorites() }
        navProfile.setOnClickListener { openProfile() }
        
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                if (q.isNotEmpty()) {
                    searchUsers(q)
                    btnSearchClear.visibility = View.VISIBLE
                } else {
                    btnSearchClear.visibility = View.GONE
                    loadUsers()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        btnSearchClear.setOnClickListener {
            searchInput.text.clear()
            hideKeyboard()
        }
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        token = prefs.getString("token", "") ?: ""
        me = prefs.getString("username", "") ?: ""
        server = prefs.getString("server_url", server) ?: server
        
        if (token.isNotEmpty() && me.isNotEmpty()) {
            showMain()
        }
    }

    override fun onResume() {
        super.onResume()
        chatsScreen.visibility = View.VISIBLE
        profileScreen.visibility = View.GONE
        loadUsers()
    }

    private fun openFavorites() {
        val intent = Intent(this, FavoritesActivity::class.java).apply {
            putExtra("token", token)
            putExtra("username", me)
        }
        startActivity(intent)
    }

    private fun openProfile() {
        try {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("token", token)
                putExtra("username", me)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTab(tab: Int) {
        chatsScreen.visibility = if (tab == 0) View.VISIBLE else View.GONE
        profileScreen.visibility = if (tab == 2) View.VISIBLE else View.GONE
        if (tab == 2) loadProfile()
    }

    private fun loadProfile() {
        profileAvatar.text = me.take(1).uppercase()
        profileName.text = me
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val status = prefs.getString("user_status", "No bio") ?: "No bio"
        profileBio.text = status
        editBio.setText(status)
    }

    private fun saveProfile() {
        val bio = editBio.text.toString().trim()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("user_status", bio).apply()
        profileBio.text = bio
        ws?.send(JSONObject().apply {
            put("type", "profile_updated")
            put("bio", bio)
            put("status_text", bio)
            put("avatar_url", "")
        }.toString())
        t("Profile updated!")
        loadUsers()
    }

    private fun showCreateMenu() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.menu_create, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<LinearLayout>(R.id.menuGroup).setOnClickListener {
            dialog.dismiss()
            showCreateGroupDialog()
        }
        
        dialogView.findViewById<LinearLayout>(R.id.menuFeed).setOnClickListener {
            dialog.dismiss()
            showCreateFeedDialog()
        }
        dialog.show()
    }

    private fun showCreateGroupDialog() {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val nameIn = EditText(this).apply {
            hint = "Название группы"
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        val membIn = EditText(this).apply {
            hint = "Участники (через запятую)"
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
        }
        v.addView(nameIn)
        v.addView(membIn)
        AlertDialog.Builder(this)
            .setTitle("Создать группу")
            .setView(v)
            .setPositiveButton("Создать") { _, _ ->
                val n = nameIn.text.toString().trim()
                val m = membIn.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (n.isNotEmpty() && m.isNotEmpty()) {
                    m.add(me)
                    ws?.send(JSONObject().apply {
                        put("type", "create_group")
                        put("name", n)
                        put("members", JSONArray(m))
                        put("private", false)
                    }.toString())
                    t("Группа создана!")
                    handler.postDelayed({ loadUsers() }, 1000)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCreateFeedDialog() {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val nameIn = EditText(this).apply {
            hint = "Название ленты"
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        val descIn = EditText(this).apply {
            hint = "Описание"
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
        }
        v.addView(nameIn)
        v.addView(descIn)
        AlertDialog.Builder(this)
            .setTitle("Создать ленту")
            .setView(v)
            .setPositiveButton("Создать") { _, _ ->
                val n = nameIn.text.toString().trim()
                if (n.isNotEmpty()) {
                    ws?.send(JSONObject().apply {
                        put("type", "create_feed")
                        put("name", n)
                        put("description", descIn.text.toString().trim())
                        put("private", false)
                    }.toString())
                    t("Лента создана!")
                    handler.postDelayed({ loadUsers() }, 1000)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun login() {
        val u = loginUser.text.toString().trim()
        val p = loginPass.text.toString().trim()
        server = serverUrl.text.toString().trim()
        if (u.isEmpty() || p.isEmpty()) return t("Fill all fields")
        thread {
            try {
                val j = JSONObject().apply {
                    put("username", u)
                    put("password", p)
                }
                val b = j.toString().toRequestBody("application/json".toMediaType())
                val r = client.newCall(Request.Builder().url("$server/login").post(b).build()).execute()
                if (r.isSuccessful) {
                    val d = JSONObject(r.body!!.string())
                    token = d.optString("access_token", "")
                    me = u
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                        .edit()
                        .putString("token", token)
                        .putString("username", me)
                        .putString("server_url", server)
                        .apply()
                    handler.post { showMain() }
                } else {
                    handler.post { t(JSONObject(r.body!!.string()).optString("detail", "Error")) }
                }
            } catch (e: Exception) {
                handler.post { t("Server unavailable") }
            }
        }
    }

    private fun register() {
        val u = loginUser.text.toString().trim()
        val p = loginPass.text.toString().trim()
        server = serverUrl.text.toString().trim()
        if (u.isEmpty() || p.isEmpty()) return t("Fill all fields")
        thread {
            try {
                val j = JSONObject().apply {
                    put("username", u)
                    put("password", p)
                }
                val b = j.toString().toRequestBody("application/json".toMediaType())
                val r = client.newCall(Request.Builder().url("$server/register").post(b).build()).execute()
                if (r.isSuccessful) {
                    handler.post {
                        t("Account created!")
                        login()
                    }
                } else {
                    handler.post { t(JSONObject(r.body!!.string()).optString("detail", "Error")) }
                }
            } catch (e: Exception) {
                handler.post { t("Server unavailable") }
            }
        }
    }

    private fun showMain() {
        loginLayout.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        connectWS()
        loadUsers()
        showTab(0)
    }

    private fun openChat(id: String) {
        selId = id
        val u = users.find { it.username == id }
        val name = u?.name ?: id
        chatTitle.text = name
        chatAvatar.text = name.take(1).uppercase()
        chatAvatar.background = circleBg(u?.avatarColor ?: "#2AABEE")
        chatStatus.text = if (u?.online == true) "online" else "offline"
        chatStatus.setTextColor(if (u?.online == true) 0xff34c759.toInt() else 0xff8e8e93.toInt())
        mainContainer.visibility = View.GONE
        bottomNav.visibility = View.GONE
        chatLayout.visibility = View.VISIBLE
        refreshMessages()
        startPolling()
    }

    private fun closeChat() {
        selId = ""
        stopPolling()
        chatLayout.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        loadUsers()
    }

    private fun connectWS() {
        try {
            val wsUrl = "ws://${server.replace("http://", "")}/ws/$me?token=$token"
            ws = client.newWebSocket(
                Request.Builder().url(wsUrl).build(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val j = JSONObject(text)
                            if (j.optString("type") == "ping") return
                            if (selId.isNotEmpty()) handler.post { refreshMessages() }
                            handler.post { loadUsers() }
                        } catch (_: Exception) {}
                    }
                }
            )
        } catch (e: Exception) {
            handler.post { t("Connection error: ${e.message}") }
        }
    }

    private fun loadUsers() {
        thread {
            try {
                val r = client.newCall(
                    Request.Builder().url("$server/users/$me?token=$token").build()
                ).execute()
                if (r.isSuccessful) {
                    val a = JSONArray(r.body!!.string())
                    Log.d("MainActivity", "Users response: $a")
                    users.clear()
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                    val savedStatus = prefs.getString("user_status", "No bio") ?: "No bio"
                    
                    val userList = mutableListOf<User>()
                    for (i in 0 until a.length()) {
                        val o = a.getJSONObject(i)
                        val username = o.optString("username")
                        if (username == "MyChat") continue
                        
                        val displayName = o.optString("name", "")
                        val finalName = if (displayName.isNotEmpty()) displayName else username
                        val bio = if (username == me) savedStatus else o.optString("bio", "")
                        val isGroup = o.optBoolean("is_group", false)
                        val isFeed = o.optBoolean("is_feed", false)
                        val online = o.optBoolean("online", false)
                        
                        Log.d("MainActivity", "User: $username, online: $online")
                        
                        if (!isGroup && !isFeed) {
                            userList.add(
                                User(
                                    username = username,
                                    avatarColor = o.optString("avatar_color", "#2AABEE"),
                                    online = online,
                                    lastSeen = o.optString("last_seen", ""),
                                    bio = bio,
                                    avatarUrl = o.optString("avatar_url", ""),
                                    isGroup = false,
                                    isFeed = false,
                                    name = finalName
                                )
                            )
                        }
                    }
                    
                    for (user in userList) {
                        try {
                            val msgR = client.newCall(
                                Request.Builder().url("$server/messages/${user.username}?me=$me&token=$token").build()
                            ).execute()
                            if (msgR.isSuccessful) {
                                val msgs = JSONArray(msgR.body!!.string())
                                if (msgs.length() > 0) {
                                    val last = msgs.getJSONObject(msgs.length() - 1)
                                    user.lastMsg = last.optString("text", "")
                                    user.lastTime = formatTime(last.optString("time", ""))
                                    
                                    if (last.has("file")) {
                                        val file = last.getJSONObject("file")
                                        val fileName = file.optString("name", "")
                                        if (fileName.contains(".jpg") || fileName.contains(".png") || fileName.contains(".jpeg")) {
                                            user.lastMsgType = "photo"
                                            user.lastMsg = "Фото"
                                        } else {
                                            user.lastMsgType = "file"
                                            user.lastMsg = "Файл: $fileName"
                                        }
                                    } else {
                                        user.lastMsgType = "text"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Игнорируем ошибки
                        }
                    }
                    
                    users.addAll(userList)
                    handler.post {
                        chatAdapter.update(users)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatTime(timeStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(timeStr)
            sdf.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }

    private fun searchUsers(q: String) {
        thread {
            try {
                val r = client.newCall(
                    Request.Builder().url("$server/users/$me?token=$token").build()
                ).execute()
                if (r.isSuccessful) {
                    val a = JSONArray(r.body!!.string())
                    val res = mutableListOf<User>()
                    for (i in 0 until a.length()) {
                        val o = a.getJSONObject(i)
                        val un = o.optString("username")
                        val nm = o.optString("name", "")
                        if ((un.contains(q, true) || nm.contains(q, true)) && un != "MyChat") {
                            val displayName = if (nm.isNotEmpty()) nm else un
                            res.add(
                                User(
                                    username = un,
                                    avatarColor = o.optString("avatar_color", "#2AABEE"),
                                    online = o.optBoolean("online", false),
                                    lastSeen = o.optString("last_seen", ""),
                                    bio = o.optString("bio", ""),
                                    avatarUrl = o.optString("avatar_url", ""),
                                    isGroup = o.optBoolean("is_group", false),
                                    isFeed = o.optBoolean("is_feed", false),
                                    name = displayName
                                )
                            )
                        }
                    }
                    handler.post { chatAdapter.update(res) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshMessages() {
        if (selId.isEmpty()) return
        thread {
            try {
                val r = client.newCall(
                    Request.Builder().url("$server/messages/$selId?me=$me&token=$token").build()
                ).execute()
                if (r.isSuccessful) {
                    val a = JSONArray(r.body!!.string())
                    val nm = mutableListOf<ChatMessage>()
                    for (i in 0 until a.length()) {
                        val o = a.getJSONObject(i)
                        var fi: FileInfo? = null
                        if (o.has("file")) {
                            val f = o.getJSONObject("file")
                            fi = FileInfo(f.optString("name"), f.optString("url"), f.optLong("size"))
                        }
                        nm.add(
                            ChatMessage(
                                o.optString("id"),
                                o.optString("from"),
                                o.optString("to"),
                                o.optString("text"),
                                o.optString("time"),
                                fi,
                                o.optBoolean("is_group")
                            )
                        )
                    }
                    handler.post {
                        msgAdapter.update(nm)
                        if (nm.isNotEmpty()) messagesList.scrollToPosition(nm.size - 1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage() {
        val t = msgInput.text.toString().trim()
        if (t.isEmpty() || selId.isEmpty()) return
        ws?.send(
            JSONObject().apply {
                put("type", "private")
                put("to", selId)
                put("text", t)
            }.toString()
        )
        msgInput.text.clear()
        handler.postDelayed({ refreshMessages() }, 500)
        handler.postDelayed({ loadUsers() }, 1000)
    }

    private fun pickFile() {
        startActivityForResult(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            100
        )
    }

    override fun onActivityResult(rc: Int, rc2: Int, data: Intent?) {
        super.onActivityResult(rc, rc2, data)
        if (rc == 100 && rc2 == RESULT_OK) data?.data?.let { uploadFile(it) }
    }

    private fun uploadFile(uri: Uri) {
        val pd = AlertDialog.Builder(this)
            .setTitle("Uploading...")
            .setView(ProgressBar(this).apply { setPadding(40, 30, 40, 30) })
            .create()
        pd.show()
        thread {
            try {
                val ins = contentResolver.openInputStream(uri)
                val bytes = ins?.readBytes()
                ins?.close()
                var fn = "file"
                contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) fn = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME)) ?: "file"
                }
                val rb = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fn, bytes!!.toRequestBody("application/octet-stream".toMediaType()))
                    .build()
                val r = client.newCall(
                    Request.Builder().url("$server/upload?token=$token").post(rb).build()
                ).execute()
                if (r.isSuccessful) {
                    val u = JSONObject(r.body!!.string()).optString("url", "")
                    ws?.send(
                        JSONObject().apply {
                            put("type", "private")
                            put("to", selId)
                            put("text", "File: $fn")
                            put("file", JSONObject().apply {
                                put("name", fn)
                                put("url", u)
                                put("size", bytes.size)
                            })
                        }.toString()
                    )
                }
                handler.post {
                    pd.dismiss()
                    refreshMessages()
                    loadUsers()
                }
            } catch (e: Exception) {
                handler.post {
                    pd.dismiss()
                    t("Upload error")
                }
            }
        }
    }

    private fun downloadFile(url: String, name: String) {
        thread {
            try {
                val bytes = client.newCall(Request.Builder().url("$server$url").build()).execute()
                    .body?.bytes() ?: return@thread
                val f = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    name
                )
                f.writeBytes(bytes)
                handler.post {
                    t("Saved: ${f.absolutePath}")
                    val uri = FileProvider.getUriForFile(
                        this,
                        "$packageName.fileprovider",
                        f
                    )
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    uri,
                                    when (name.substringAfterLast('.').lowercase()) {
                                        "jpg", "jpeg", "png" -> "image/*"
                                        "pdf" -> "application/pdf"
                                        else -> "*/*"
                                    }
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            "Open"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPolling() {
        stopPolling()
        pollRunnable = object : Runnable {
            override fun run() {
                if (selId.isNotEmpty()) {
                    refreshMessages()
                    handler.postDelayed(this, 2000)
                }
                handler.postDelayed({ loadUsers() }, 5000)
            }
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
    }

    private fun t(msg: String) {
        handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun thread(r: () -> Unit) = Thread(r).start()

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
