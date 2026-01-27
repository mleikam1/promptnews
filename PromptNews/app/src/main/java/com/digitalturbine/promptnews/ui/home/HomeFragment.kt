package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val topStoriesAdapter = HomeArticleAdapter(::openArticle)
    private val localStoriesAdapter = LocalArticleAdapter(::openArticle)
    private val searchRepository = SearchRepository()
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private lateinit var loadingView: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var contentView: View
    private lateinit var topHeaderView: TextView
    private lateinit var localHeaderView: TextView
    private lateinit var localSubtitleView: TextView
    private lateinit var topEmptyView: TextView
    private lateinit var localEmptyView: TextView
    private lateinit var topListView: RecyclerView
    private lateinit var localListView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingView = view.findViewById(R.id.home_loading)
        errorView = view.findViewById(R.id.home_error)
        contentView = view.findViewById(R.id.home_content)
        topHeaderView = view.findViewById(R.id.top_stories_header)
        localHeaderView = view.findViewById(R.id.local_stories_header)
        localSubtitleView = view.findViewById(R.id.local_stories_subtitle)
        topEmptyView = view.findViewById(R.id.top_stories_empty)
        localEmptyView = view.findViewById(R.id.local_stories_empty)
        topListView = view.findViewById(R.id.top_stories_list)
        localListView = view.findViewById(R.id.local_stories_list)

        configureList(topListView, topStoriesAdapter, true)
        configureList(localListView, localStoriesAdapter, false)

        loadFeed(HomePrefs.getLocation(requireContext()))
    }

    override fun onStart() {
        super.onStart()
        val prefs = HomePrefs.getPrefs(requireContext())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HomePrefs.KEY_LOCATION) {
                loadFeed(HomePrefs.getLocation(requireContext()))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        prefsListener = listener
    }

    override fun onStop() {
        super.onStop()
        val prefs = HomePrefs.getPrefs(requireContext())
        prefsListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        prefsListener = null
    }

    private fun configureList(listView: RecyclerView, adapter: RecyclerView.Adapter<*>, showDivider: Boolean) {
        listView.layoutManager = LinearLayoutManager(requireContext())
        listView.adapter = adapter
        listView.isNestedScrollingEnabled = false
        if (showDivider) {
            listView.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun loadFeed(locationLabel: String) {
        topHeaderView.text = getString(R.string.home_top_stories)
        localHeaderView.text = getString(R.string.home_local_stories)
        localSubtitleView.text = locationLabel
        localSubtitleView.isVisible = locationLabel.isNotBlank()
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val topResult = withContext(Dispatchers.IO) {
                runCatching {
                    searchRepository.fetchSerpNews(
                        query = "top news",
                        page = 0,
                        pageSize = TOP_STORIES_COUNT,
                        useRawImageUrls = true
                    )
                }
            }
            val localResult = if (locationLabel.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        searchRepository.fetchSerpNews(
                            query = "local news $locationLabel",
                            page = 0,
                            pageSize = LOCAL_STORIES_TOTAL,
                            location = locationLabel,
                            useRawImageUrls = true
                        )
                    }
                }
            } else {
                Result.success(emptyList())
            }

            if (!isAdded) return@launch

            if (topResult.isFailure && localResult.isFailure) {
                showError(getString(R.string.home_error))
                return@launch
            }

            val topStories = topResult.getOrDefault(emptyList())
                .take(TOP_STORIES_COUNT)
            val localStories = localResult.getOrDefault(emptyList())
                .take(LOCAL_STORIES_TOTAL)

            topStoriesAdapter.submitList(topStories)
            localStoriesAdapter.submitList(localStories)

            topEmptyView.isVisible = topStories.isEmpty()
            topListView.isVisible = topStories.isNotEmpty()
            localEmptyView.text = if (locationLabel.isBlank()) {
                getString(R.string.home_local_requires_location)
            } else {
                getString(R.string.home_local_empty)
            }
            localEmptyView.isVisible = localStories.isEmpty()
            localListView.isVisible = localStories.isNotEmpty()

            showContent()
        }
    }

    private fun showLoading() {
        loadingView.isVisible = true
        errorView.isVisible = false
        contentView.isVisible = false
    }

    private fun showError(message: String) {
        errorView.text = message
        errorView.isVisible = true
        loadingView.isVisible = false
        contentView.isVisible = false
    }

    private fun showContent() {
        contentView.isVisible = true
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

    companion object {
        private const val TOP_STORIES_COUNT = 7
        private const val LOCAL_STORIES_TOTAL = 8
        fun newInstance() = HomeFragment()
    }
}

