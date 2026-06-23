package com.mychat.app.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.ChatMessage
import com.mychat.app.models.FileInfo

class MessageAdapter(
    private val me: String,
    private val onDownload: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_IN = 0
        private const val TYPE_OUT = 1
        private const val TYPE_IN_FILE = 2
        private const val TYPE_OUT_FILE = 3
    }

    fun update(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
        
        // Логируем в Logcat
        Log.d("MessageAdapter", "=== me: $me ===")
        for (msg in list) {
            Log.d("MessageAdapter", "msg.from: ${msg.from}, me: $me, isMe: ${msg.from == me}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        // Показываем в логах
        Log.d("MessageAdapter", "getItemViewType: from=${msg.from}, me=$me, isMe=${msg.from == me}")
        return when {
            msg.from == me -> {
                if (msg.file != null) TYPE_OUT_FILE else TYPE_OUT
            }
            else -> {
                if (msg.file != null) TYPE_IN_FILE else TYPE_IN
            }
        }
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
            TYPE_IN_FILE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_file_in, parent, false)
                InFileViewHolder(view)
            }
            TYPE_OUT_FILE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_file_out, parent, false)
                OutFileViewHolder(view)
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
        when (holder) {
            is InViewHolder -> {
                holder.from.text = msg.from
                holder.text.text = msg.text
                holder.time.text = msg.time.takeLast(5)
                // Отладочная информация
                holder.from.text = "${msg.from} (${if (msg.from == me) "я" else "он"})"
            }
            is OutViewHolder -> {
                holder.text.text = msg.text
                holder.time.text = msg.time.takeLast(5)
                // Отладочная информация
                holder.text.text = "[я] ${msg.text}"
            }
            is InFileViewHolder -> {
                holder.from.text = msg.from
                holder.fileName.text = msg.file?.name ?: "Файл"
                holder.fileSize.text = formatSize(msg.file?.size ?: 0)
                holder.time.text = msg.time.takeLast(5)
                holder.itemView.setOnClickListener {
                    msg.file?.let { onDownload(it.url, it.name) }
                }
            }
            is OutFileViewHolder -> {
                holder.fileName.text = msg.file?.name ?: "Файл"
                holder.fileSize.text = formatSize(msg.file?.size ?: 0)
                holder.time.text = msg.time.takeLast(5)
                holder.itemView.setOnClickListener {
                    msg.file?.let { onDownload(it.url, it.name) }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    // ViewHolders
    class InViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val from: TextView = view.findViewById(R.id.from)
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
    }

    class OutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text)
        val time: TextView = view.findViewById(R.id.time)
    }

    class InFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val from: TextView = view.findViewById(R.id.from)
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileSize: TextView = view.findViewById(R.id.fileSize)
        val time: TextView = view.findViewById(R.id.time)
    }

    class OutFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileSize: TextView = view.findViewById(R.id.fileSize)
        val time: TextView = view.findViewById(R.id.time)
    }
}
