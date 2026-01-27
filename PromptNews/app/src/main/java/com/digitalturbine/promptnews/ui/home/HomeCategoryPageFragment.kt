package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.ceil
import kotlin.math.max

class HomeCategoryPageFragment : Fragment(R.layout.fragment_home_category_page) {
    private val searchRepository = SearchRepository()
    private val feedAdapter = HomeCategoryAdapter(
        onArticleClick = ::openArticle,
        onCtaClick = ::openLocalMore
    )
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private lateinit var feedView: RecyclerView
    private lateinit var loadingView: ProgressBar
    private lateinit var errorView: TextView

    private lateinit var category: HomeCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = HomeCategory(
            id = requireArguments().getString(ARG_ID).orEmpty(),
            displayName = requireArguments().getString(ARG_NAME).orEmpty(),
            type = HomeCategoryType.valueOf(requireArguments().getString(ARG_TYPE).orEmpty()),
            query = requireArguments().getString(ARG_QUERY).orEmpty()
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

    private fun loadFeed() {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val locationLabel = HomePrefs.getLocation(requireContext())
            val hero = fetchArticles(category.query, HERO_COUNT).firstOrNull()
            val trending = fetchArticles(trendingQuery(), TRENDING_COUNT)
            val local = if (category.type == HomeCategoryType.HOME) {
                fetchArticles(
                    query = localQuery(locationLabel),
                    count = LOCAL_COUNT,
                    location = locationLabel
                )
            } else {
                emptyList()
            }
            val feed = if (category.type == HomeCategoryType.INTEREST) {
                fetchArticles(category.query, FEED_COUNT, page = 1)
            } else {
                emptyList()
            }
            val pulse = if (category.type == HomeCategoryType.HOME) {
                fetchArticles(TRENDING_PULSE_QUERY, TRENDING_PULSE_COUNT)
            } else {
                emptyList()
            }

            if (!isAdded) return@launch

            val items = buildItems(
                hero = hero,
                trending = trending,
                local = local,
                pulse = pulse,
                feed = feed,
                locationLabel = locationLabel
            )

            if (items.isEmpty()) {
                showError(getString(R.string.home_error))
            } else {
                feedAdapter.submitList(items)
                showContent()
            }
        }
    }

    private suspend fun fetchArticles(
        query: String,
        count: Int,
        page: Int = 0,
        location: String? = null
    ): List<Article> {
        return withContext(Dispatchers.IO) {
            runCatching {
                searchRepository.fetchSerpNews(
                    query = query,
                    page = page,
                    pageSize = count,
                    location = location,
                    useRawImageUrls = true
                ).take(count)
            }.getOrDefault(emptyList())
        }
    }

    private fun buildItems(
        hero: Article?,
        trending: List<Article>,
        local: List<Article>,
        pulse: List<Article>,
        feed: List<Article>,
        locationLabel: String
    ): List<HomeFeedItem> {
        val items = mutableListOf<HomeFeedItem>()
        hero?.let { article ->
            val readTime = estimateReadTime(article.title)
            val meta = buildMeta(readTime, article.ageLabel.orEmpty())
            items.add(HomeFeedItem.Hero(article, readTime, meta))
        }
        if (trending.isNotEmpty()) {
            items.add(HomeFeedItem.SectionHeader(getString(R.string.home_trending_now)))
            items.addAll(trending.take(TRENDING_COUNT).map { HomeFeedItem.SmallCard(it) })
        }

        if (category.type == HomeCategoryType.HOME) {
            items.add(HomeFeedItem.SectionHeader(localHeader(locationLabel)))
            if (local.isNotEmpty()) {
                items.addAll(local.take(LOCAL_COUNT).map { HomeFeedItem.SmallCard(it) })
            }
            items.add(HomeFeedItem.CtaButton(getString(R.string.home_local_more)))

            if (pulse.isNotEmpty()) {
                items.add(HomeFeedItem.SectionHeader(getString(R.string.home_trending_pulse)))
                items.addAll(
                    pulse.take(TRENDING_PULSE_COUNT).mapIndexed { index, article ->
                        HomeFeedItem.TrendingPulse(
                            rank = index + 1,
                            article = article,
                            indicator = pulseIndicator(index)
                        )
                    }
                )
            }
        } else {
            if (feed.isNotEmpty()) {
                items.add(HomeFeedItem.SectionHeader(getString(R.string.home_latest_stories)))
                items.addAll(feed.take(FEED_COUNT).map { HomeFeedItem.FeedCard(it) })
            }
        }
        return items
    }

    private fun trendingQuery(): String {
        return if (category.type == HomeCategoryType.HOME) {
            "trending news"
        } else {
            "trending ${category.displayName} news"
        }
    }

    private fun localQuery(locationLabel: String): String {
        return if (locationLabel.isBlank()) {
            "local news"
        } else {
            "local news $locationLabel"
        }
    }

    private fun localHeader(locationLabel: String): String {
        return if (locationLabel.isBlank()) {
            getString(R.string.home_local_near_you)
        } else {
            getString(R.string.home_local_near_city, locationLabel)
        }
    }

    private fun pulseIndicator(index: Int): TrendIndicator {
        return when (index) {
            0, 1 -> TrendIndicator.UP
            2 -> TrendIndicator.FLAT
            else -> TrendIndicator.DOWN
        }
    }

    private fun estimateReadTime(title: String): String {
        val wordCount = title.split(Regex("\\s+")).count { it.isNotBlank() }
        val minutes = max(2, ceil(wordCount / 3.0).toInt())
        return resources.getQuantityString(R.plurals.home_read_minutes, minutes, minutes)
    }

    private fun buildMeta(readTime: String, ageLabel: String): String {
        return if (ageLabel.isBlank()) {
            readTime
        } else {
            getString(R.string.home_hero_meta, readTime, ageLabel)
        }
    }

    private fun showLoading() {
        loadingView.isVisible = true
        errorView.isVisible = false
        feedView.isVisible = false
    }

    private fun showError(message: String) {
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

    private fun openArticle(article: Article) {
        if (article.url.isBlank()) return
        val ctx = requireContext()
        ctx.startActivity(
            Intent(ctx, ArticleWebViewActivity::class.java)
                .putExtra("url", article.url)
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
        private const val ARG_QUERY = "arg_category_query"

        private const val HERO_COUNT = 1
        private const val TRENDING_COUNT = 5
        private const val LOCAL_COUNT = 5
        private const val FEED_COUNT = 10
        private const val TRENDING_PULSE_COUNT = 5
        private const val TRENDING_PULSE_QUERY = "trending topics"

        fun newInstance(category: HomeCategory): HomeCategoryPageFragment {
            return HomeCategoryPageFragment().apply {
                arguments = bundleOf(
                    ARG_ID to category.id,
                    ARG_NAME to category.displayName,
                    ARG_TYPE to category.type.name,
                    ARG_QUERY to category.query
                )
            }
        }
    }
}
