package com.mychat.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.adapters.FavoritesAdapter
import com.mychat.app.models.FavoriteItem
import com.mychat.app.network.ApiClient
import com.mychat.app.utils.Constants
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
    private var token = ""
    private var username = ""
    private var serverUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        token = intent.getStringExtra("token") ?: prefs.getString("token", "") ?: ""
        username = intent.getStringExtra("username") ?: prefs.getString("username", "") ?: ""
        serverUrl = Constants.SERVER_URL

        initViews()
        setupFilters()
        setupSearch()
        setupSwipeToDelete()
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
            onRemove = { item -> removeFavorite(item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.backBtn).setOnClickListener { finish() }
        clearBtn.setOnClickListener {
            searchInput.text.clear()
            clearBtn.visibility = View.GONE
            adapter.search("")
        }
    }

    private fun loadFavorites() {
        if (token.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Ошибка: не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Thread {
            try {
                val response = ApiClient.get("$serverUrl/favorites/$username", token)
                if (response.isSuccessful) {
                    val json = JSONArray(response.body!!.string())
                    val items = mutableListOf<FavoriteItem>()
                    for (i in 0 until json.length()) {
                        val obj = json.getJSONObject(i)
                        items.add(FavoriteItem(
                            msgId = obj.optString("msg_id"),
                            text = obj.optString("text", ""),
                            from = obj.optString("from_user", obj.optString("username", "")),
                            time = obj.optString("time", obj.optString("added_at", "")),
                            type = obj.optString("type", "message"),
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
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun removeFavorite(item: FavoriteItem) {
        Thread {
            try {
                val response = ApiClient.post(
                    "$serverUrl/api/favorites/remove/${item.msgId}",
                    token,
                    okhttp3.RequestBody.create("application/json".toMediaType(), "{}")
                )
                if (response.isSuccessful) {
                    runOnUiThread {
                        favorites.remove(item)
                        adapter.update(favorites)
                        favCount.text = favorites.size.toString()
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun updateEmptyState() {
        emptyState.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupFilters() {}
    private fun setupSearch() {}
    private fun setupSwipeToDelete() {}
}
