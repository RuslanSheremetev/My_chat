package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mychat.app.R

data class StickerItem(
    val id: String,
    val emoji: String,
    val url: String
)

class StickerAdapter(
    private val stickers: List<StickerItem>,
    private val onClick: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerAdapter.VH>() {
    
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.stickerImage)
    }
    
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        LayoutInflater.from(p.context).inflate(R.layout.item_sticker, p, false)
    )
    
    override fun onBindViewHolder(h: VH, pos: Int) {
        val sticker = stickers[pos]
        Glide.with(h.img.context)
            .load(sticker.url)
            .placeholder(R.drawable.sticker1)
            .into(h.img)
        h.itemView.setOnClickListener { onClick(sticker) }
    }
    
    override fun getItemCount() = stickers.size
}
