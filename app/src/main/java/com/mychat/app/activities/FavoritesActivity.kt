package com.mychat.app.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.adapters.FavoritesAdapter
import com.mychat.app.models.FavoriteItem
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoritesAdapter
    private lateinit var searchInput: EditText
    private lateinit var clearBtn: ImageView
    private lateinit var filterContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var favCount: TextView
    
    private val favorites = mutableListOf<FavoriteItem>()
    private val client = OkHttpClient()
    private var token = ""
    private var username = ""
    private var serverUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        token = intent.getStringExtra("token") ?: prefs.getString("token", "") ?: ""
        username = intent.getStringExtra("username") ?: prefs.getString("username", "") ?: ""
        serverUrl = prefs.getString("server_url", "http://2.26.71.102:8000") ?: "http://2.26.71.102:8000"

        initViews()
        setupFilters()
        setupSearch()
        loadFavorites()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.favoritesRecycler)
        searchInput = findViewById(R.id.searchInput)
        clearBtn = findViewById(R.id.clearBtn)
        filterContainer = findViewById(R.id.filterContainer)
        emptyState = findViewById(R.id.emptyState)
        favCount = findViewById(R.id.favCount)

        adapter = FavoritesAdapter(
            onItemClick = { item ->
                Toast.makeText(this, "Открыть чат с ${item.from}", Toast.LENGTH_SHORT).show()
            },
            onRemove = { item ->
                removeFavorite(item)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

            Toast.makeText(this, "Выберите сообщение для сохранения", Toast.LENGTH_SHORT).show()
        }

        clearBtn.setOnClickListener {
            searchInput.text.clear()
            clearBtn.visibility = View.GONE
            adapter.search("")
        }
    }

    private fun setupFilters() {
        val filters = listOf("Все", "Текст", "Файлы", "Фото", "Группы", "Ленты")
        filterContainer.removeAllViews()

        filters.forEachIndexed { index, filter ->
            val chip = TextView(this).apply {
                text = filter
                val isActive = index == 0
                setTextColor(resources.getColor(if (isActive) android.R.color.white else android.R.color.darker_gray))
                setBackgroundResource(if (isActive) R.drawable.bg_filter_active else R.drawable.bg_filter_inactive)
                setPadding(32, 12, 32, 12)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
                isClickable = true
                setOnClickListener {
                    for (i in 0 until filterContainer.childCount) {
                        val child = filterContainer.getChildAt(i) as TextView
                        val active = child.text == filter
                        child.setBackgroundResource(
                            if (active) R.drawable.bg_filter_active 
                            else R.drawable.bg_filter_inactive
                        )
                        child.setTextColor(
                            resources.getColor(
                                if (active) android.R.color.white 
                                else android.R.color.darker_gray
                            )
                        )
                    }
                    adapter.filterBy(filter)
                    updateEmptyState()
                }
            }
            filterContainer.addView(chip)
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    clearBtn.visibility = View.VISIBLE
                    adapter.search(query)
                } else {
                    clearBtn.visibility = View.GONE
                    adapter.search("")
                }
                updateEmptyState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadFavorites() {
        if (token.isEmpty() || username.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        val url = "$serverUrl/favorites/$username?token=$token"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@FavoritesActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { body ->
                    try {
                        val json = JSONArray(body.string())
                        val items = mutableListOf<FavoriteItem>()
                        for (i in 0 until json.length()) {
                            val obj = json.getJSONObject(i)
                            items.add(FavoriteItem(
                                msgId = obj.optString("msg_id"),
                                text = obj.optString("text"),
                                from = obj.optString("from_user"),
                                time = obj.optString("time"),
                                type = detectType(obj),
                                isGroup = obj.optBoolean("is_group"),
                                isFeed = obj.optBoolean("is_feed")
                            ))
                        }
                        runOnUiThread {
                            favorites.clear()
                            favorites.addAll(items)
                            adapter.update(items)
                            favCount.text = items.size.toString()
                            updateEmptyState()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun removeFavorite(item: FavoriteItem) {
        val url = "$serverUrl/favorites/remove/${item.msgId}?token=$token"
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ""))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@FavoritesActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        favorites.remove(item)
                        adapter.update(favorites)
                        favCount.text = favorites.size.toString()
                        updateEmptyState()
                        Toast.makeText(this@FavoritesActivity, "Удалено из избранного", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun detectType(obj: JSONObject): String {
        return when {
            obj.has("file") -> "file"
            obj.optString("text").contains(".jpg") || 
            obj.optString("text").contains(".png") ||
            obj.optString("text").contains(".jpeg") -> "photo"
            obj.optBoolean("is_group") -> "group"
            obj.optBoolean("is_feed") -> "feed"
            else -> "text"
        }
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
