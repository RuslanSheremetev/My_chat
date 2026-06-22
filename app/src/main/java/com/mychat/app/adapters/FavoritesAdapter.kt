package com.mychat.app.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.FavoriteItem
import java.text.SimpleDateFormat
import java.util.*

class FavoritesAdapter(
    private val onItemClick: (FavoriteItem) -> Unit,
    private val onRemove: (FavoriteItem) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    private var items = mutableListOf<FavoriteItem>()
    private var filteredItems = mutableListOf<FavoriteItem>()
    private var currentFilter: String = "Все"

    fun update(newItems: List<FavoriteItem>) {
        items.clear()
        items.addAll(newItems)
        applyFilter(currentFilter)
    }

    fun filterBy(type: String) {
        currentFilter = type
        applyFilter(type)
    }

    private fun applyFilter(type: String) {
        filteredItems.clear()
        filteredItems.addAll(
            when (type) {
                "Текст" -> items.filter { it.type == "text" }
                "Файлы" -> items.filter { it.type == "file" }
                "Фото" -> items.filter { it.type == "photo" }
                "Группы" -> items.filter { it.isGroup }
                "Ленты" -> items.filter { it.isFeed }
                else -> items
            }
        )
        notifyDataSetChanged()
    }

    fun search(query: String) {
        if (query.isEmpty()) {
            applyFilter(currentFilter)
            return
        }
        val filtered = items.filter {
            it.text.contains(query, ignoreCase = true) ||
            it.from.contains(query, ignoreCase = true)
        }
        filteredItems.clear()
        filteredItems.addAll(filtered)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = filteredItems.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: TextView = itemView.findViewById(R.id.favAvatar)
        private val name: TextView = itemView.findViewById(R.id.favName)
        private val time: TextView = itemView.findViewById(R.id.favTime)
        private val text: TextView = itemView.findViewById(R.id.favText)
        private val typeIcon: ImageView = itemView.findViewById(R.id.favTypeIcon)
        private val removeBtn: View = itemView.findViewById(R.id.favRemoveBtn)
        private val badge: TextView = itemView.findViewById(R.id.favBadge)

        fun bind(item: FavoriteItem) {
            avatar.text = item.from.take(1).uppercase()
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.parseColor(item.avatarColor))
            avatar.background = bg

            name.text = item.from

            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(item.time)
                time.text = sdf.format(date ?: Date())
            } catch (e: Exception) {
                time.text = item.time
            }

            text.text = item.text

            val badgeText = when {
                item.isGroup -> "Группа"
                item.isFeed -> "Лента"
                item.type == "file" -> "Файл"
                item.type == "photo" -> "Фото"
                else -> ""
            }
            if (badgeText.isNotEmpty()) {
                badge.visibility = View.VISIBLE
                badge.text = badgeText
            } else {
                badge.visibility = View.GONE
            }

            val iconRes = when (item.type) {
                "file" -> R.drawable.ic_attach
                "photo" -> R.drawable.ic_photo
                else -> R.drawable.ic_message
            }
            typeIcon.setImageResource(iconRes)
            typeIcon.visibility = if (item.type != "text") View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(item) }
            removeBtn.setOnClickListener { onRemove(item) }
        }
    }
}
