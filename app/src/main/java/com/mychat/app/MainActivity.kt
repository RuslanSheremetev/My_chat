package com.mychat.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Vibrator
import android.os.VibrationEffect
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.mychat.app.activities.FavoritesActivity
import com.mychat.app.activities.ProfileActivity
import com.mychat.app.adapters.ChatAdapter
import android.view.animation.TranslateAnimation
import android.view.animation.Animation
import android.widget.FrameLayout

import com.mychat.app.adapters.MessageAdapter
import com.mychat.app.adapters.StickerAdapter
import com.mychat.app.adapters.circleBg
import com.mychat.app.models.ChatMessage
import com.mychat.app.models.FileInfo
import com.mychat.app.models.User
import com.mychat.app.utils.FileCache
import com.mychat.app.data.AppDatabase
import com.mychat.app.data.ChatSettings
import com.mychat.app.data.MessageEntity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    companion object {
        var mainWs: okhttp3.WebSocket? = null
        fun sendCallSignal(msg: String) {
            mainWs?.send(msg)
        }
    }
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
    private lateinit var db: AppDatabase
    private var ws: WebSocket? = null
    private val users = mutableListOf<User>()
    private var currentUserId: String = ""
    private var currentUserPhone: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var lastMessageCount = 0
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var logText: android.widget.TextView
    private lateinit var logScroll: android.widget.ScrollView
    private lateinit var contextMenuBar: FrameLayout
    private var selectedUserForDelete: User? = null

    private lateinit var msgAdapter: MessageAdapter
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Контекстное меню для чатов
        logText = findViewById(R.id.logText)
        logScroll = findViewById(R.id.logScroll)
        logScroll.visibility = android.view.View.GONE  // Скрыто на главном экране
        log("Log started")
        chatHeader = findViewById(R.id.chatHeader)
        selectPanel = findViewById(R.id.selectPanel)
        selectPanel.findViewById<LinearLayout>(R.id.btnDeleteSelected).setOnClickListener {
            deleteSelectedMessages()
        }
        selectPanel.findViewById<LinearLayout>(R.id.btnForwardSelected).setOnClickListener {
            forwardSelectedMessages()
        }
        contextMenuBar = findViewById(R.id.contextMenuBar)
        contextMenuBar.visibility = View.GONE
        contextMenuBar.findViewById<ImageView>(R.id.btn_close).setOnClickListener { hideContextMenu() }
        contextMenuBar.findViewById<ImageView>(R.id.btn_delete).setOnClickListener {
            selectedUserForDelete?.let { deleteChat(it) }
            hideContextMenu()
        }

        FileCache.init(this)
        db = AppDatabase.getInstance(this)
        
        loginLayout = findViewById(R.id.loginLayout)
        mainContainer = findViewById(R.id.mainContainer)
        bottomNav = findViewById(R.id.bottomNav)
        chatsScreen = findViewById(R.id.chatsScreen)
        profileScreen = findViewById(R.id.profileScreen)
        chatLayout = findViewById(R.id.chatLayout)
        
        // Свайп для выхода из чата
        var swipeStartX = 0f
        chatLayout.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    false
                }
                android.view.MotionEvent.ACTION_CANCEL, android.view.MotionEvent.ACTION_UP -> {
                    if (event.x - swipeStartX > 200 && swipeStartX < 80) {
                        closeChat()
                        true
                    } else false
                }
                else -> false
            }
        }
        
        // Свайп справа-налево для выхода из чата
        var startX = 0f
        chatLayout.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    false
                }
                android.view.MotionEvent.ACTION_CANCEL, android.view.MotionEvent.ACTION_UP -> {
                    if (event.x - startX > 150 && startX < 100) {
                        closeChat()
                        true
                    } else false
                }
                else -> false
            }
        }
        loginUser = findViewById(R.id.loginUser)
        loginPass = findViewById(R.id.loginPass)
        serverUrl = findViewById(R.id.serverUrl)
        searchInput = findViewById(R.id.searchInput)
        
        val btnSearchOpen = findViewById<android.widget.ImageButton>(R.id.btnSearchOpen)
        val searchOverlayPanel = findViewById<android.widget.LinearLayout>(R.id.searchOverlayPanel)
        val searchOverlayInput = searchOverlayPanel.findViewById<android.widget.EditText>(R.id.searchOverlayInput)
        
        btnSearchOpen.setOnClickListener {
            searchOverlayPanel.visibility = android.view.View.VISIBLE
            searchOverlayPanel.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.search_slide_down))
            searchOverlayInput.requestFocus()
        }
        
        searchOverlayPanel.findViewById<android.widget.ImageButton>(R.id.btnSearchBack).setOnClickListener {
            val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.search_slide_up)
            anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationEnd(a: android.view.animation.Animation?) { searchOverlayPanel.visibility = android.view.View.GONE }
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            })
            searchOverlayPanel.startAnimation(anim)
        }
        
        searchOverlayInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { searchUsers(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        chatList = findViewById(R.id.chatList)
        messagesList = findViewById(R.id.messagesList)
        replyPreview = findViewById(R.id.replyPreview)
        previewAuthor = replyPreview.findViewById(R.id.previewAuthor)
        previewText = replyPreview.findViewById(R.id.previewText)
        replyPreview.findViewById<android.widget.ImageButton>(R.id.btnCloseReply).setOnClickListener { cancelReply() }
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
        chatAdapter = ChatAdapter(
            onClick = { user -> openChat(user.username) },
            onLongClick = { user -> showChatActions(user) }
        )
        chatList.layoutManager = LinearLayoutManager(this)
        chatList.adapter = chatAdapter
        
        // Создаём адаптер позже
        val lm = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesList.layoutManager = lm
        messagesList.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        // Эффект растяжения при прокрутке
        messagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Лёгкое затемнение краёв при скролле
                val topChild = recyclerView.getChildAt(0)
                val bottomChild = recyclerView.getChildAt(recyclerView.childCount - 1)
                topChild?.alpha = if (recyclerView.canScrollVertically(-1)) 0.7f else 1.0f
                bottomChild?.alpha = if (recyclerView.canScrollVertically(1)) 0.7f else 1.0f
            }
        })
        messagesList.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            removeDuration = 250
            addDuration = 300
        }
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener { login() }
        findViewById<Button>(R.id.btnRegister).setOnClickListener { register() }
        var voiceRecorder: android.media.MediaRecorder? = null
        var voiceFile: java.io.File? = null
        
        findViewById<ImageButton>(R.id.btnMic).setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
                        t("Разрешите доступ к микрофону")
                        true  // Не вылетаем
                    }
                    try {
                        voiceFile = java.io.File.createTempFile("voice_", ".m4a", cacheDir)
                        voiceRecorder = android.media.MediaRecorder().apply {
                            setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                            setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                            setOutputFile(voiceFile!!.absolutePath)
                            prepare()
                            start()
                        }
                        log("VOICE: recording started"); t("🎤 Запись...")
                    } catch (e: Exception) {
                        log("VOICE: error - mic failed"); t("Ошибка микрофона")
                    }
                    true
                }
                android.view.MotionEvent.ACTION_CANCEL, android.view.MotionEvent.ACTION_UP -> {
                    voiceRecorder?.apply { stop(); release() }; val voiceDuration = ((voiceFile?.length() ?: 0) / 800).toInt().coerceAtLeast(1); log("VOICE: recording stopped, size=${voiceFile?.length() ?: 0}, dur=${voiceDuration}s")
                    voiceRecorder = null
                    voiceFile?.let { file ->
                        if (file.length() > 0) sendVoiceFile(file)
                    }
                    true
                }
                else -> false
            }
        }
        msgInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.isNotEmpty() == true
                // Переключение кнопок
                val mic = findViewById<ImageButton>(R.id.btnMic)
                val send = findViewById<ImageButton>(R.id.btnSend)
                if (hasText) {
                    if (mic.visibility == View.VISIBLE) {
                        mic.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@MainActivity, R.anim.icon_fade_out))
                        mic.visibility = View.GONE
                        send.visibility = View.VISIBLE
                        send.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@MainActivity, R.anim.icon_fade_in))
                    }
                } else {
                    if (send.visibility == View.VISIBLE) {
                        send.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@MainActivity, R.anim.icon_fade_out))
                        send.visibility = View.GONE
                        mic.visibility = View.VISIBLE
                        mic.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@MainActivity, R.anim.icon_fade_in))
                    }
                }
                // Typing отправляется раз в 3 секунды, не на каждый символ
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        findViewById<ImageButton>(R.id.btnStickers).setOnClickListener { showStickers() }

        findViewById<ImageButton>(R.id.btnClearInput).setOnClickListener {
            msgInput.text.clear()
        }
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener { v ->
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_scale))
            v.postDelayed({ v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_release)) }, 150)
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.send_button_in))
            sendMessage()
        }
        findViewById<ImageButton>(R.id.btnAttach).setOnClickListener { showAttachmentMenu() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { closeChat() }
        
findViewById<ImageButton>(R.id.btnCall)?.setOnClickListener { v ->
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_scale))
            v.postDelayed({ v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_release)) }, 150)
            val intent = android.content.Intent(this, com.mychat.app.activities.CallActivity::class.java).apply {
                putExtra("name", chatTitle.text.toString())
                putExtra("avatar", chatTitle.text.toString().take(1))
                putExtra("incoming", false)
            }
            startActivity(intent)
        }
        findViewById<ImageButton>(R.id.btnChatMenu)?.setOnClickListener { anchor ->
            val view = layoutInflater.inflate(R.layout.dropdown_chat_menu, null)
            val popup = android.widget.PopupWindow(view, 
                (220 * resources.displayMetrics.density).toInt(),
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, true)
            popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            popup.elevation = 20f
            
            view.findViewById<LinearLayout>(R.id.menuInfo).setOnClickListener { popup.dismiss(); showUserInfo() }
            view.findViewById<LinearLayout>(R.id.menuMute).setOnClickListener { popup.dismiss(); toggleMute() }
            view.findViewById<LinearLayout>(R.id.menuSearch).setOnClickListener { popup.dismiss(); showSearchOverlay() }
            view.findViewById<LinearLayout>(R.id.menuClear).setOnClickListener {
                popup.dismiss()
                AlertDialog.Builder(this).setTitle("Очистить историю")
                    .setMessage("Удалить все сообщения?")
                    .setPositiveButton("Очистить") { _, _ -> clearHistory() }
                    .setNegativeButton("Отмена", null).show()
            }
            view.findViewById<LinearLayout>(R.id.menuBlock).setOnClickListener { popup.dismiss(); blockUser() }
            
            popup.showAsDropDown(anchor, -180.dpToPx(), 8.dpToPx())
        }
        findViewById<ImageButton>(R.id.btnCreate).setOnClickListener { v ->
            v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_scale))
            v.postDelayed({ v.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_click_release)) }, 150)
            t("Создание групп и лент будет позже")
        }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        
        navChats.setOnClickListener { showTab(0) }
        navFavorites.setOnClickListener { openFavorites() }
        navProfile.setOnClickListener { t("Профиль будет позже") }
        
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
        currentUserId = prefs.getString("username", "") ?: ""
        currentUserPhone = prefs.getString("phone", "") ?: ""
        me = prefs.getString("username", "") ?: ""
        me = prefs.getString("username", "") ?: ""
        server = prefs.getString("server_url", server) ?: server
        
        Log.d("MainActivity", "me = '$me'")
        
        if (token.isNotEmpty() && me.isNotEmpty()) {
            showMain()
        }
    }

    override fun onResume() {
        super.onResume()
        // Принудительно восстанавливаем экран чатов
        chatsScreen.visibility = View.VISIBLE
        profileScreen.visibility = View.GONE
        chatLayout.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        highlightTab(0)
    }

    private fun openFavorites() {
        val intent = Intent(this, FavoritesActivity::class.java).apply {
            putExtra("token", token)
            putExtra("username", me)
        }
        startActivity(intent)
        // Не вызываем finish(), чтобы onResume сработал при возврате
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
        if (tab == 0) {
            chatLayout.visibility = View.GONE
            mainContainer.visibility = View.VISIBLE
            bottomNav.visibility = View.VISIBLE
        }
        if (tab == 2) loadProfile()
        highlightTab(tab)
    }
    
    private fun highlightTab(tab: Int) {
        val activeColor = resources.getColor(R.color.primary, theme)
        val inactiveColor = resources.getColor(R.color.nav_inactive, theme)
        
        // Подсветка Чатов
        val chatIcon = navChats.getChildAt(0) as? ImageView
        val chatLabel = navChats.getChildAt(1) as? TextView
        chatIcon?.setColorFilter(if (tab == 0) activeColor else inactiveColor)
        chatLabel?.setTextColor(if (tab == 0) activeColor else inactiveColor)
        
        // Подсветка Избранного
        val favIcon = navFavorites.getChildAt(0) as? ImageView
        val favLabel = navFavorites.getChildAt(1) as? TextView
        favIcon?.setColorFilter(if (tab == 1) activeColor else inactiveColor)
        favLabel?.setTextColor(if (tab == 1) activeColor else inactiveColor)
        
        // Подсветка Профиля
        val profIcon = navProfile.getChildAt(0) as? ImageView
        val profLabel = navProfile.getChildAt(1) as? TextView
        profIcon?.setColorFilter(if (tab == 2) activeColor else inactiveColor)
        profLabel?.setTextColor(if (tab == 2) activeColor else inactiveColor)
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
        log("WS send: typing"); ws?.send(JSONObject().apply {
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
                    log("WS send: reaction"); ws?.send(JSONObject().apply {
                        put("type", "create_group")
                        put("name", n)
                        put("members", JSONArray(m))
                        put("private", false)
                    }.toString())
                    t("Группа создана!")
                    // loadUsers removed - too many calls
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
                    log("WS send: sticker"); ws?.send(JSONObject().apply {
                        put("type", "create_feed")
                        put("name", n)
                        put("description", descIn.text.toString().trim())
                        put("private", false)
                    }.toString())
                    t("Лента создана!")
                    // loadUsers removed - too many calls
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
                    currentUserId = d.optString("username", d.optString("user_id", ""))
                    currentUserPhone = u
                    me = u
                    Log.d("MainActivity", "Login - me = '$me'")
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                        .edit()
                        .putString("token", token)
                        .putString("user_id", currentUserId)
                        .putString("phone", currentUserPhone)
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
        
        // СОЗДАЁМ АДАПТЕР ЗДЕСЬ, КОГДА me УЖЕ ЕСТЬ
        msgAdapter = MessageAdapter(
            me = me,
            onDownload = { url, name -> downloadFile(url, name) },
            onMessageLongClick = { msg -> showMessageActions(msg) },
            onSaveReaction = { msgId, json ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    db.messageDao().updateReactions(msgId, json)
                }
            },
            appContext = applicationContext,
            onLog = { msg -> log(msg) }
        )
        messagesList.adapter = msgAdapter
        
        connectWS()
        // loadUsers removed
        showTab(0)
    }

    private fun openChat(id: String) {
        if (selId == id && chatLayout.visibility == View.VISIBLE) return
        if (selId == id && chatLayout.visibility == View.VISIBLE) return  // Уже открыт
        if (selId == id && chatLayout.visibility == View.VISIBLE) return  // Уже открыт
        log("FORWARD: openChat id=$id, mode=$isForwardMode, msgs=${pendingForwardMessages?.size}")
        if (isForwardMode && pendingForwardMessages != null) {
            val messages = pendingForwardMessages!!
            for (msg in messages) {
                val forwardText = "↪ ${msg.from}: ${msg.text}"
                sendMessageTo(id, forwardText)
            }
            pendingForwardMessages = null
            isForwardMode = false
            log("FORWARD: sent ${messages.size} messages to $id")
            t("Переслано: ${messages.size} сообщений")
            pendingForwardMessages = null
            isForwardMode = false
            // Продолжаем открытие чата
        }
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
        
        lastMessageCount = 0
        refreshMessages()
        msgInput.requestFocus()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        isMuted = prefs.getBoolean("mute_$id", false)
        // Восстанавливаем блокировку из Room
        thread {
            val settings = db.messageDao().getChatSettings(id)
            isBlocked = settings?.isBlocked ?: false
        }
        pendingForward?.let { msg -> handler.postDelayed({ forwardMessage(msg); pendingForward = null }, 500) }
        // Отмечаем сообщения прочитанными

        startPolling()
    }

    private fun closeChat() {
        // Скрываем клавиатуру
        val immClose = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { immClose.hideSoftInputFromWindow(it.windowToken, 0) }
        selId = ""
        stopPolling()
        chatLayout.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        // Скрываем клавиатуру
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        loadUsers()
    }
    
    private var selectedMessage: ChatMessage? = null
    private var selectedMessages = mutableListOf<ChatMessage>()
    private var isSelectMode = false
    private val logBuffer = mutableListOf<org.json.JSONObject>()
    private lateinit var chatHeader: android.view.View
    private lateinit var selectPanel: android.view.View
    private var pendingForward: ChatMessage? = null
    private var replyToMsg: ChatMessage? = null
    private lateinit var replyPreview: android.view.View
    private lateinit var previewAuthor: android.widget.TextView
    private lateinit var previewText: android.widget.TextView
    private var pendingForwardMessages: List<ChatMessage>? = null
    private var isForwardMode = false
    
    private fun showMessageActions(msg: ChatMessage) {
        selectedMessage = msg
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_message_actions, null)
        bottomSheet.setContentView(view)
        
        val reactions = mapOf(
            R.id.reactionLove to "❤️",
            R.id.reactionLike to "👍",
            R.id.reactionLaugh to "😂",
            R.id.reactionWow to "😮",
            R.id.reactionSad to "😢",
            R.id.reactionAngry to "😡"
        )
        reactions.forEach { (id, emoji) ->
            view.findViewById<TextView>(id)?.setOnClickListener {
                // Отправляем реакцию через HTTP API
                val jsonBody = JSONObject().apply {
                    put("message_id", msg.id)
                    put("user_id", currentUserId)
                    put("emoji", emoji)
                }
                val bodyStr = jsonBody.toString()
                log("Request body: $bodyStr")
                val body = bodyStr.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$server/api/messages/reaction?token=$token")
                    .post(body)
                    .build()
                log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        android.util.Log.e("REACTION", "Network error: ${e.message}")
                        log("Network error: ${e.message}")
                        runOnUiThread { t("Ошибка реакции") }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            android.util.Log.d("REACTION", "Server response: isSuccessful=${response.isSuccessful}, code=${response.code}")
                            log("Server response: OK, code=${response.code}")
                            if (response.isSuccessful) {
                                android.util.Log.d("REACTION", "Calling addReaction: msgId=${msg.id}, emoji=$emoji, phone=$currentUserPhone")
                                log("addReaction: msgId=${msg.id}, emoji=$emoji, phone=$currentUserPhone")
                                log("Reaction: $emoji on ${msg.id}")
                                msgAdapter.addReaction(msg.id, emoji, currentUserPhone)
                                // Сохраняем реакции в Room
                                val reactionsMap = mapOf(emoji to listOf(currentUserPhone))
                                val reactionsJson = JSONObject(reactionsMap as Map<*, *>).toString()
                                Thread {
                                    db.messageDao().updateReactions(msg.id, reactionsJson)
                                    log("Saved to Room: ${msg.id} -> $reactionsJson")
                                }.start()
                                t("$emoji")
                            }
                        }
                    }
                })
                bottomSheet.dismiss()
            }
        }
        
        view.findViewById<LinearLayout>(R.id.actionReply)?.setOnClickListener {
            bottomSheet.dismiss()
            replyToMessage(msg)
        }
        view.findViewById<LinearLayout>(R.id.actionEdit)?.setOnClickListener {
            bottomSheet.dismiss()
            editMessage(msg)
        }
        view.findViewById<LinearLayout>(R.id.actionSelect)?.setOnClickListener {
            bottomSheet.dismiss()
            log("UI: select mode ON"); log("UI: select mode ON - ${msg.id}")
            isSelectMode = true
            selectedMessages.clear()
            selectedMessages.add(msg)
            msgAdapter.selectMode = true
            msgAdapter.selectedIds.clear()
            msgAdapter.notifyDataSetChanged()
            chatHeader.visibility = android.view.View.GONE
            selectPanel.visibility = android.view.View.VISIBLE
            log("Select mode ON")
            t("Режим выбора. Нажмите на сообщения")
            t("Сообщение выбрано. Нажмите ещё для выбора нескольких")
        }
        view.findViewById<LinearLayout>(R.id.actionCopy)?.setOnClickListener {
            bottomSheet.dismiss()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", msg.text))
            t("Текст скопирован!")
        }
        view.findViewById<LinearLayout>(R.id.actionForward)?.setOnClickListener {
            bottomSheet.dismiss()
            showForwardDialog(msg)
        }
        view.findViewById<LinearLayout>(R.id.actionFavorite)?.setOnClickListener {
            bottomSheet.dismiss()
            addToFavorites(msg)
        }
        view.findViewById<LinearLayout>(R.id.actionDelete)?.setOnClickListener {
            bottomSheet.dismiss()
            deleteMessage(msg)
        }
        
        log("UI: bottomSheet show"); bottomSheet.show()
    }

    private fun connectWS() {
        try {
            log("WS connecting..."); val wsUrl = "ws://${server.replace("http://", "")}/ws/$me?token=$token"
            mainWs = client.newWebSocket(
                Request.Builder().url(wsUrl).build(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        com.mychat.app.activities.CallActivity.onSignalingMessage?.invoke(text)
                    }
                }
            )
            ws = client.newWebSocket(
                Request.Builder().url(wsUrl).build(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {  log("WS received: ${text.take(50)}...")
                        try {
                            val j = JSONObject(text)
                            if (j.optString("type") == "call_offer") {
                        val from = j.optString("from", j.optString("username", ""))
                        val intent = android.content.Intent(this@MainActivity, com.mychat.app.activities.CallActivity::class.java).apply {
                            putExtra("name", from)
                            putExtra("caller", false)
                        }
                        startActivity(intent)
                        return
                    }
                    val jtype = j.optString("type")
                    if (jtype == "call_offer") {
                        val from = j.optString("from", "")
                        val intent = android.content.Intent(this@MainActivity, com.mychat.app.activities.CallActivity::class.java).apply {
                            putExtra("name", from)
                            putExtra("caller", false)
                        }
                        startActivity(intent)
                        return
                    }
                    if (jtype in listOf("call_answer", "ice_candidate", "call_end")) {
                        com.mychat.app.activities.CallActivity.onSignalingMessage?.invoke(text)
                        return
                    }
                    if (jtype == "ping") { webSocket.send("{\"type\":\"pong\"}"); return }
                            if (isBlocked) return
                            if (selId.isNotEmpty()) {
                                handler.post {
                                    updateMessagesSilent()
                                }
                            }
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
        log("HTTP: loadUsers")
        thread {
            try {
                val r = client.newCall(
                    Request.Builder().url("$server/users/$me?token=$token").build()
                ).execute()
                if (r.isSuccessful) {
                    val a = JSONArray(r.body!!.string())
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
                                            user.lastMsg = ""
                                        } else {
                                            user.lastMsgType = if (last.optString("type") == "call") "call" else "file"
                                            user.lastMsg = when {
                                                last.optString("type") == "call" -> if (last.optBoolean("missed", false)) "🔴 Пропущенный звонок" else "📞 Звонок ${last.optString("duration", "")}"
                                                last.optString("file_type") == "voice" -> "🎤 ${last.optString("file_name", "Голосовое")}"
                                                else -> "Файл: $fileName"
                                            }
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
                    // Загружаем isMuted из Room
                            for (u in res) {
                                val s = db.messageDao().getChatSettings(u.username)
                                if (s != null) { u.isMuted = s.isMuted; log("Mute loaded: ${u.username} = ${u.isMuted}") }
                            }
                            // Загружаем isMuted для каждого пользователя
                            for (u in res) {
                                val s = db.messageDao().getChatSettings(u.username)
                                if (s != null) { u.isMuted = s.isMuted }
                            }
                            for (u in res) {
                            val s = db.messageDao().getChatSettings(u.username)
                            if (s != null) u.isMuted = s.isMuted
                        }
                        handler.post { chatAdapter.update(res) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshMessages() {
        // Сначала показываем из Room (мгновенно)
        thread {
            try {
                val localMessages = db.messageDao().getMessages(selId)
                if (localMessages.isNotEmpty()) {
                    val msgs = localMessages.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            from = entity.fromUser,
                            to = entity.toUser,
                            text = entity.text,
                            time = entity.time,
                            file = if (entity.fileUrl.isNotEmpty()) FileInfo(entity.fileName, entity.fileUrl) else null,
                            reactions = parseReactions(entity.reactionsJson)
                        )
                    }
                    // Обновляем статусы на sent
                    thread { db.messageDao().markSent(selId) }
                    handler.post { msgAdapter.update(msgs); loadReactions(msgs) }
                }
            } catch (e: Exception) {}
        }
        // Потом обновляем с сервера
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
                    lastMessageCount = nm.size
                    handler.post {
                        // Сохраняем в Room
                    thread {
                        try {
                            val entities = nm.map { msg ->
                                MessageEntity(
                                    id = msg.id,
                                    chatKey = selId,
                                    fromUser = msg.from,
                                    toUser = msg.to,
                                    text = msg.text,
                                    time = msg.time,
                                    fileUrl = msg.file?.url ?: "",
                                    fileName = msg.file?.name ?: "",
                                    reactionsJson = org.json.JSONObject(msg.reactions as Map<*, *>).toString()
                                )
                            }
                            // db.messageDao().insertMessages(entities)  // Отключено - вызывает прыжки
                            // db.messageDao().deleteOldMessages(selId)  // Отключено - вызывает прыжки
                        } catch (e: Exception) {}
                    }
                    // Удаляем временные сообщения (отправленные локально)
                    thread {
                        val local = db.messageDao().getMessages(selId)
                        val tempIds = local.filter { it.id.startsWith("sending_") }.map { it.id }
                        if (tempIds.isNotEmpty()) {
                            db.messageDao().deleteTempMessages(tempIds)
                        }
                    }
                    // Фильтруем удалённые сообщения
                    val filtered = nm.filter { it.text != "Сообщение удалено" }
                    msgAdapter.update(filtered)
                        if (nm.isNotEmpty()) {
                            messagesList.scrollToPosition(msgAdapter.itemCount - 1)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMessagesSilent() {
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
                    
                    if (nm.size > lastMessageCount) {
                        val wasAtBottom = !messagesList.canScrollVertically(1)
                        handler.post {
                            // Сохраняем в Room
                    thread {
                        try {
                            val entities = nm.map { msg ->
                                MessageEntity(
                                    id = msg.id,
                                    chatKey = selId,
                                    fromUser = msg.from,
                                    toUser = msg.to,
                                    text = msg.text,
                                    time = msg.time,
                                    fileUrl = msg.file?.url ?: "",
                                    fileName = msg.file?.name ?: "",
                                    reactionsJson = org.json.JSONObject(msg.reactions as Map<*, *>).toString()
                                )
                            }
                            // db.messageDao().insertMessages(entities)  // Отключено - вызывает прыжки
                            // db.messageDao().deleteOldMessages(selId)  // Отключено - вызывает прыжки
                        } catch (e: Exception) {}
                    }
                    // Удаляем временные сообщения (отправленные локально)
                    thread {
                        val local = db.messageDao().getMessages(selId)
                        val tempIds = local.filter { it.id.startsWith("sending_") }.map { it.id }
                        if (tempIds.isNotEmpty()) {
                            db.messageDao().deleteTempMessages(tempIds)
                        }
                    }
                    // Фильтруем удалённые сообщения
                    val filtered = nm.filter { it.text != "Сообщение удалено" }
                    msgAdapter.update(filtered)
                            lastMessageCount = nm.size
                            if (wasAtBottom && nm.isNotEmpty()) {
                                messagesList.scrollToPosition(msgAdapter.itemCount - 1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun sendMessageTo(to: String, text: String) {
        val json = JSONObject().apply {
            put("type", "private")
            put("to", to)
            put("text", text)
        }
        log("WS send: message"); ws?.send(json.toString())
        // Добавляем в локальный список
        val msg = ChatMessage(
            id = "sending_${System.currentTimeMillis()}",
            from = me,
            to = to,
            text = text,
            time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        )
        msgAdapter.addMessage(msg)
    }

    private fun sendMessage() {
        val t = msgInput.text.toString().trim()
        if (t.isEmpty() || selId.isEmpty()) return
        log("WS send: file"); ws?.send(
            JSONObject().apply {
                put("type", "private")
                put("to", selId)
                put("text", t)
                replyToMsg?.let { put("reply_to_msg_id", it.id) }
            }.toString()
        )
        msgInput.text.clear()
        cancelReply()
        handler.postDelayed({ refreshMessages() }, 200)
        // loadUsers removed - too many calls
    }

    private fun showAttachmentMenu() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_attachment, null)
        bottomSheet.setContentView(view)
        
        view.findViewById<LinearLayout>(R.id.attachCamera).setOnClickListener {
            bottomSheet.dismiss()
            // Сразу камера для фото
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, 200)
            } else {
                t("Камера не доступна")
            }
        }
        view.findViewById<LinearLayout>(R.id.attachGallery).setOnClickListener {
            bottomSheet.dismiss()
            pickPhoto()
        }
        view.findViewById<LinearLayout>(R.id.attachFile).setOnClickListener {
            bottomSheet.dismiss()
            pickFile()
        }
        
        // Закрытие по свайпу вниз
        view.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                view.tag = event.y
            }
            if (event.action == android.view.MotionEvent.ACTION_MOVE) {
                val startY = view.tag as? Float ?: 0f
                if (event.y - startY > 100) {
                    bottomSheet.dismiss()
                }
            }
            false
        }
        
        log("UI: bottomSheet show"); bottomSheet.show()
    }
    
    private fun pickPhoto() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            },
            101
        )
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
        if (rc == 200 && rc2 == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? android.graphics.Bitmap
            if (bitmap != null) { uploadBitmap(bitmap) }
            return
        }
        if (rc == 101 && rc2 == RESULT_OK) data?.data?.let { showPhotoDialog(it) }
        if (rc == 100 && rc2 == RESULT_OK) data?.data?.let { uploadFile(it) }
    }


    private fun uploadBitmap(bitmap: android.graphics.Bitmap) {
        thread {
            try {
                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                val bytes = baos.toByteArray()
                val fn = "photo_${System.currentTimeMillis()}.jpg"
                val rb = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fn, bytes.toRequestBody("image/jpeg".toMediaType()))
                    .build()
                val r = client.newCall(
                    Request.Builder().url("$server/upload?token=$token").post(rb).build()
                ).execute()
                if (r.isSuccessful) {
                    val u = JSONObject(r.body!!.string()).optString("url", "")
                    log("WS send: forward"); ws?.send(
                        JSONObject().apply {
                            put("type", "private")
                            put("to", selId)
                            put("text", "📷 Фото")
                            put("file", JSONObject().apply {
                                put("name", fn)
                                put("url", u)
                                put("size", bytes.size)
                            })
                        }.toString()
                    )
                }
            } catch (e: Exception) {
                handler.post { t("Ошибка") }
            }
        }
    }
    
    private fun showPhotoDialog(uri: Uri) {
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).create()
        dialog.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xff000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val closeBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(0xffffffff.toInt())
            background = null
            layoutParams = LinearLayout.LayoutParams(48, 48).apply { setMargins(8, 24, 0, 0) }
        }
        val imageView = ImageView(this).apply {
            setImageURI(uri)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 16)
            setBackgroundColor(0xff1c1c1e.toInt())
        }
        val captionInput = EditText(this).apply {
            hint = "Добавить подпись..."
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sendBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_send)
            background = getDrawable(R.drawable.bg_send_btn)
            setColorFilter(0xffffffff.toInt())
            layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginStart = 8 }
            scaleType = ImageView.ScaleType.CENTER
        }
        closeBtn.setOnClickListener { dialog.dismiss() }
        sendBtn.setOnClickListener {
            val caption = captionInput.text.toString().trim()
            uploadFile(uri, caption)
            dialog.dismiss()
        }
        bottomBar.addView(captionInput)
        bottomBar.addView(sendBtn)
        container.addView(closeBtn)
        container.addView(imageView)
        container.addView(bottomBar)
        dialog.setView(container)
        dialog.show()
    }
    
    private fun uploadFile(uri: Uri, caption: String = "") {
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
                    val isImage = fn.endsWith(".jpg") || fn.endsWith(".jpeg") || fn.endsWith(".png") || fn.endsWith(".gif") || fn.endsWith(".webp")
                    log("WS send: block"); ws?.send(
                        JSONObject().apply {
                            put("type", "private")
                            put("to", selId)
                            val text = if (caption.isNotEmpty()) caption else if (isImage) "📷 Фото" else "File: $fn"
                            put("text", text)
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
        log("HTTP: download $name")
        val fullUrl = if (url.startsWith("http")) url else "$server$url"
        // Проверяем кеш
        val cached = FileCache.getCachedFile(fullUrl)
        if (cached != null) {
            handler.post {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(androidx.core.content.FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", cached), "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            return
        }
        thread {
            try {
                val bytes = client.newCall(Request.Builder().url(fullUrl).build()).execute()
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
                            if (isBlocked) return
                if (selId.isNotEmpty()) {
                    updateMessagesSilent()
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
    }

    private fun showChatActions(user: User) {
        selectedUserForDelete = user
        
        // Подсвечиваем выбранный элемент
        val index = users.indexOf(user)
        if (index >= 0) {
            chatAdapter.selectedPosition = index
            chatAdapter.notifyDataSetChanged()
        }
        
        // Показываем меню с анимацией
        contextMenuBar.visibility = View.VISIBLE
        contextMenuBar.clearAnimation()
        val slideDown = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, -1f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        slideDown.duration = 300
        slideDown.interpolator = android.view.animation.DecelerateInterpolator()
        contextMenuBar.startAnimation(slideDown)
    }
    

    private fun log(msg: String) {
        // Добавляем в буфер для отправки на сервер
        val entry = org.json.JSONObject().apply {
            put("timestamp", android.text.format.DateFormat.format("yyyy-MM-dd'T'HH:mm:ss", java.util.Date()))
            put("message", msg)
            put("level", "INFO")
        }
        logBuffer.add(entry)
        // Отправляем если накопилось 10
        if (logBuffer.size >= 10) {
            flushLogs()
        }
        runOnUiThread {
            val timestamp = android.text.format.DateFormat.format("HH:mm:ss", java.util.Date())
            logText.append("$timestamp $msg\n")
            logScroll.post { logScroll.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }



    private fun parseReactions(json: String): MutableMap<String, MutableList<String>> {
        log("parseReactions: json=${json.take(100)}")
        if (json.isEmpty() || json == "{}") return mutableMapOf()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, MutableList<String>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                result[key] = list
            }
            result
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun loadReactions(msgs: List<ChatMessage>) {
        if (msgs.isEmpty()) return
        log("loadReactions: ${msgs.size} messages (batch)")
        
        val ids = org.json.JSONArray()
        msgs.forEach { ids.put(it.id) }
        val body = org.json.JSONObject().apply { put("msg_ids", ids) }
            .toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$server/api/messages/reactions/batch?token=$token")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: return
                    val json = JSONObject(respBody)
                    val allReactions = json.optJSONObject("reactions") ?: return
                    
                    runOnUiThread {
                        allReactions.keys().forEach { msgId ->
                            val r = allReactions.optJSONObject(msgId)
                            if (r != null && r.length() > 0) {
                                val reactions = mutableMapOf<String, MutableList<String>>()
                                r.keys().forEach { key ->
                                    val arr = r.getJSONArray(key)
                                    val list = mutableListOf<String>()
                                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                                    reactions[key] = list
                                }
                                msgAdapter.setReactions(msgId, reactions)
                                val jsonStr = org.json.JSONObject(reactions as Map<*, *>).toString()
                                thread { db.messageDao().updateReactions(msgId, jsonStr) }
                            }
                        }
                    }
                }
            }
        })
    }
    
    // Старый код удалён
    private fun loadReactions_OLD(msgs: List<ChatMessage>) {
        log("loadReactions: ${msgs.size} messages")
        for (msg in msgs) {
            val request = Request.Builder()
                .url("$server/api/messages/reactions/${msg.id}?token=$token")
                .get()
                .build()
            log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return
                        log("GET reactions response: ${body.take(100)}")
                        val json = JSONObject(body)
                        val reactionsJson = json.optJSONObject("reactions")
                        if (reactionsJson != null) {
                            val reactions = mutableMapOf<String, MutableList<String>>()
                            reactionsJson.keys().forEach { key ->
                                val arr = reactionsJson.getJSONArray(key)
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) list.add(arr.getString(i))
                                reactions[key] = list
                            }
                            // Сохраняем в Room
                            val reactionsJson = JSONObject(reactions as Map<*, *>).toString()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.messageDao().updateReactions(msg.id, reactionsJson)
                            }
                            runOnUiThread {
                                log("Reactions loaded for ${msg.id}: $reactions")
                                msgAdapter.setReactions(msg.id, reactions)
                            // Сохраняем в Room только если есть реакции
                            if (reactions.isNotEmpty()) {
                                val json = org.json.JSONObject(reactions as Map<*, *>).toString()
                                thread { db.messageDao().updateReactions(msg.id, json) }
                            }
                            }
                        }
                    }
                }
            })
        }
    }


    private fun showSearchOverlay() {
        val overlay = findViewById<LinearLayout>(R.id.searchOverlay)
        overlay.visibility = android.view.View.VISIBLE
        
        val searchField = overlay.findViewById<EditText>(R.id.searchField)
        val searchResults = overlay.findViewById<RecyclerView>(R.id.searchResults)
        val searchCount = overlay.findViewById<TextView>(R.id.searchCount)
        
        searchResults.layoutManager = LinearLayoutManager(this)
        searchField.requestFocus()
        
        overlay.findViewById<ImageButton>(R.id.btnSearchClose).setOnClickListener {
            overlay.visibility = android.view.View.GONE
        }
        
        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.length >= 2) {
                    thread {
                        val results = db.messageDao().searchMessages(selId, query)
                        runOnUiThread {
                            searchResults.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                                    val v = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
                                    return object : RecyclerView.ViewHolder(v) {}
                                }
                                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                                    val msg = results[position]
                                    holder.itemView.findViewById<TextView>(R.id.resultText).text = msg.text
                                    holder.itemView.findViewById<TextView>(R.id.resultMeta).text = "${msg.fromUser} • ${msg.time}"
                                }
                                override fun getItemCount(): Int = results.size
                            }
                            searchCount.text = "Найдено: ${results.size}"
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    private fun flushLogs() {
        if (logBuffer.isEmpty()) return
        val logsCopy = logBuffer.toList()
        logBuffer.clear()
        val json = org.json.JSONObject().apply {
            put("logs", org.json.JSONArray(logsCopy))
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = okhttp3.Request.Builder()
            .url("$server/api/logs?token=$token")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {}
        })
    }


    private fun cancelReply() {
        replyToMsg = null
        replyPreview.visibility = android.view.View.GONE
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun sendVoiceFile(file: java.io.File) {
        thread {
            try {
                val bytes = file.readBytes()
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("file", "voice.m4a", 
                        okhttp3.RequestBody.create(null, bytes))
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("$server/upload?token=$token")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                val respStr = response.body!!.string()
                    log("VOICE: upload response=$respStr")
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(respStr)
                    val fileId = json.optString("file_id", json.optString("url", ""))
                    log("VOICE: fileId=$fileId")
                    val vd = (bytes.size / 800).coerceAtLeast(1)
                    runOnUiThread {
                        val msg = org.json.JSONObject().apply {
                            put("type", "private")
                            put("to", selId)
                            put("text", "🎤 Голосовое $fileId")
                            put("file_id", fileId)
                            put("file_type", "voice")
                        put("file_name", "Голосовое ${vd}с")
                        }
                        ws?.send(msg.toString())
                        log("VOICE: sent successfully"); t("✅ Отправлено")
                    }
                } else {
                    runOnUiThread { log("VOICE: upload failed"); t("Ошибка отправки") }
                }
            } catch (e: Exception) {
                runOnUiThread { log("VOICE: error - ${e.message}"); t("Ошибка: ${e.message}") }
            }
        }
    }

    private fun connectCallSocket() {
        val callClient = OkHttpClient()
        val callRequest = Request.Builder().url("ws://2.26.71.102:8000/ws/call").build()
        callClient.newWebSocket(callRequest, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = org.json.JSONObject(text)
                if (msg.optString("type") == "call_offer") {
                    val from = msg.optString("from")
                    runOnUiThread {
                        val intent = android.content.Intent(this@MainActivity, com.mychat.app.activities.CallActivity::class.java).apply {
                            putExtra("name", from)
                            putExtra("avatar", from.take(1))
                            putExtra("caller", false)
                        }
                        startActivity(intent)
                    }
                }
            }
        })
    }
    
    private fun hideContextMenu() {
        selectedUserForDelete = null
        chatAdapter.selectedPosition = -1
        chatAdapter.notifyDataSetChanged()
        
        val slideUp = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, -1f
        )
        slideUp.duration = 250
        slideUp.interpolator = android.view.animation.AccelerateInterpolator()
        slideUp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) {}
            override fun onAnimationEnd(a: Animation?) {
                contextMenuBar.visibility = View.GONE
            }
            override fun onAnimationRepeat(a: Animation?) {}
        })
        contextMenuBar.startAnimation(slideUp)
    }
    

    private fun generateChatId(phone1: String, phone2: String): String {
        val ids = listOf(phone1, phone2).sorted()
        return java.security.MessageDigest.getInstance("MD5")
            .digest(ids.joinToString("").toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun deleteChat(user: User) {
        val chatId = generateChatId(currentUserPhone, user.username)
        val request = Request.Builder()
            .url("$server/api/chat/$chatId?user_id=$currentUserId&token=$token")
            .delete()
            .build()
        log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { t("Ошибка удаления") }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    log("Server response: OK, code=${response.code}")
                            if (response.isSuccessful) {
                        // Удаляем чат локально из Room
                        CoroutineScope(Dispatchers.IO).launch {
                            db.messageDao().deleteChat(chatId)
                        }
                        // Удаляем из списка
                        val index = users.indexOf(user)
                        if (index >= 0) {
                            users.removeAt(index)
                            chatAdapter.update(users)
                        }
                        t("Чат удалён")
                    } else {
                        t("Ошибка удаления")
                        loadUsers()
                    }
                }
            }
        })
    }
    
    private fun replyToMessage(msg: ChatMessage) {
        val replyText = "↪ ${msg.from}: ${msg.text.take(50)}...\n"
        msgInput.setText(replyText)
        msgInput.setSelection(msgInput.text.length)
    }
    
    private fun showForwardDialog(msg: ChatMessage) {
        pendingForward = msg
        closeChat()
        t("Выберите чат для пересылки")
    }
    
    private fun forwardMessage(msg: ChatMessage) {
        val forwardData = JSONObject().apply {
            put("from", msg.from)
            put("text", msg.text)
        }
        // Если текст пустой, но есть файл — добавляем подпись
        if (msg.file != null) {
            forwardData.put("file", JSONObject().apply {
                put("url", msg.file!!.url)
                put("name", msg.file!!.name)
                put("size", msg.file!!.size)
            })
            if (msg.text.isEmpty()) {
                forwardData.put("text", "📷 Фото")
            }
        }
        val json = JSONObject().apply {
            put("type", "forward")
            put("to", selId)
            put("forward", forwardData)
        }
        log("WS send: delete msg"); ws?.send(json.toString())
        t("Сообщение переслано!")
    }
    
    private fun loadMessages(userId: String) {
        val request = Request.Builder()
            .url("$server/messages/$userId?me=$me&token=$token")
            .build()
        log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        val messages = mutableListOf<ChatMessage>()
                        for (i in 0 until json.length()) {
                            val obj = json.getJSONObject(i)
                            val fileId = obj.optString("file_id", "")
                            val fileType = obj.optString("file_type", "")
                            val fileName = obj.optString("file_name", "")
                            val file = if (fileId.isNotEmpty()) FileInfo(fileName, fileId) else null
                            messages.add(ChatMessage(
                                id = obj.optString("id"),
                                from = obj.optString("from"),
                                to = obj.optString("to"),
                                text = obj.optString("text"),
                                time = obj.optString("time"),
                                file = file
                            ))
                        }
                        runOnUiThread {
                            msgAdapter = MessageAdapter(
                                me = me,
                                onDownload = { url, name -> downloadFile(url, name) },
                                onMessageLongClick = { msg -> showMessageActions(msg) },
                                onSaveReaction = { msgId, json ->
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        db.messageDao().updateReactions(msgId, json)
                                    }
                                },
                                appContext = applicationContext,
                                onLog = { msg -> log(msg) }
                            )
                            messagesList.adapter = msgAdapter
                            messagesList.scrollToPosition(msgAdapter.itemCount - 1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }
    
    private fun editMessage(msg: ChatMessage) {
        if (msg.from != me) {
            t("Можно изменить только свои сообщения")
            return
        }
        val input = EditText(this).apply {
            setText(msg.text)
            setTextColor(0xffffffff.toInt())
            setHintTextColor(0xff636366.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(30, 20, 30, 20)
        }
        AlertDialog.Builder(this)
            .setTitle("Изменить сообщение")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty() && newText != msg.text) {
                    val json = JSONObject().apply {
                        put("type", "edit")
                        put("to", msg.to)
                        put("msg_id", msg.id)
                        put("text", newText)
                    }
                    log("WS send: clear history"); ws?.send(json.toString())
                    t("Сообщение изменено")
                    // Обновляем через 500мс
                    handler.postDelayed({ refreshMessages() }, 500)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    


    private fun forwardSelectedMessages() {
        val ids = msgAdapter.selectedIds.toList()
        if (ids.isEmpty()) {
            t("Ничего не выбрано")
            return
        }
        // Собираем выбранные сообщения
        val messagesToForward = msgAdapter.getItems()
            .filterIsInstance<ChatMessage>()
            .filter { it.id in ids }
        
        pendingForwardMessages = messagesToForward
        isForwardMode = true
        log("FORWARD: ${messagesToForward.size} messages to forward")
        
        // Выходим из чата
        exitSelectMode()
        closeChat()
        t("Выберите чат для пересылки")
    }


    private fun exitSelectMode() {
        log("UI: exit select mode")
        isSelectMode = false
        msgAdapter.selectMode = false
        msgAdapter.selectedIds.clear()
        msgAdapter.notifyDataSetChanged()
        log("UI: select mode OFF"); selectPanel.visibility = android.view.View.GONE
        chatHeader.visibility = android.view.View.VISIBLE
    }

        private fun deleteSelectedMessages() {
        val ids = msgAdapter.selectedIds.toList()
        if (ids.isEmpty()) {
            t("Ничего не выбрано")
            return
        }
        
        log("DELETE: deleting ${ids.size} messages")
        
        // Сначала собираем все сообщения
        val messagesToDelete = msgAdapter.getItems()
            .filterIsInstance<ChatMessage>()
            .filter { it.id in ids }
            .toList()  // Фиксируем список
        
        // Отправляем на сервер и помечаем в Room
        for (msg in messagesToDelete) {
            val json = org.json.JSONObject().apply {
                put("type", "delete")
                put("to", msg.to)
                put("msg_id", msg.id)
            }
            log("WS send: delete msg"); ws?.send(json.toString())
            thread { db.messageDao().markDeleted(msg.id); log("Room: markDeleted ${msg.id}") }
        }
        
        // Удаляем из адаптера все сразу
        for (msg in messagesToDelete) {
            msgAdapter.markDeleted(msg.id)
        }
        
        exitSelectMode()
        log("DELETE: done ${messagesToDelete.size} messages")
        t("Удалено: ${messagesToDelete.size}")
    }

    private fun deleteMessage(msg: ChatMessage) {
        if (msg.from != me) {
            t("Можно удалить только свои сообщения")
            return
        }
        val json = JSONObject().apply {
            put("type", "delete")
            put("to", msg.to)
            put("msg_id", msg.id)
        }
        log("WS send: mute"); ws?.send(json.toString())
        thread { db.messageDao().markDeleted(msg.id); log("Room: markDeleted ${msg.id}") }
        msgAdapter.markDeleted(msg.id)
        // Меняем текст локально сразу
        msgAdapter.markDeleted(msg.id)
        t("Сообщение удалено")
    }
    
    private fun addToFavorites(msg: ChatMessage) {
        val json = JSONObject().apply {
            put("id", msg.id)
            put("from", msg.from)
            put("to", msg.to)
            put("text", msg.text)
            put("time", msg.time)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$server/favorites/add?token=$token")
            .post(requestBody)
            .build()
        log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { t("Ошибка") }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    log("Server response: OK, code=${response.code}")
                            if (response.isSuccessful) {
                        t("✅ Добавлено в избранное!")
                    } else {
                        t("Ошибка добавления")
                    }
                }
            }
        })
    }
    
    private fun showStickers() {
        val bs = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.bottom_stickers, null)
        bs.setContentView(v)
        val list = listOf(R.drawable.sticker1, R.drawable.sticker2, R.drawable.sticker3, R.drawable.sticker4, R.drawable.sticker5, R.drawable.sticker6, R.drawable.sticker7, R.drawable.sticker8)
        v.findViewById<RecyclerView>(R.id.stickersGrid).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = StickerAdapter(list) { resId ->
            bs.dismiss()
            // Отправляем стикер как эмодзи-текст (временно)
            val stickerNames = mapOf(
                R.drawable.sticker1 to "🐱❤️",
                R.drawable.sticker2 to "🐱🦁",
                R.drawable.sticker3 to "🐱🖤",
                R.drawable.sticker4 to "🐱🤍",
                R.drawable.sticker5 to "🐱✨",
                R.drawable.sticker6 to "🐱😴",
                R.drawable.sticker7 to "🐱🎀",
                R.drawable.sticker8 to "🐱🐼"
            )
            val emoji = stickerNames[resId] ?: "🐱"
            val json = JSONObject().apply {
                put("type", "private")
                put("to", selId)
                put("text", emoji)
            }
            ws?.send(json.toString())
        }
        }
        bs.show()
    }
    
    private fun blockUser() {
        if (selId.isEmpty()) return
        val json = JSONObject().apply {
            put("type", "block")
            put("to", selId)
        }
        ws?.send(json.toString())
        // Сохраняем в Room
        thread {
            val settings = db.messageDao().getChatSettings(selId) ?: ChatSettings(selId)
            db.messageDao().saveChatSettings(settings.copy(isBlocked = true))
            // Синхронизация с сервером
            try {
                val body = JSONObject().apply {
                    put("chat_key", selId)
                    put("is_blocked", true)
                }.toString().toRequestBody("application/json".toMediaType())
                client.newCall(Request.Builder().url("$server/chat_settings?token=$token").post(body).build()).execute()
            } catch (e: Exception) {}
        }
        isBlocked = true
        t("Пользователь заблокирован")
        closeChat()
    }
    
        private fun searchInChat(query: String) {
        thread {
            try {
                val results = db.messageDao().searchMessages(selId, query)
                if (results.isNotEmpty()) {
                    val msgs = results.map { e ->
                        ChatMessage(e.id, e.fromUser, e.toUser, e.text, e.time)
                    }
                    handler.post {
                        msgAdapter.update(msgs)
                        t("Найдено: ${results.size}")
                    }
                } else {
                    handler.post { t("Ничего не найдено") }
                }
            } catch (e: Exception) {
                handler.post { t("Ошибка поиска") }
            }
        }
    }
    
    private fun clearHistory() {
        thread { db.messageDao().deleteChat(selId) }
        val ck = chatKey(me, selId)
        val request = Request.Builder()
            .url("$server/chat/clear/$ck?token=$token")
            .post(RequestBody.create(null, ""))
            .build()
        log("HTTP: request"); client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                handler.post {
                    msgAdapter.update(emptyList())
                    t("История очищена")
                }
            }
        })
    }
    
    private fun showUserInfo() {
        val user = users.find { it.username == selId }
        if (user == null) {
            t("Информация не найдена")
            return
        }
        val info = """
            Имя: ${user.name.ifEmpty { user.username }}
            Логин: ${user.username}
            Статус: ${if (user.online) "Онлайн" else "Офлайн"}
            Последний вход: ${user.lastSeen.ifEmpty { "Неизвестно" }}
            О себе: ${user.bio.ifEmpty { "Не указано" }}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Информация")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private var isMuted = false
    private var isBlocked = false
    
    private fun toggleMute() {
        isMuted = !isMuted
        log("Mute: $isMuted for $selId")
        // Обновляем иконку в списке
        users.find { it.username == selId }?.isMuted = isMuted
        chatAdapter.notifyDataSetChanged()
        // Сохраняем в Room
        thread {
            val settings = db.messageDao().getChatSettings(selId) ?: ChatSettings(selId)
            db.messageDao().saveChatSettings(settings.copy(isMuted = isMuted))
            // Синхронизация с MongoDB
            try {
                val json = JSONObject().apply {
                    put("chat_key", selId)
                    put("is_muted", isMuted)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                client.newCall(Request.Builder().url("$server/chat_settings?token=$token").post(body).build()).execute()
            } catch (e: Exception) {}
        }
        if (isMuted) {
            t("🔇 Уведомления отключены")
        } else {
            t("🔔 Уведомления включены")
        }
    }
    
    private fun chatKey(u1: String, u2: String) = listOf(u1, u2).sorted().joinToString("_")
    
    private fun t(msg: String) {
        handler.post {
            val toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
            toast.view?.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.notification_slide_down))
            toast.show()
        }
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
