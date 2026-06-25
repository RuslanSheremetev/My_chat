package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.graphics.BitmapFactory
import java.net.URL
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mychat.app.R
import com.mychat.app.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val me: String,
    private val onDownload: (String, String) -> Unit,
    private val onMessageLongClick: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val today = dateFormat.format(Date())

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_IN = 1
        private const val TYPE_OUT = 2
    }

    fun update(list: List<ChatMessage>) {
        items.clear()
        var lastDate = ""
        for (msg in list) {
            val msgDate = msg.time.take(10)
            if (msgDate != lastDate) {
                lastDate = msgDate
                items.add(formatDate(msgDate))
            }
            items.add(msg)
        }
        notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String): String {
        return when (dateStr) {
            today -> "Сегодня"
            getYesterday() -> "Вчера"
            else -> {
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                    SimpleDateFormat("d MMMM", Locale("ru")).format(date ?: Date())
                } catch (e: Exception) {
                    dateStr
                }
            }
        }
    }

    private fun getYesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is String -> TYPE_DATE
            is ChatMessage -> {
                if (item.from == me) TYPE_OUT else TYPE_IN
            }
            else -> TYPE_IN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date, parent, false)
                DateViewHolder(view)
            }
            TYPE_IN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_in, parent, false)
                InViewHolder(view)
            }
            TYPE_OUT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_out, parent, false)
                OutViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_in, parent, false)
                InViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var lastClickTime = 0L
        val item = items[position]
        when {
            item is String && holder is DateViewHolder -> {
                holder.dateText.text = item
            }
            item is ChatMessage && holder is InViewHolder -> {
                holder.from.text = item.from
                holder.text.text = item.text
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(item.time) ?: Date()
                )
                holder.itemView.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 300) {
                        // Двойное нажатие - лайк
                        onMessageLongClick(item)
                    }
                    lastClickTime = now
                }
                holder.itemView.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 300) {
                        // Двойное нажатие - лайк
                        onMessageLongClick(item)
                    }
                    lastClickTime = now
                }
                holder.itemView.setOnLongClickListener {
                    onMessageLongClick(item)
                    true
                }
            }
            item is ChatMessage && holder is OutViewHolder -> {
                holder.text.text = item.text
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(item.time) ?: Date()
                )
                holder.itemView.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 300) {
                        // Двойное нажатие - лайк
                        onMessageLongClick(item)
                    }
                    lastClickTime = now
                }
                holder.itemView.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 300) {
                        // Двойное нажатие - лайк
                        onMessageLongClick(item)
                    }
                    lastClickTime = now
                }
                holder.itemView.setOnLongClickListener {
                    onMessageLongClick(item)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
    }

    class InViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val from: TextView = view.findViewById(R.id.from)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val image: ImageView = view.findViewById(R.id.image)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val readStatus: TextView = view.findViewById(R.id.readStatus)
    }
    fun getMessages(): List<ChatMessage> {
        return items.filterIsInstance<ChatMessage>()
    }
    
    fun addReaction(msgId: String, emoji: String, username: String) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        if (index >= 0) {
            val msg = items[index] as ChatMessage
            val reactions = msg.reactions.toMutableMap()
            val users = reactions.getOrDefault(emoji, mutableListOf()).toMutableList()
            if (!users.contains(username)) {
                users.add(username)
                reactions[emoji] = users
                items[index] = msg.copy(reactions = reactions)
                notifyItemChanged(index)
            }
        }
    }
    
    fun markRead(msgId: String) {
        val index = items.indexOfFirst { 
            it is ChatMessage && it.id == msgId 
        }
        if (index >= 0) {
            val msg = items[index] as ChatMessage
            items[index] = msg.copy(read = true)
            notifyItemChanged(index)
        }
    }
}
