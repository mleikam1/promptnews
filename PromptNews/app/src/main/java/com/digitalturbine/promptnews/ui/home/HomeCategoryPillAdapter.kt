package com.digitalturbine.promptnews.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.digitalturbine.promptnews.R

class HomeCategoryPillAdapter(
    private val onPillSelected: (Int) -> Unit
) : RecyclerView.Adapter<HomeCategoryPillAdapter.PillViewHolder>() {
    private val items = mutableListOf<HomeCategory>()
    private var selectedIndex: Int = 0

    fun submitList(categories: List<HomeCategory>) {
        items.clear()
        items.addAll(categories)
        selectedIndex = 0
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_category_pill, parent, false) as TextView
        return PillViewHolder(view)
    }

    override fun onBindViewHolder(holder: PillViewHolder, position: Int) {
        holder.bind(items[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    inner class PillViewHolder(private val pill: TextView) : RecyclerView.ViewHolder(pill) {
        fun bind(category: HomeCategory, selected: Boolean) {
            pill.text = category.displayName
            pill.isSelected = selected
            pill.setOnClickListener {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onPillSelected(position)
            }
        }
    }
}
