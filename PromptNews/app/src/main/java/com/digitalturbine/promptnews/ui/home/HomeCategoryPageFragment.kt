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
import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesArticle
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesArticleActivity
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesArticleUi
import com.digitalturbine.promptnews.ui.fotoscapes.toFotoscapesArticleUi
import com.digitalturbine.promptnews.ui.fotoscapes.toFotoscapesUi
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HomeCategoryPageFragment : Fragment(R.layout.fragment_home_category_page) {
    private val homeCategoryRepository = HomeCategoryRepository()
    private val feedAdapter = HomeCategoryAdapter(
        onArticleClick = ::openArticle,
        onFotoscapesClick = ::openFotoscapesArticle,
        onCtaClick = ::openLocalMore
    )
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private lateinit var feedView: RecyclerView
    private lateinit var loadingView: ProgressBar
    private lateinit var errorView: TextView

    private var isLoading: Boolean = false
    private var hasLoaded: Boolean = false
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
                if (
                    key == HomePrefs.KEY_LOCATION ||
                    key == HomePrefs.KEY_LOCATION_CITY ||
                    key == HomePrefs.KEY_LOCATION_STATE
                ) {
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
        if (category.type == HomeCategoryType.INTEREST && !isLoading && !hasLoaded) {
            loadFeed()
        }
    }

    private fun loadFeed() {
        isLoading = true
        hasLoaded = false
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val userLocation = HomePrefs.getUserLocation(requireContext())
            val locationLabel = HomePrefs.getLocation(requireContext())
            val local = if (category.type == HomeCategoryType.HOME) {
                homeCategoryRepository.loadLocalNews(userLocation)
            } else {
                emptyList()
            }
            val feed = if (category.type == HomeCategoryType.INTEREST) {
                val selectedIds = UserInterestRepositoryImpl.getInstance(requireContext())
                    .getSelectedInterests()
                    .map { it.id }
                    .toSet()
                if (selectedIds.contains(category.id)) {
                    homeCategoryRepository.loadFotoscapesInterest(category)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

            if (!isAdded) return@launch

            if (category.type == HomeCategoryType.INTEREST) {
                val first = feed.firstOrNull()
                Log.d("Fotoscapes", "HomeScreen list size=${feed.size}")
                Log.d(
                    "Fotoscapes",
                    "HomeScreen first item lbtype=${first?.lbType} uid=${first?.id}"
                )
            }

            val items = buildItems(
                local = local,
                feed = feed,
                locationLabel = locationLabel
            )

            isLoading = false
            hasLoaded = true
            if (items.isEmpty()) {
                feedAdapter.submitList(emptyList())
                showEmptyState(emptyMessage(locationLabel))
            } else {
                feedAdapter.submitList(items)
                showContent()
            }
        }
    }

    private fun buildItems(
        local: List<Article>,
        feed: List<FotoscapesArticle>,
        locationLabel: String
    ): List<HomeFeedItem> {
        val items = mutableListOf<HomeFeedItem>()

        if (category.type == HomeCategoryType.HOME) {
            if (local.isEmpty()) {
                return emptyList()
            }
            items.add(HomeFeedItem.SectionHeader(localHeader(locationLabel)))
            items.addAll(local.take(LOCAL_COUNT).map { HomeFeedItem.SmallCard(it) })
            items.add(HomeFeedItem.CtaButton(getString(R.string.home_local_more)))
        } else {
            if (feed.isNotEmpty()) {
                items.add(HomeFeedItem.SectionHeader(getString(R.string.home_latest_stories)))
            }
            items.addAll(
                feed.take(FEED_COUNT).map { article ->
                    HomeFeedItem.FotoscapesArticle(article)
                }
            )
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
        val isFotoscapesArticle = article.fotoscapesLbtype.equals("article", ignoreCase = true)
        if (isFotoscapesArticle) {
            val ui = article.toFotoscapesUi()
            if (ui is FotoscapesArticleUi) {
                FotoscapesArticleActivity.start(requireContext(), ui)
            }
            return
        }
        val url = if (article.isFotoscapesStory()) {
            article.fotoscapesLink
        } else {
            article.url
        }
        if (url.isBlank()) return
        openWebView(url)
    }

    private fun openFotoscapesArticle(article: FotoscapesArticle) {
        if (article.lbType.equals("article", ignoreCase = true)) {
            val ui = article.toFotoscapesArticleUi()
            FotoscapesArticleActivity.start(requireContext(), ui)
            return
        }
        val url = article.articleUrl
        if (url.isBlank()) return
        openWebView(url)
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
