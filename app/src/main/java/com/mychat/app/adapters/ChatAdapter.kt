package com.mychat.app.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.User
import com.mychat.app.views.WaveformView
import android.media.MediaPlayer
import com.mychat.app.utils.FileCache
import kotlin.concurrent.thread

fun circleBg(color: String): GradientDrawable {
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(Color.parseColor(color))
    return d
}

class ChatAdapter(
    private val onClick: (User) -> Unit,
    private val onLongClick: ((User) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private var mediaPlayer: MediaPlayer? = null
    private val users = mutableListOf<User>()
    var selectedPosition: Int = -1
    

    class UserDiffCallback(
        private val oldList: List<User>,
        private val newList: List<User>
    ) : androidx.recyclerview.widget.DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].username == newList[newItemPosition].username
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.lastTime == new.lastTime &&
                   old.lastTime == new.lastTime &&
                   old.unread == new.unread &&
                   old.online == new.online &&
                   old.isBot == new.isBot &&
                   old.isGroup == new.isGroup &&
                   old.isFeed == new.isFeed
        }
    }

    fun update(list: List<User>) {
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(UserDiffCallback(users, list))
        users.clear()
        users.addAll(list)
        selectedPosition = -1
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun removeItem(position: Int): User {
        val user = users.removeAt(position)
        notifyItemRemoved(position)
        selectedPosition = -1
        return user
    }
    
    fun getItem(position: Int): User = users[position]
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
        holder.itemView.isSelected = (position == selectedPosition)
    }
    
    fun getUsers(): List<User> = users.toList()
    
    fun stopVoice() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
    override fun getItemCount(): Int = users.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: TextView = itemView.findViewById(R.id.avatar)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val lastTime: TextView = itemView.findViewById(R.id.lastTime)
        private val onlineDot: View = itemView.findViewById(R.id.onlineDot)
        private val msgStatusIcon: ImageView = itemView.findViewById(R.id.msgStatusIcon)
        private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
        private val muteIcon: ImageView = itemView.findViewById(R.id.muteIcon)
        private val voicePreview: LinearLayout = itemView.findViewById(R.id.voicePreview)
        private val waveformView: WaveformView = itemView.findViewById(R.id.waveformView)
        private val voiceDuration: TextView = itemView.findViewById(R.id.voiceDuration)
        private val voicePlayIcon: TextView = itemView.findViewById(R.id.voicePlayIcon)
        private val badge: TextView = itemView.findViewById(R.id.badge)
        private val filePreview: LinearLayout = itemView.findViewById(R.id.filePreview)
        private val chatFileIconBg: View = itemView.findViewById(R.id.chatFileIconBg)
        private val chatFileIconText: TextView = itemView.findViewById(R.id.chatFileIconText)
        private val chatFileName: TextView = itemView.findViewById(R.id.chatFileName)
        
        fun bind(user: User) {
            val displayName = if (user.name.isNotEmpty()) user.name else user.username
            avatar.text = displayName.take(1).uppercase()
            val colors = arrayOf("#2AABEE", "#34C759", "#FF9500", "#FF3B30", "#9C6BFF", "#FF6B9D", "#00BCD4", "#FF5722")
            val colorIndex = user.username.hashCode().mod(colors.size)
            val color = colors[if (colorIndex < 0) colorIndex * -1 else colorIndex]
            avatar.background = circleBg(color)
            name.text = displayName
            
            // Бейдж (Группа / Лента / Bot)
            android.util.Log.d("BADGE", "user=" + user.username + " bot=" + user.isBot + " group=" + user.isGroup + " feed=" + user.isFeed)
            when {
                user.isBot -> {
                    badge.visibility = View.VISIBLE
                    badge.text = "bot"
                    badge.setTextColor(0xff34c759.toInt())
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 20f * itemView.resources.displayMetrics.density
                        setColor(0x2E34c759.toInt())
                    }
                }
                user.isGroup -> {
                    badge.visibility = View.VISIBLE
                    badge.text = "группа"
                    badge.setTextColor(0xffff5e8e.toInt())
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 20f * itemView.resources.displayMetrics.density
                        setColor(0x2Eff5e8e.toInt())
                    }
                }
                user.isFeed -> {
                    badge.visibility = View.VISIBLE
                    badge.text = "лента"
                    badge.setTextColor(0xff3ca0ff.toInt())
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 20f * itemView.resources.displayMetrics.density
                        setColor(0x2E3ca0ff.toInt())
                    }
                }
                user.username == "MyChat" -> {
                    badge.visibility = View.VISIBLE
                    badge.text = "системный"
                    badge.setTextColor(0xff8e44ad.toInt())
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0x228e44ad.toInt())
                        cornerRadius = 12f * itemView.context.resources.displayMetrics.density
                    }
                }
                user.username == "MyChat" -> {
                    badge.visibility = View.VISIBLE
                    badge.text = "системный"
                    badge.setTextColor(0xff8e44ad.toInt())
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0x228e44ad.toInt())
                        cornerRadius = 12f * itemView.context.resources.displayMetrics.density
                    }
                }
                else -> { badge.visibility = View.GONE }
            }
            
            // Проверяем тип файла
            val fileExt = user.lastMsg.let { msg ->
                when {
                    msg.contains(".pdf", true) -> "PDF"
                    msg.contains(".doc", true) -> "DOC"
                    msg.contains(".xls", true) -> "XLS"
                    msg.contains(".zip", true) || msg.contains(".rar", true) -> "ZIP"
                    msg.contains(".jpg", true) || msg.contains(".png", true) || msg.contains(".gif", true) -> "IMG"
                    msg.contains(".mp3", true) || msg.contains(".wav", true) -> "MP3"
                    msg.contains(".mp4", true) || msg.contains(".avi", true) -> "VID"
                    else -> null
                }
            }
            
            if (user.lastMsgType == "photo" || user.lastMsgType == "image" || user.lastMsg.contains("📷") || user.lastMsg.contains("Фото")) {
                voicePreview.visibility = View.GONE
                filePreview.visibility = View.GONE
                lastMessage.visibility = View.VISIBLE
                lastMessage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_photo_preview, 0, 0, 0)
                lastMessage.compoundDrawablePadding = 6
                lastMessage.text = "Фото"
            } else if (user.lastMsgType == "file" && fileExt != null) {
                voicePreview.visibility = View.GONE
                filePreview.visibility = View.VISIBLE
                lastMessage.visibility = View.GONE
                
                val bgColor = when (fileExt) {
                    "PDF" -> 0x1FFF3B30.toInt()
                    "DOC" -> 0x1F2AABEE.toInt()
                    "XLS" -> 0x1F34C759.toInt()
                    "ZIP" -> 0x1FFF9500.toInt()
                    "IMG" -> 0x1F9C6BFF.toInt()
                    "MP3" -> 0x1FFF5E8E.toInt()
                    "VID" -> 0x1F00BCD4.toInt()
                    else -> 0x1F888888.toInt()
                }
                chatFileIconBg.setBackgroundColor(bgColor)
                chatFileIconText.text = fileExt
                chatFileName.text = user.lastMsg.removePrefix("Файл: ").trim()
            } else if (user.lastMsgType == "voice") {
                voicePreview.visibility = View.VISIBLE
                lastMessage.visibility = View.GONE
                val mins = user.lastMsgDuration / 60
                val secs = user.lastMsgDuration % 60
                voiceDuration.visibility = View.GONE
                voicePlayIcon.text = "▶"
                voicePlayIcon.isClickable = true
                voicePlayIcon.isFocusable = true
                voicePlayIcon.setOnClickListener { v ->
                    v.postDelayed({
                        playVoice(voicePlayIcon, user.lastFileUrl, waveformView)
                    }, 100)
                }
                waveformView.stopAnimation()
            } else {
                voicePreview.visibility = View.GONE
                filePreview.visibility = View.GONE
                lastMessage.visibility = View.VISIBLE
                val lastMsg = user.lastMsg
                if (lastMsg.isNotEmpty()) {
                    // Проверяем reply
            val replyPreview = itemView.findViewById<LinearLayout>(R.id.replyPreview)
            val replyText = itemView.findViewById<TextView>(R.id.replyText)
            if (lastMsg.startsWith("↩") || lastMsg.startsWith("↪")) {
                replyPreview.visibility = View.VISIBLE
                replyText.text = lastMsg.replace("↩", "").replace("↪", "").trim()
                lastMessage.visibility = View.GONE
            } else {
                replyPreview.visibility = View.GONE
                lastMessage.visibility = View.VISIBLE
                lastMessage.text = lastMsg
            }
                    when {
                        user.lastMsgType == "call" && lastMsg.contains("Пропущенный") -> 
                            lastMessage.setTextColor(0xffff453a.toInt())
                        user.lastMsgType == "call" -> 
                            lastMessage.setTextColor(0xff8e8e93.toInt())
                        else -> 
                            lastMessage.setTextColor(0xff8e8e93.toInt())
                    }
                    lastMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    lastMessage.text = user.bio
                    lastMessage.setTextColor(0xff8e8e93.toInt())
                }
            }
            
            lastTime.visibility = View.VISIBLE
            lastTime.text = user.lastTime
            // Unread badge
            if (user.unread > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (user.unread > 99) "99+" else user.unread.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }
            
            when (user.lastMsgStatus) {
                "read" -> { msgStatusIcon.visibility = View.VISIBLE; msgStatusIcon.setImageResource(R.drawable.ic_check_read); msgStatusIcon.setColorFilter(0xff34c759.toInt()) }
                "sent", "delivered" -> { msgStatusIcon.visibility = View.VISIBLE; msgStatusIcon.setImageResource(R.drawable.ic_check_sent); msgStatusIcon.setColorFilter(0xff8e8e93.toInt()) }
                else -> { if (user.lastMsg.isNotEmpty()) { msgStatusIcon.visibility = View.VISIBLE; msgStatusIcon.setImageResource(R.drawable.ic_check_sent); msgStatusIcon.setColorFilter(0xff8e8e93.toInt()) } else { msgStatusIcon.visibility = View.GONE } }
            }
            
            onlineDot.visibility = if (user.online && !user.isGroup && !user.isFeed && !user.isBot) View.VISIBLE else View.GONE
        
            muteIcon.visibility = if (user.isMuted) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                itemView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(itemView.context, R.anim.item_click_scale))
                itemView.postDelayed({
                    itemView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(itemView.context, R.anim.item_click_release))
                }, 150)
                onClick(user)
            }
            
            itemView.setOnLongClickListener {
                selectedPosition = adapterPosition
                notifyDataSetChanged()
                onLongClick?.invoke(user)
                true
            }
        }
    }


    private fun playVoice(playIcon: View, url: String, waveform: WaveformView) {
        if (url.isEmpty()) return
        val fullUrl = if (url.startsWith("http")) url else "http://2.26.71.102:8000$url"
        
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            (playIcon as? TextView)?.text = "▶"
            waveform.stopAnimation()
            return
        }
        
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(fullUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    (playIcon as? TextView)?.post { waveform.startAnimation() }
                }
                setOnCompletionListener {
                    playIcon.post { (playIcon as? TextView)?.text = "▶" }
                    waveform.stopAnimation()
                }
                prepareAsync()
            }
            (playIcon as? TextView)?.text = "⏸"
        } catch (e: Exception) {
            e.printStackTrace()
            (playIcon as? TextView)?.text = "▶"
        }
    }
}