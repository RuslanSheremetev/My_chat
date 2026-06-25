package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.graphics.BitmapFactory
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

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
                } catch (e: Exception) { dateStr }
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
            is ChatMessage -> if (item.from == me) TYPE_OUT else TYPE_IN
            else -> TYPE_IN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
                DateViewHolder(view)
            }
            TYPE_IN -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_msg_in, parent, false)
                InViewHolder(view)
            }
            TYPE_OUT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_msg_out, parent, false)
                OutViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_msg_in, parent, false)
                InViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when {
            item is String && holder is DateViewHolder -> {
                holder.dateText.text = item
            }
            item is ChatMessage && holder is InViewHolder -> {
                holder.from.text = item.from
                holder.text.text = item.text
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(item.time) ?: Date()
                )
                loadPhoto(item, holder.imageMsg)
                holder.itemView.setOnLongClickListener { onMessageLongClick(item); true }
            }
            item is ChatMessage && holder is OutViewHolder -> {
                holder.text.text = item.text
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(item.time) ?: Date()
                )
                loadPhoto(item, holder.imageMsg)
                holder.itemView.setOnLongClickListener { onMessageLongClick(item); true }
            }
        }
    }

    private fun loadPhoto(item: ChatMessage, imageView: ImageView) {
        val fi = item.file
        if (fi != null && fi.url.isNotEmpty()) {
            imageView.visibility = View.VISIBLE
            var url = fi.url
            if (!url.startsWith("http")) url = "http://2.26.71.102:8000$url"
            thread {
                try {
                    val bmp = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                    imageView.post {
                        if (bmp != null) imageView.setImageBitmap(bmp)
                        else imageView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    imageView.post { imageView.visibility = View.GONE }
                }
            }
        } else {
            imageView.visibility = View.GONE
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
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
    }
}
