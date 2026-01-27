package com.digitalturbine.promptnews.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeCategoryPagerAdapter(
    fragment: Fragment,
    private val categories: List<HomeCategory>
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        val category = categories[position]
        return HomeCategoryPageFragment.newInstance(category)
    }
}
