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

fun circleBg(color: String): GradientDrawable {
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(Color.parseColor(color))
    return d
}

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
        private val preview: TextView = itemView.findViewById(R.id.preview)

        fun bind(user: User) {
            avatar.text = user.username.take(1).uppercase()
            avatar.background = circleBg(user.avatarColor)
            name.text = user.name.ifEmpty { user.username }
            preview.text = user.bio
            itemView.setOnClickListener { onClick(user) }
        }
    }
}