private class HomeArticleAdapter(
    private val onClick: (Article) -> Unit
) : ListAdapter<Article, SmallArticleViewHolder>(ArticleDiff) {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SmallArticleViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_local_article, parent, false)
        return SmallArticleViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: SmallArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class LocalArticleAdapter(
    private val onClick: (Article) -> Unit
) : ListAdapter<Article, RecyclerView.ViewHolder>(ArticleDiff) {
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FEATURE else VIEW_TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = android.view.LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_FEATURE) {
            val view = inflater.inflate(R.layout.item_local_feature_article, parent, false)
            FeatureArticleViewHolder(view, onClick)
        } else {
            val view = inflater.inflate(R.layout.item_local_article, parent, false)
            SmallArticleViewHolder(view, onClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = getItem(position)
        when (holder) {
            is FeatureArticleViewHolder -> holder.bind(article)
            is SmallArticleViewHolder -> holder.bind(article)
        }
    }

    companion object {
        private const val VIEW_TYPE_FEATURE = 0
        private const val VIEW_TYPE_SMALL = 1
    }
}

private class SmallArticleViewHolder(
    itemView: View,
    private val onClick: (Article) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val titleView: TextView = itemView.findViewById(R.id.local_article_title)
    private val metaView: TextView = itemView.findViewById(R.id.local_article_meta)
    private val thumbnailView: ImageView = itemView.findViewById(R.id.local_article_thumbnail)
    private val logoView: ImageView = itemView.findViewById(R.id.local_article_source_logo)
    private val chipView: com.google.android.material.chip.Chip =
        itemView.findViewById(R.id.local_article_chip)

    fun bind(article: Article) {
        titleView.text = article.title
        val meta = listOfNotNull(article.sourceName, article.ageLabel)
            .joinToString(" • ")
        metaView.text = meta
        metaView.isVisible = meta.isNotBlank()

        bindImage(thumbnailView, article.imageUrl)
        bindImage(logoView, article.logoUrl)

        val chipLabel = formatInterestLabel(article.interest)
        chipView.text = chipLabel
        chipView.isVisible = !chipLabel.isNullOrBlank()
        itemView.setOnClickListener { onClick(article) }
    }
}

private class FeatureArticleViewHolder(
    itemView: View,
    private val onClick: (Article) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.local_feature_image)
    private val titleView: TextView = itemView.findViewById(R.id.local_feature_title)
    private val metaView: TextView = itemView.findViewById(R.id.local_feature_meta)
    private val chipView: com.google.android.material.chip.Chip =
        itemView.findViewById(R.id.local_feature_chip)

    fun bind(article: Article) {
        titleView.text = article.title
        val meta = listOfNotNull(article.sourceName, article.ageLabel)
            .joinToString(" • ")
        metaView.text = meta
        metaView.isVisible = meta.isNotBlank()

        bindImage(imageView, article.imageUrl)

        val chipLabel = formatInterestLabel(article.interest)
        chipView.text = chipLabel
        chipView.isVisible = !chipLabel.isNullOrBlank()
        itemView.setOnClickListener { onClick(article) }
    }
}

private fun formatInterestLabel(interest: String): String? {
    return interest
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { char -> char.titlecase() }
}

private fun bindImage(imageView: ImageView, url: String) {
    if (url.isBlank()) {
        imageView.isVisible = false
        imageView.setImageDrawable(null)
        return
    }
    imageView.isVisible = true
    imageView.load(url) {
        listener(
            onError = { _, _ ->
                imageView.isVisible = false
            }
        )
    }
}

private object ArticleDiff : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
        return oldItem == newItem
    }
}
