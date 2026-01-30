package com.digitalturbine.promptnews.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.util.HomePrefs

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
        val pillSpacing = resources.getDimensionPixelSize(R.dimen.home_category_pill_spacing)
        chipsView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) return
                val itemCount = parent.adapter?.itemCount ?: 0
                outRect.right = if (position == itemCount - 1) 0 else pillSpacing
            }
        })
        pillAdapter.submitList(categories)

        pagerView.adapter = HomeCategoryPagerAdapter(this, categories)
        pagerView.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pillAdapter.setSelectedIndex(position)
                chipsView.smoothScrollToPosition(position)
                val fragment = childFragmentManager.findFragmentByTag("f$position") as? HomeCategoryPageFragment
                fragment?.refreshOnTabSelected()
            }
        })
    }

    private fun buildCategories(interests: Set<Interest>): List<HomeCategory> {
        val categories = mutableListOf<HomeCategory>()
        val userLocation = HomePrefs.getUserLocation(requireContext())
        if (userLocation != null) {
            categories.add(HomeCategory.home())
        }
        interests.forEach { interest ->
            categories.add(HomeCategory.interest(interest))
        }
        return categories
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
