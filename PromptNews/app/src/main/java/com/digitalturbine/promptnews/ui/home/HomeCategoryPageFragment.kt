package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.HomeCategoryRepository
import com.digitalturbine.promptnews.data.isFotoscapesStory
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HomeCategoryPageFragment : Fragment(R.layout.fragment_home_category_page) {
    private val homeCategoryRepository = HomeCategoryRepository()
    private val feedAdapter = HomeCategoryAdapter(
        onArticleClick = ::openArticle,
        onCtaClick = ::openLocalMore
    )
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private lateinit var feedView: RecyclerView
    private lateinit var loadingView: ProgressBar
    private lateinit var errorView: TextView

    private var isLoading: Boolean = false

    private lateinit var category: HomeCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = HomeCategory(
            id = requireArguments().getString(ARG_ID).orEmpty(),
            displayName = requireArguments().getString(ARG_NAME).orEmpty(),
            type = HomeCategoryType.valueOf(requireArguments().getString(ARG_TYPE).orEmpty()),
            endpoint = requireArguments().getString(ARG_ENDPOINT).orEmpty()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedView = view.findViewById(R.id.home_category_feed)
        loadingView = view.findViewById(R.id.home_category_loading)
        errorView = view.findViewById(R.id.home_category_error)

        feedView.layoutManager = LinearLayoutManager(requireContext())
        feedView.adapter = feedAdapter

        loadFeed()
    }

    override fun onStart() {
        super.onStart()
        if (category.type == HomeCategoryType.HOME) {
            val prefs = HomePrefs.getPrefs(requireContext())
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == HomePrefs.KEY_LOCATION) {
                    loadFeed()
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            prefsListener = listener
        }
    }

    override fun onStop() {
        super.onStop()
        if (category.type == HomeCategoryType.HOME) {
            val prefs = HomePrefs.getPrefs(requireContext())
            prefsListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
            prefsListener = null
        }
    }

    fun refreshOnTabSelected() {
        if (category.type == HomeCategoryType.INTEREST && !isLoading) {
            loadFeed()
        }
    }

    private fun loadFeed() {
        isLoading = true
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val locationLabel = HomePrefs.getLocation(requireContext())
            val content = homeCategoryRepository.fetchCategory(category, locationLabel)

            if (!isAdded) return@launch

            if (category.type == HomeCategoryType.INTEREST) {
                val first = content.feed.firstOrNull()
                Log.d("Fotoscapes", "HomeScreen list size=${content.feed.size}")
                Log.d(
                    "Fotoscapes",
                    "HomeScreen first item lbtype=${first?.fotoscapesLbtype} uid=${first?.fotoscapesUid}"
                )
            }

            val items = buildItems(
                local = content.local,
                feed = content.feed,
                locationLabel = locationLabel
            )

            val isEmpty = when (category.type) {
                HomeCategoryType.HOME -> content.local.isEmpty()
                HomeCategoryType.INTEREST -> content.feed.isEmpty()
            }

            isLoading = false
            when {
                isLoading -> showLoading()
                isEmpty -> {
                    feedAdapter.submitList(emptyList())
                    showEmptyState(emptyMessage(locationLabel))
                }
                else -> {
                    feedAdapter.submitList(items)
                    showContent()
                }
            }
        }
    }

    private fun buildItems(
        local: List<Article>,
        feed: List<Article>,
        locationLabel: String
    ): List<HomeFeedItem> {
        val items = mutableListOf<HomeFeedItem>()

        if (category.type == HomeCategoryType.HOME) {
            items.add(HomeFeedItem.SectionHeader(localHeader(locationLabel)))
            if (local.isNotEmpty()) {
                items.addAll(local.take(LOCAL_COUNT).map { HomeFeedItem.SmallCard(it) })
            }
            items.add(HomeFeedItem.CtaButton(getString(R.string.home_local_more)))
        } else {
            if (feed.isNotEmpty()) {
                items.add(HomeFeedItem.SectionHeader(getString(R.string.home_latest_stories)))
                items.addAll(feed.take(FEED_COUNT).map { HomeFeedItem.FeedCard(it) })
            }
        }
        return items
    }

    private fun localHeader(locationLabel: String): String {
        return if (locationLabel.isBlank()) {
            getString(R.string.home_local_near_you)
        } else {
            getString(R.string.home_local_near_city, locationLabel)
        }
    }

    private fun showLoading() {
        loadingView.isVisible = true
        errorView.isVisible = false
        feedView.isVisible = false
    }

    private fun showEmptyState(message: String) {
        errorView.text = message
        errorView.isVisible = true
        loadingView.isVisible = false
        feedView.isVisible = false
    }

    private fun showContent() {
        feedView.isVisible = true
        loadingView.isVisible = false
        errorView.isVisible = false
    }

    private fun emptyMessage(locationLabel: String): String {
        return if (category.type == HomeCategoryType.HOME) {
            if (locationLabel.isBlank()) {
                getString(R.string.home_local_requires_location)
            } else {
                getString(R.string.home_local_empty)
            }
        } else {
            getString(R.string.home_interest_empty)
        }
    }

    private fun openArticle(article: Article) {
        if (article.isFotoscapesStory()) {
            Log.d(
                "Fotoscapes",
                "Click uid=${article.fotoscapesUid} lbtype=${article.fotoscapesLbtype} " +
                    "link=${article.url} sourceLink=${article.fotoscapesSourceLink}"
            )
        }
        if (article.url.isBlank()) return
        openWebView(article.url)
    }

    private fun openWebView(url: String) {
        val ctx = requireContext()
        ctx.startActivity(
            Intent(ctx, ArticleWebViewActivity::class.java)
                .putExtra("url", url)
        )
    }

    private fun openLocalMore() {
        val locationLabel = HomePrefs.getLocation(requireContext())
        val query = if (locationLabel.isBlank()) "local news" else "local news $locationLabel"
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://news.google.com/search?q=$encoded"
        val ctx = requireContext()
        ctx.startActivity(
            Intent(ctx, ArticleWebViewActivity::class.java)
                .putExtra("url", url)
        )
    }

    companion object {
        private const val ARG_ID = "arg_category_id"
        private const val ARG_NAME = "arg_category_name"
        private const val ARG_TYPE = "arg_category_type"
        private const val ARG_ENDPOINT = "arg_category_endpoint"

        private const val LOCAL_COUNT = 5
        private const val FEED_COUNT = 10

        fun newInstance(category: HomeCategory): HomeCategoryPageFragment {
            return HomeCategoryPageFragment().apply {
                arguments = bundleOf(
                    ARG_ID to category.id,
                    ARG_NAME to category.displayName,
                    ARG_TYPE to category.type.name,
                    ARG_ENDPOINT to category.endpoint
                )
            }
        }
    }
}
