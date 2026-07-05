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
    private val appContext: android.content.Context? = null,
    private val onLog: ((String) -> Unit)? = null
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
                onLog?.invoke("VOICE: checking text=${item.text.take(50)}"); if (item.text.contains("🎤 Голосовое")) {
                    val fc = holder.itemView.findViewById<LinearLayout>(R.id.fileIconContainer)
                    if (fc != null) fc.visibility = View.GONE
                    showVoicePlayer(holder.itemView, item)
                    holder.text.visibility = View.GONE
                    holder.text.text = ""
                } else {
                    val sp = android.text.SpannableString(item.text)
        "@(\\w+)".toRegex().findAll(item.text).forEach { m ->
            sp.setSpan(android.text.style.ForegroundColorSpan(0xFFFF5E8E.toInt()), m.range.first, m.range.last + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        holder.text.text = sp
                    holder.text.autoLinkMask = android.text.util.Linkify.WEB_URLS
                    holder.text.movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    holder.itemView.findViewById<LinearLayout>(R.id.ytPreview)?.visibility = View.GONE
                    holder.text.visibility = View.VISIBLE
                    // Скрываем иконку файла для обычных сообщений
                    val fc = holder.itemView.findViewById<LinearLayout>(R.id.fileIconContainer)
                    if (fc != null) fc.visibility = View.GONE
                    if (item.file == null) showLinkPreview(holder.itemView, item.text)
                    val vp = holder.itemView.findViewById<LinearLayout>(R.id.voicePlayer)
                    if (vp != null) vp.visibility = View.GONE
                }
                // Если это файл — делаем кликабельным
                if (item.file != null && !item.text.contains("🎤 Голосовое")) {
                    val name = item.file?.name ?: ""
                    val isImage = name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) || name.endsWith(".gif", true) || name.endsWith(".webp", true) || name.endsWith(".bmp", true)
                    if (!isImage) {
                        showFileIcon(holder.itemView, item)
                    }
                    holder.text.visibility = View.GONE
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
                onLog?.invoke("VOICE: checking text=${item.text.take(50)}"); if (item.text.contains("🎤 Голосовое")) {
                    val fc = holder.itemView.findViewById<LinearLayout>(R.id.fileIconContainer)
                    if (fc != null) fc.visibility = View.GONE
                    showVoicePlayer(holder.itemView, item)
                    holder.text.visibility = View.GONE
                    holder.text.text = ""
                } else {
                    val sp = android.text.SpannableString(item.text)
        "@(\\w+)".toRegex().findAll(item.text).forEach { m ->
            sp.setSpan(android.text.style.ForegroundColorSpan(0xFFFF5E8E.toInt()), m.range.first, m.range.last + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        holder.text.text = sp
                    holder.text.autoLinkMask = android.text.util.Linkify.WEB_URLS
                    holder.text.movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    holder.itemView.findViewById<LinearLayout>(R.id.ytPreview)?.visibility = View.GONE
                    holder.itemView.findViewById<LinearLayout>(R.id.ytPreview)?.visibility = View.GONE
                    holder.text.visibility = View.VISIBLE
                        val vp = holder.itemView.findViewById<LinearLayout>(R.id.voicePlayer)
                    if (vp != null) vp.visibility = View.GONE
                }
                // Если это файл — делаем кликабельным
                if (item.file != null && !item.text.contains("🎤 Голосовое")) {
                    val name = item.file?.name ?: ""
                    val isImage = name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) || name.endsWith(".gif", true) || name.endsWith(".webp", true) || name.endsWith(".bmp", true)
                    if (!isImage) {
                        showFileIcon(holder.itemView, item)
                    }
                    holder.text.visibility = View.GONE
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
        thread {
            try {
                val json = org.json.JSONObject(java.net.URL("https://www.youtube.com/oembed?url=https://youtube.com/watch?v=$videoId&format=json").readText())
                val title = json.optString("title", "YouTube")
                val bytes = java.net.URL("https://img.youtube.com/vi/$videoId/0.jpg").readBytes()
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                view.post {
                    preview.visibility = View.VISIBLE
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


    private fun showLinkPreview(view: View, text: String) {
        val regex = Regex("https?://[\\w\\d./?=&#%:_-]+")
        val url = regex.find(text)?.value ?: return
        val preview = view.findViewById<LinearLayout>(R.id.linkPreview) ?: return
        preview.visibility = View.GONE  // скрываем по умолчанию
        
        thread {
            try {
                val json = org.json.JSONObject(java.net.URL("http://2.26.71.102:8000/api/preview?url=$url&token=preview").readText())
                val title = json.optString("title", "")
                val desc = json.optString("description", "")
                val image = json.optString("image", "")
                
                if (title.isNotEmpty() || image.isNotEmpty()) {
                    view.post {
                        preview.visibility = View.VISIBLE
                        view.findViewById<TextView>(R.id.previewTitle)?.text = title
                        view.findViewById<TextView>(R.id.previewDesc)?.text = desc
                        if (image.isNotEmpty()) {
                            thread {
                                try {
                                    val bytes = java.net.URL(image).readBytes()
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    view.post { view.findViewById<ImageView>(R.id.previewImage)?.setImageBitmap(bmp) }
                                } catch (_: Exception) {}
                            }
                        }
                        preview.setOnClickListener {
                            view.context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }


    private fun showFileIcon(view: View, msg: ChatMessage) {
        val container = view.findViewById<LinearLayout>(R.id.fileIconContainer)
        if (container == null || msg.file == null) return
        
        // Скрываем текст сообщения
        val textView = view.findViewById<TextView>(R.id.text)
        textView?.visibility = View.GONE
        
        container.visibility = View.VISIBLE
        
        val iconBg = view.findViewById<View>(R.id.fileIconBg)
        val iconText = view.findViewById<TextView>(R.id.fileIconText)
        val fileName = view.findViewById<TextView>(R.id.fileNameText)
        val fileSize = view.findViewById<TextView>(R.id.fileSizeText)
        val downloadBtn = view.findViewById<TextView>(R.id.fileDownloadBtn)
        
        val name = msg.file?.name ?: "file"
        val ext = name.substringAfterLast('.').uppercase().take(3).ifEmpty { "?" }
        val size = formatFileSize(msg.file?.size ?: 0)
        
        val bgColor = when (ext) {
            "PDF" -> 0x1FFF3B30.toInt()
            "DOC", "DOCX" -> 0x1F2AABEE.toInt()
            "XLS", "XLSX" -> 0x1F34C759.toInt()
            "ZIP", "RAR", "7Z", "GZ" -> 0x1FFF9500.toInt()
            "JPG", "PNG", "GIF", "BMP", "WEBP" -> 0x1F9C6BFF.toInt()
            "MP3", "WAV", "AAC", "M4A", "OGG" -> 0x1FFF5E8E.toInt()
            "MP4", "AVI", "MOV", "MKV" -> 0x1F00BCD4.toInt()
            else -> 0x1F888888.toInt()
        }
        iconBg.setBackgroundColor(bgColor)
        iconText.text = ext
        fileName.text = name
        fileSize.text = size
        
        // Кнопка скачать
        downloadBtn?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.text = "↓ Скачать"
            btn.setOnClickListener {
                val url = msg.file?.url ?: return@setOnClickListener
                val fullUrl = if (url.startsWith("http")) url else "http://2.26.71.102:8000$url"
                btn.text = "..."
                thread {
                    try {
                        val cached = com.mychat.app.utils.FileCache.getCachedFile(fullUrl)
                        if (cached != null) {
                            btn.post { openFile(view.context, cached); btn.text = "✓ Открыть" }
                        } else {
                            val bytes = java.net.URL(fullUrl).readBytes()
                            val saved = com.mychat.app.utils.FileCache.saveToCache(fullUrl, bytes)
                            saved?.let { btn.post { openFile(view.context, it); btn.text = "✓ Открыть" } }
                        }
                    } catch (e: Exception) {
                        btn.post { btn.text = "↓ Скачать" }
                    }
                }
            }
        }
    }
    
    private fun openFile(context: android.content.Context, file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Нет приложения для открытия", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024*1024))} MB"
            else -> "${"%.1f".format(bytes.toDouble() / (1024*1024*1024))} GB"
        }
    }
    
    
    private fun showVoicePlayer(view: View, msg: ChatMessage) {
        val player = view.findViewById<LinearLayout>(R.id.voicePlayer) ?: return
        player.visibility = View.VISIBLE
        
        val url = msg.file?.url ?: msg.text.removePrefix("🎤 Голосовое ").trim()
        if (url.isEmpty() || url == "🎤 Голосовое") return
        val fullUrl = if (url.startsWith("http")) url else "http://2.26.71.102:8000$url"
        val playBtn = view.findViewById<TextView>(R.id.btnPlayVoice)
        val durationText = view.findViewById<TextView>(R.id.voiceDuration)
        val waveform = view.findViewById<com.mychat.app.views.WaveformView>(R.id.waveformView)
        waveform?.setWaveColor(0x66ffffff.toInt())
        var mediaPlayer: android.media.MediaPlayer? = null
        var isPlaying = false
        playBtn.setOnClickListener {
            if (isPlaying) {
                mediaPlayer?.pause()
                playBtn.text = "▶"
                durationText.visibility = View.GONE
                waveform?.stopAnimation()
                isPlaying = false
            } else {
                try {
                    if (mediaPlayer == null) {
                        mediaPlayer = android.media.MediaPlayer().apply {
                            setDataSource(fullUrl)
                            setOnPreparedListener { mp ->
                                durationText.text = formatDuration(mp.duration)
                                durationText.visibility = View.VISIBLE
                                mp.start()
                                waveform?.startAnimation()
                            }
                            setOnCompletionListener {
                                playBtn.text = "▶"
                                durationText.visibility = View.GONE
                                waveform?.stopAnimation()
                                isPlaying = false
                            }
                            prepareAsync()
                        }
                    } else {
                        mediaPlayer?.start()
                        durationText.visibility = View.VISIBLE
                        waveform?.startAnimation()
                    }
                    playBtn.text = "⏸"
                    isPlaying = true
                } catch (e: Exception) {}
            }
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

    private fun showLocationMap(msg: ChatMessage) {
        val lat = msg.location?.lat ?: return
        val lon = msg.location?.lon ?: return
        
        thread {
            try {
                val mapUrl = "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=15&size=600x300&markers=$lat,$lon,red-pushpin"
                val bmp = android.graphics.BitmapFactory.decodeStream(java.net.URL(mapUrl).openStream())
                mapImage.post { mapImage.setImageBitmap(bmp) }
            } catch (e: Exception) {
                mapImage.post { mapImage.setBackgroundColor(0xff2a2a2a.toInt()) }
            }
        }
        
        mapImage.setOnClickListener {
            val uri = android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            itemView.context.startActivity(intent)
        }
    }

}
