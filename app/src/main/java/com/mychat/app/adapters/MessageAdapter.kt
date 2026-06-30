package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
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
    var selectMode: Boolean = false,
    val selectedIds: MutableSet<String> = mutableSetOf(),
    private val me: String,
    private val onDownload: (String, String) -> Unit,
    private val onMessageLongClick: (ChatMessage) -> Unit = {},
    private val onSaveReaction: ((String, String) -> Unit)? = null,
    private val appContext: android.content.Context? = null
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
                holder.selectCheck.visibility = if (selectMode) android.view.View.VISIBLE else android.view.View.GONE
                holder.selectCheck.isChecked = selectedIds.contains(item.id)
                holder.selectCheck.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                }
                holder.itemView.setOnClickListener {
                    if (selectMode) {
                        holder.selectCheck.isChecked = !holder.selectCheck.isChecked
                    }
                }
                holder.selectCheck.visibility = if (selectMode) android.view.View.VISIBLE else android.view.View.GONE
                holder.selectCheck.isChecked = selectedIds.contains(item.id)
                holder.selectCheck.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                }
                holder.itemView.setOnClickListener {
                    if (selectMode) {
                        holder.selectCheck.isChecked = !holder.selectCheck.isChecked
                    }
                }
                showReplyQuote(holder.itemView, item)
                if (item.file?.url?.endsWith(".m4a") == true || item.text.contains("🎤 Голосовое")) {
                    showVoicePlayer(holder.itemView, item)
                    holder.text.visibility = View.GONE
                } else {
                    holder.text.text = item.text
                    holder.text.visibility = View.VISIBLE
                }
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
                android.util.Log.d("Reaction", "In bind: id=${item.id}, reactions=${item.reactions}, str=$reactionsStr")
                holder.reactionsText.text = reactionsStr
                holder.reactionsText.visibility = if (reactionsStr.isNotEmpty()) View.VISIBLE else View.GONE
            }
            item is ChatMessage && holder is OutViewHolder -> {
                holder.selectCheck.visibility = if (selectMode) android.view.View.VISIBLE else android.view.View.GONE
                holder.selectCheck.isChecked = selectedIds.contains(item.id)
                holder.selectCheck.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                }
                holder.itemView.setOnClickListener {
                    if (selectMode) {
                        holder.selectCheck.isChecked = !holder.selectCheck.isChecked
                    }
                }
                showReplyQuote(holder.itemView, item)
                if (item.file?.url?.endsWith(".m4a") == true || item.text.contains("🎤 Голосовое")) {
                    showVoicePlayer(holder.itemView, item)
                    holder.text.visibility = View.GONE
                } else {
                    holder.text.text = item.text
                    holder.text.visibility = View.VISIBLE
                }
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
                // msgStatus removed
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
            imageView.setBackgroundColor(0xff1c1c1e.toInt())
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


    private fun formatReactions(reactions: Map<String, out List<String>>): String { android.util.Log.d("Reaction", "formatReactions: input=$reactions")
        if (reactions.isEmpty()) return ""
        return reactions.entries.joinToString("  ") { (emoji, users) ->
            if (users.size > 1) "$emoji ${users.size}" else emoji
        }
    }


    fun setReactions(msgId: String, reactions: MutableMap<String, MutableList<String>>) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        if (index >= 0) {
            val msg = items[index] as ChatMessage
            // Обновляем только если изменились
            if (msg.reactions != reactions) {
                items[index] = msg.copy(reactions = reactions)
                notifyItemChanged(index)
// Room save already done above
            }
        }
    }

    fun addReaction(msgId: String, emoji: String, from: String) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        android.util.Log.d("Reaction", "addReaction: msgId=$msgId, emoji=$emoji, from=$from, index=$index")
        if (index >= 0) {
            val msg = items[index] as ChatMessage
            android.util.Log.d("Reaction", "Before: reactions=${msg.reactions}")
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
            android.util.Log.d("Reaction", "After: reactions=${newReactions}, formatted=${formatReactions(newReactions)}")
            notifyItemChanged(index)
            // Сохраняем в Room
            val json = org.json.JSONObject(newReactions as Map<*, *>).toString()
            val ctx = appContext
            if (ctx != null) {
                thread {
                    com.mychat.app.data.AppDatabase.getInstance(ctx).messageDao().updateReactions(msgId, json)
                }
            }
android.util.Log.d("REACTION", "Saving to Room: $msgId -> $newReactions")
            // onSaveReaction removed
        }
    }
    
    fun markDeleted(msgId: String) {
        val index = items.indexOfFirst { it is ChatMessage && it.id == msgId }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    

    private fun showReplyQuote(view: View, msg: ChatMessage) {
        val quoteView = view.findViewById<android.widget.LinearLayout>(R.id.replyQuote) ?: return
        if (msg.text.startsWith("↪ ")) {
            quoteView.visibility = View.VISIBLE
            val parts = msg.text.removePrefix("↪ ").split(": ", limit = 2)
            view.findViewById<TextView>(R.id.quoteAuthor)?.text = parts.getOrElse(0) { "" }
            view.findViewById<TextView>(R.id.quoteText)?.text = parts.getOrElse(1) { "" }.take(100)
        } else {
            quoteView.visibility = View.GONE
        }
    }


    private fun showYoutubePreview(view: View, text: String) {
        val regex = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)")
        val match = regex.find(text) ?: return
        val videoId = match.groupValues[1]
        val preview = view.findViewById<LinearLayout>(R.id.ytPreview) ?: return
        preview.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.ytTitle)?.text = "Загрузка..."
        thread {
            try {
                val json = org.json.JSONObject(java.net.URL("https://www.youtube.com/oembed?url=https://youtube.com/watch?v=$videoId&format=json").readText())
                val title = json.optString("title", "YouTube")
                val bytes = java.net.URL("https://img.youtube.com/vi/$videoId/0.jpg").readBytes()
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                view.post {
                    view.findViewById<ImageView>(R.id.ytThumbnail)?.setImageBitmap(bmp)
                    view.findViewById<TextView>(R.id.ytTitle)?.text = title
                    preview.setOnClickListener {
                        view.context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(match.value)))
                    }
                }
            } catch (e: Exception) {
                view.post { preview.visibility = View.GONE }
            }
        }
    }


    private fun showVoicePlayer(view: View, msg: ChatMessage) {
        val player = view.findViewById<LinearLayout>(R.id.voicePlayer) ?: return
        player.visibility = View.VISIBLE
        
        val playBtn = view.findViewById<ImageView>(R.id.btnPlayVoice)
        val durationText = view.findViewById<TextView>(R.id.voiceDuration)
        var isPlaying = false
        var mediaPlayer: android.media.MediaPlayer? = null
        
        playBtn.setOnClickListener {
            if (isPlaying) {
                mediaPlayer?.pause()
                playBtn.setImageResource(R.drawable.ic_play)
            } else {
                try {
                    val url = msg.file?.url ?: return@setOnClickListener
                    val fullUrl = if (url.startsWith("http")) url else "http://2.26.71.102:8000$url"
                    mediaPlayer = android.media.MediaPlayer().apply {
                        setDataSource(fullUrl)
                        setOnPreparedListener { 
                            it.start()
                            durationText.text = formatDuration(it.duration)
                        }
                        setOnCompletionListener {
                            playBtn.setImageResource(R.drawable.ic_play)
                            isPlaying = false
                        }
                        prepareAsync()
                    }
                    playBtn.setImageResource(R.drawable.ic_pause)
                } catch (e: Exception) {
                    playBtn.setImageResource(R.drawable.ic_play)
                }
            }
            isPlaying = !isPlaying
        }
    }
    
    private fun formatDuration(ms: Int): String {
        val seconds = ms / 1000
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    fun getItems(): List<Any> = items.toList()

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
        val selectCheck: android.widget.CheckBox = view.findViewById(R.id.selectCheck)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
        val reactionsText: TextView = view.findViewById(R.id.reactionsText)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectCheck: android.widget.CheckBox = view.findViewById(R.id.selectCheck)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
        val imageMsg: ImageView = view.findViewById(R.id.imageMsg)
                val reactionsText: TextView = view.findViewById(R.id.reactionsText)
    }
}
