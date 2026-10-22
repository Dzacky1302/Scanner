package com.example.scanner.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.databinding.ItemSelectedImageBinding

class SelectedImageAdapter(
    private val images: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectedImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = images[position]
        holder.binding.imgPreview.setImageURI(uri)
        holder.binding.tvIndex.text = (position + 1).toString()
        holder.binding.btnRemove.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onRemoveClick(currentPos)
            }
        }
    }

    override fun getItemCount(): Int = images.size
}
