package com.mychat.app.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.User

class ChatAdapter(
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val users = mutableListOf<User>()

    fun update(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: TextView = itemView.findViewById(R.id.avatar)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val lastTime: TextView = itemView.findViewById(R.id.lastTime)

        fun bind(user: User) {
            val displayName = if (user.name.isNotEmpty()) user.name else user.username
            avatar.text = displayName.take(1).uppercase()
            
            val colors = arrayOf("#2AABEE", "#34C759", "#FF9500", "#FF3B30", "#9C6BFF", "#FF6B9D", "#00BCD4", "#FF5722")
            val colorIndex = user.username.hashCode().mod(colors.size)
            val color = colors[if (colorIndex < 0) colorIndex * -1 else colorIndex]
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.parseColor(color))
            avatar.background = bg
            
            name.text = displayName
            
            val lastMsg = user.lastMsg
            if (lastMsg.isNotEmpty()) {
                val icon = when {
                    user.lastMsgType == "file" -> "📎 "
                    user.lastMsgType == "photo" -> "🖼️ "
                    user.lastMsgType == "voice" -> "🎤 "
                    else -> ""
                }
                lastMessage.text = icon + lastMsg
            } else {
                lastMessage.text = user.bio
            }
            
            lastTime.text = user.lastTime
            
            itemView.setOnClickListener { onClick(user) }
        }
    }
}
