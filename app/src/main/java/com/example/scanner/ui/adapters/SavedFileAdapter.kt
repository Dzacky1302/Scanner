package com.example.scanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.R
import com.example.scanner.databinding.ItemSavedFileBinding
import com.example.scanner.model.ScannedDocument

class SavedFileAdapter(
    private val files: MutableList<ScannedDocument>,
    private val onItemClick: (ScannedDocument) -> Unit,
    private val onShareClick: (ScannedDocument) -> Unit,
    private val onDeleteClick: (ScannedDocument, Int) -> Unit
) : RecyclerView.Adapter<SavedFileAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSavedFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = files[position]
        holder.binding.tvFileName.text = doc.name
        holder.binding.tvFileDetails.text = "${doc.sizeFormatted} • ${doc.dateModifiedFormatted}"

        when (doc.extension) {
            "pdf" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_edit)
            "docx" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_edit)
            "xlsx" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_view)
            "pptx" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_slideshow)
            "zip" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_save)
            "jpg", "jpeg", "png" -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_gallery)
            else -> holder.binding.imgFileType.setImageResource(android.R.drawable.ic_menu_save)
        }

        holder.itemView.setOnClickListener {
            onItemClick(doc)
        }

        holder.binding.btnShare.setOnClickListener {
            onShareClick(doc)
        }

        holder.binding.btnDelete.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onDeleteClick(doc, currentPos)
            }
        }
    }

    override fun getItemCount(): Int = files.size
}
