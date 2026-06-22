package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.models.ChatMessage
import com.mychat.app.models.FileInfo
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val me: String,
    private val onDownload: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    fun update(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when {
            item is String -> 0
            item is ChatMessage -> {
                if (item.from == me) 2 else 1
            }
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date, parent, false)
                DateViewHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_msg_in, parent, false)
                InViewHolder(view)
            }
            2 -> {
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
        val item = items[position]
        when {
            item is String && holder is DateViewHolder -> {
                holder.dateText.text = item
            }
            item is ChatMessage && holder is InViewHolder -> {
                holder.from.text = item.from
                holder.text.text = item.text
                holder.time.text = item.time.takeLast(5)
            }
            item is ChatMessage && holder is OutViewHolder -> {
                holder.text.text = item.text
                holder.time.text = item.time.takeLast(5)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
    }

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
