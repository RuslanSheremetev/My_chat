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
import com.mychat.app.utils.FileCache
import java.io.File
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
                // Если это файл — делаем кликабельным
                if (item.file != null && item.text.startsWith("File:")) {
                    holder.text.isClickable = true
                    holder.text.setOnClickListener {
                        val url = item.file!!.url.let { if (it.startsWith("http")) it else "http://2.26.71.102:8000$it" }
                        // Проверяем кеш
                        val cachedFile = com.mychat.app.utils.FileCache.getCachedFile(url)
                        if (cachedFile != null) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                holder.itemView.context,
                                "${holder.itemView.context.packageName}.fileprovider",
                                cachedFile
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            holder.itemView.context.startActivity(intent)
                        } else {
                            thread {
                                try {
                                    val bytes = java.net.URL(url).readBytes()
                                    val savedFile = com.mychat.app.utils.FileCache.saveToCache(url, bytes)
                                    savedFile?.let { file ->
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            holder.itemView.context,
                                            "${holder.itemView.context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "*/*")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        holder.itemView.post {
                                            holder.itemView.context.startActivity(intent)
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(item.time) ?: Date()
                )
                loadPhoto(item, holder.imageMsg)
                holder.itemView.setOnLongClickListener { onMessageLongClick(item); true }
                // Показываем реакции
                val reactionsStr = formatReactions(item.reactions)
                holder.reactionsText.text = reactionsStr
                holder.reactionsText.visibility = if (reactionsStr.isNotEmpty()) View.VISIBLE else View.GONE
            }
            item is ChatMessage && holder is OutViewHolder -> {
                holder.text.text = item.text
                // Если это файл — делаем кликабельным
                if (item.file != null && item.text.startsWith("File:")) {
                    holder.text.isClickable = true
                    holder.text.setOnClickListener {
                        val url = item.file!!.url.let { if (it.startsWith("http")) it else "http://2.26.71.102:8000$it" }
                        // Проверяем кеш
                        val cachedFile = com.mychat.app.utils.FileCache.getCachedFile(url)
                        if (cachedFile != null) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                holder.itemView.context,
                                "${holder.itemView.context.packageName}.fileprovider",
                                cachedFile
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            holder.itemView.context.startActivity(intent)
                        } else {
                            thread {
                                try {
                                    val bytes = java.net.URL(url).readBytes()
                                    val savedFile = com.mychat.app.utils.FileCache.saveToCache(url, bytes)
                                    savedFile?.let { file ->
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            holder.itemView.context,
                                            "${holder.itemView.context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "*/*")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        holder.itemView.post {
                                            holder.itemView.context.startActivity(intent)
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
                holder.msgStatus.text = if (item.id.startsWith("sending_")) "🕒" else "✅✅"
                holder.time.text = timeFormat.format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(item.time) ?: Date()
                )
                loadPhoto(item, holder.imageMsg)
                holder.itemView.setOnLongClickListener { onMessageLongClick(item); true }
                // Показываем реакции
                val reactionsStr = formatReactions(item.reactions)
                holder.reactionsText.text = reactionsStr
                holder.reactionsText.visibility = if (reactionsStr.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadPhoto(item: ChatMessage, imageView: ImageView) {
        val fi = item.file
        val isImage = fi?.url?.let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".gif") || it.endsWith(".webp") } ?: false
            if (fi != null && fi.url.isNotEmpty() && isImage) {
            imageView.visibility = View.VISIBLE
            // Не показываем shimmer если клавиатура открыта
            val imm = imageView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            if (imm.isAcceptingText) {
                imageView.setBackgroundColor(0xff1c1c1e.toInt())
            } else {
                imageView.setBackgroundResource(R.drawable.shimmer_placeholder)
                imageView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                    imageView.context, R.anim.shimmer
                ))
            }
            imageView.setOnLongClickListener { onMessageLongClick(item); true }
            imageView.setBackgroundColor(0xff1c1c1e.toInt())
            imageView.setOnClickListener {
                // Собираем все фото из чата и открываем галерею
                val allPhotos = items.filterIsInstance<ChatMessage>()
                    .filter { it.file != null && it.file!!.url.isNotEmpty() }
                    .map { it.file!!.url }
                val index = allPhotos.indexOf(fi.url)
                val intent = android.content.Intent(imageView.context, com.mychat.app.activities.GalleryActivity::class.java).apply {
                    putStringArrayListExtra("photos", ArrayList(allPhotos))
                    putExtra("index", if (index >= 0) index else 0)
                }
                imageView.context.startActivity(intent)
            }
            var url = fi.url
            if (!url.startsWith("http")) url = "http://2.26.71.102:8000$url"
            
            // Проверяем кеш
            val cached = FileCache.getCachedFile(url)
            if (cached != null) {
                val bmp = BitmapFactory.decodeFile(cached.absolutePath)
                imageView.setImageBitmap(bmp)
                return
            }
            
            // Загружаем с задержкой и уменьшением
            imageView.postDelayed({
                thread {
                    try {
                        val bytes = java.net.URL(url).readBytes()
                        FileCache.saveToCache(url, bytes)
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        imageView.post {
                            if (bmp != null) {
                        imageView.setImageBitmap(bmp)
                        imageView.clearAnimation()
                        imageView.setBackgroundResource(0)
                        imageView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                            imageView.context, R.anim.photo_load_in
                        ))
                    }
                            else imageView.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        imageView.post { imageView.visibility = View.GONE }
                    }
                }
            }, 50)
        } else {
            imageView.visibility = View.GONE
        }
    }


    private fun formatReactions(reactions: Map<String, out List<String>>): String {
        if (reactions.isEmpty()) return ""
        return reactions.entries.joinToString("  ") { (emoji, users) ->
            "$emoji ${users.size}"
        }
    }

    fun addReaction(msgId: String, emoji: String, from: String) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        if (index >= 0) {
            val msg = items[index] as ChatMessage
            val newReactions = msg.reactions.toMutableMap()
            // Удаляем пользователя из других реакций
            for (key in newReactions.keys) {
                newReactions[key] = newReactions[key]?.filter { it != from }?.toMutableList() ?: mutableListOf()
                if (newReactions[key]?.isEmpty() == true) {
                    newReactions.remove(key)
                }
            }
            // Добавляем/убираем реакцию
            val users = newReactions.getOrDefault(emoji, mutableListOf()).toMutableList()
            if (users.contains(from)) {
                users.remove(from)
                if (users.isEmpty()) newReactions.remove(emoji)
                else newReactions[emoji] = users
            } else {
                users.add(from)
                newReactions[emoji] = users
            }
            items[index] = msg.copy(reactions = newReactions)
            notifyItemChanged(index)
        }
    }
    
    fun markDeleted(msgId: String) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    
    fun addMessage(msg: ChatMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }
    
    override fun getItemCount(): Int = items.size

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        init {
            itemView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                itemView.context, R.anim.date_slide_down
            ))
        }
    }

    class InViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val from: TextView = view.findViewById(R.id.from)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
        val reactionsText: TextView = view.findViewById(R.id.reactionsText)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
        val msgStatus: TextView = view.findViewById(R.id.msgStatus)
        val reactionsText: TextView = view.findViewById(R.id.reactionsText)
    }
}
