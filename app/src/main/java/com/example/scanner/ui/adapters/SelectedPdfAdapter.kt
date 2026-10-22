package com.example.scanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.databinding.ItemSelectedPdfBinding
import com.example.scanner.model.SelectedPdfItem
import com.example.scanner.utils.FileUtils

class SelectedPdfAdapter(
    private val pdfItems: MutableList<SelectedPdfItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedPdfAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectedPdfBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedPdfBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pdfItems[position]
        holder.binding.tvPdfName.text = item.name
        holder.binding.tvPdfSize.text = FileUtils.formatFileSize(item.size)
        holder.binding.btnRemovePdf.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onRemoveClick(currentPos)
            }
        }
    }

    override fun getItemCount(): Int = pdfItems.size
}
