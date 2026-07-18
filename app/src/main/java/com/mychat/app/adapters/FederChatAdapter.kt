package com.mychat.app.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.User
import com.mychat.app.utils.formatTime

class FederChatAdapter(
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<FederChatAdapter.VH>() {
    
    private var users = listOf<User>()
    
    fun update(list: List<User>) { users = list; notifyDataSetChanged() }
    
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.avatar)
        val name: TextView = v.findViewById(R.id.name)
        val lastMessage: TextView = v.findViewById(R.id.lastMessage)
        val lastTime: TextView = v.findViewById(R.id.lastTime)
        val unreadBadge: TextView = v.findViewById(R.id.unreadBadge)
        val onlineDot: View = v.findViewById(R.id.onlineDot)
        val badge: TextView = v.findViewById(R.id.badge)
        val muteIcon: ImageView = v.findViewById(R.id.muteIcon)
        val check: ImageView = v.findViewById(R.id.check)
    }
    
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        return VH(LayoutInflater.from(p.context).inflate(R.layout.item_chat_feder, p, false))
    }
    
    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = users[pos]
        val name = u.name.ifEmpty { u.username }
        h.avatar.text = name.take(1).uppercase()
        h.avatar.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(android.graphics.Color.parseColor(u.avatarColor)) }
        h.name.text = name
        h.lastMessage.text = u.lastMsg.ifEmpty { u.bio }
        h.lastTime.text = formatTime(u.lastTime)
        h.onlineDot.visibility = if (u.online) View.VISIBLE else View.GONE
        h.badge.visibility = if (u.isBot || u.isGroup || u.isFeed) View.VISIBLE else View.GONE
        h.badge.text = when { u.isBot -> "BOT"; u.isGroup -> "GROUP"; else -> "FEED" }
        h.muteIcon.visibility = if (u.isMuted) View.VISIBLE else View.GONE
        h.unreadBadge.visibility = if (u.unread > 0) View.VISIBLE else View.GONE
        h.unreadBadge.text = if (u.unread > 99) "99+" else u.unread.toString()
        h.check.visibility = if (u.lastMsgFromMe) View.VISIBLE else View.GONE
        h.itemView.setOnClickListener { onClick(u) }
    }
    
    override fun getItemCount() = users.size
}
