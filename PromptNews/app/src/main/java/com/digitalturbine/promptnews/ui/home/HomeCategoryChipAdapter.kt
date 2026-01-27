package com.digitalturbine.promptnews.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.digitalturbine.promptnews.R
import com.google.android.material.chip.Chip

class HomeCategoryChipAdapter(
    private val onChipSelected: (Int) -> Unit
) : RecyclerView.Adapter<HomeCategoryChipAdapter.ChipViewHolder>() {
    private val items = mutableListOf<HomeCategory>()
    private var selectedIndex: Int = 0

    fun submitList(categories: List<HomeCategory>) {
        items.clear()
        items.addAll(categories)
        notifyDataSetChanged()
    }

    fun setSelectedIndex(index: Int) {
        val previous = selectedIndex
        selectedIndex = index
        if (previous != index) {
            notifyItemChanged(previous)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_category_chip, parent, false) as Chip
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(items[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    inner class ChipViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(category: HomeCategory, selected: Boolean) {
            chip.text = category.displayName
            chip.isChecked = selected
            chip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChipSelected(position)
                }
            }
        }
    }
}
