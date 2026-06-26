package com.mychat.app.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.User

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

    private val users = mutableListOf<User>()

    fun update(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }
    
    fun removeItem(position: Int): User {
        val user = users.removeAt(position)
        notifyItemRemoved(position)
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
    }

    fun getUsers(): List<User> = users.toList()
    
    override fun getItemCount(): Int = users.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: TextView = itemView.findViewById(R.id.avatar)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val lastTime: TextView = itemView.findViewById(R.id.lastTime)
        private val onlineDot: View = itemView.findViewById(R.id.onlineDot)
        private val msgStatusIcon: ImageView = itemView.findViewById(R.id.msgStatusIcon)

        fun bind(user: User) {
            val displayName = if (user.name.isNotEmpty()) user.name else user.username
            avatar.text = displayName.take(1).uppercase()
            
            val colors = arrayOf("#2AABEE", "#34C759", "#FF9500", "#FF3B30", "#9C6BFF", "#FF6B9D", "#00BCD4", "#FF5722")
            val colorIndex = user.username.hashCode().mod(colors.size)
            val color = colors[if (colorIndex < 0) colorIndex * -1 else colorIndex]
            avatar.background = circleBg(color)
            
            name.text = displayName
            
            val lastMsg = user.lastMsg
            if (lastMsg.isNotEmpty()) {
                lastMessage.text = lastMsg
                val iconRes = when {
                    user.lastMsgType == "photo" -> R.drawable.ic_msg_photo
                    user.lastMsgType == "file" -> R.drawable.ic_msg_file
                    user.lastMsgType == "voice" -> R.drawable.ic_msg_audio
                    else -> 0
                }
                if (iconRes != 0) {
                    lastMessage.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
                    lastMessage.compoundDrawablePadding = 6
                } else {
                    lastMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            } else {
                lastMessage.text = user.bio
            }
            
            lastTime.text = user.lastTime
            // Статус последнего сообщения
            if (user.lastMsgStatus == "sending") {
                msgStatusIcon.visibility = View.VISIBLE
                msgStatusIcon.setImageResource(R.drawable.ic_check_sending)
                msgStatusIcon.setColorFilter(0xff8e8e93.toInt())
            } else if (user.lastMsgStatus == "sent") {
                msgStatusIcon.visibility = View.VISIBLE
                msgStatusIcon.setImageResource(R.drawable.ic_check_sent)
                msgStatusIcon.setColorFilter(0xff8e8e93.toInt())
            } else if (user.lastMsgStatus == "delivered") {
                msgStatusIcon.visibility = View.VISIBLE
                msgStatusIcon.setImageResource(R.drawable.ic_check_delivered)
                msgStatusIcon.setColorFilter(0xff8e8e93.toInt())
            } else if (user.lastMsgStatus == "read") {
                msgStatusIcon.visibility = View.VISIBLE
                msgStatusIcon.setImageResource(R.drawable.ic_check_read)
                msgStatusIcon.setColorFilter(0xff4fc3f7.toInt())
            } else {
                msgStatusIcon.visibility = View.GONE
            }
            
            // Показываем зеленую точку, если пользователь онлайн
            onlineDot.visibility = if (user.online) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener { 
            itemView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                itemView.context, R.anim.item_click_scale
            ))
            itemView.postDelayed({
                itemView.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                    itemView.context, R.anim.item_click_release
                ))
            }, 150)
            onClick(user) 
        }
        }
    }
}
