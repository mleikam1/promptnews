package com.digitalturbine.promptnews.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var chipsView: androidx.recyclerview.widget.RecyclerView
    private lateinit var pagerView: ViewPager2
    private val pillAdapter = HomeCategoryPillAdapter { index ->
        pagerView.currentItem = index
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chipsView = view.findViewById(R.id.home_category_chips)
        pagerView = view.findViewById(R.id.home_category_pager)

        val interests = UserInterestRepositoryImpl.getInstance(requireContext()).getSelectedInterests()
        val categories = buildCategories(interests)

        chipsView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        chipsView.adapter = pillAdapter
        pillAdapter.submitList(categories)

        pagerView.adapter = HomeCategoryPagerAdapter(this, categories)
        pagerView.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pillAdapter.setSelectedIndex(position)
                chipsView.smoothScrollToPosition(position)
            }
        })
    }

    private fun buildCategories(interests: List<Interest>): List<HomeCategory> {
        val categories = mutableListOf<HomeCategory>()
        categories.add(HomeCategory.home())
        interests.forEach { interest ->
            categories.add(HomeCategory.interest(interest))
        }
        return categories
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
