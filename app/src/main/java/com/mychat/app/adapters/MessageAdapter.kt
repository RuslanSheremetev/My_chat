package com.mychat.app.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.ChatMessage

class MessageAdapter(
    private val me: String,
    private val onDownload: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()
    private var debugMe = me

    companion object {
        private const val TYPE_IN = 0
        private const val TYPE_OUT = 1
    }

    fun update(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        debugMe = me
        Log.d("MessageAdapter", "=== UPDATE: me = '$debugMe' ===")
        for (msg in list) {
            Log.d("MessageAdapter", "msg.from = '${msg.from}', equals = ${msg.from == debugMe}")
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        val isMe = msg.from == debugMe
        Log.d("MessageAdapter", "getItemViewType: from='${msg.from}', me='$debugMe', isMe=$isMe")
        return if (isMe) TYPE_OUT else TYPE_IN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
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
        val msg = items[position]
        val isMe = msg.from == debugMe
        
        when (holder) {
            is InViewHolder -> {
                holder.from.text = "📩 from: '${msg.from}' | me: '$debugMe' | isMe: $isMe"
                holder.text.text = msg.text
                holder.time.text = msg.time.takeLast(5)
            }
            is OutViewHolder -> {
                holder.text.text = "📤 from: '${msg.from}' | me: '$debugMe' | isMe: $isMe\n${msg.text}"
                holder.time.text = msg.time.takeLast(5)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class InViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val from: TextView = view.findViewById(R.id.from)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
    }
}
