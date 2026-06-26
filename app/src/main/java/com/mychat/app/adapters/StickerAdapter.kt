package com.mychat.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R

class StickerAdapter(
    private val stickers: List<Int>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<StickerAdapter.VH>() {
    
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.stickerImage)
    }
    
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        LayoutInflater.from(p.context).inflate(R.layout.item_sticker, p, false)
    )
    
    override fun onBindViewHolder(h: VH, pos: Int) {
        h.img.setImageResource(stickers[pos])
        h.itemView.setOnClickListener { onClick(stickers[pos]) }
    }
    
    override fun getItemCount() = stickers.size
}
