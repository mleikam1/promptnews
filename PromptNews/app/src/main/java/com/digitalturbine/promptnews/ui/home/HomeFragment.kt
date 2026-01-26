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
import com.digitalturbine.promptnews.data.rss.GoogleNewsRepository
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.util.TimeLabelFormatter
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val topStoriesAdapter = HomeArticleAdapter(::openArticle)
    private val localStoriesAdapter = LocalArticleAdapter(::openArticle)
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
                runCatching { GoogleNewsRepository.topStories() }
            }
            val localResult = if (locationLabel.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching { GoogleNewsRepository.localNews(locationLabel) }
                }
            } else {
                Result.success(emptyList())
            }

            if (!isAdded) return@launch

            if (topResult.isFailure && localResult.isFailure) {
                showError(getString(R.string.home_error))
                return@launch
            }

            val topStories = topResult.getOrDefault(emptyList()).toUiArticles("top")
            val localStories = localResult.getOrDefault(emptyList()).toUiArticles("local")

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
        fun newInstance() = HomeFragment()
    }
}

private class HomeArticleAdapter(
    private val onClick: (Article) -> Unit
) : ListAdapter<Article, HomeArticleAdapter.HomeArticleViewHolder>(ArticleDiff) {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HomeArticleViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_article, parent, false)
        return HomeArticleViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: HomeArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HomeArticleViewHolder(
        itemView: View,
        private val onClick: (Article) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.article_title)
        private val metaView: TextView = itemView.findViewById(R.id.article_meta)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.article_thumbnail)

        fun bind(article: Article) {
            titleView.text = article.title
            val meta = listOfNotNull(article.sourceName, article.ageLabel)
                .joinToString(" • ")
            metaView.text = meta
            metaView.isVisible = meta.isNotBlank()
            thumbnailView.load(article.imageUrl) {
                placeholder(R.drawable.ic_topic_placeholder)
                error(R.drawable.ic_topic_placeholder)
            }
            itemView.setOnClickListener { onClick(article) }
        }
    }
}

private class LocalArticleAdapter(
    private val onClick: (Article) -> Unit
) : ListAdapter<Article, LocalArticleAdapter.LocalArticleViewHolder>(ArticleDiff) {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LocalArticleViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_local_article, parent, false)
        return LocalArticleViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: LocalArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LocalArticleViewHolder(
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

            thumbnailView.load(article.imageUrl) {
                placeholder(R.drawable.ic_topic_placeholder)
                error(R.drawable.ic_topic_placeholder)
            }

            if (article.logoUrl.isNotBlank()) {
                logoView.load(article.logoUrl) {
                    placeholder(R.drawable.ic_topic_placeholder)
                    error(R.drawable.ic_topic_placeholder)
                }
            } else {
                logoView.setImageResource(R.drawable.ic_topic_placeholder)
            }
            logoView.isVisible = true

            val chipLabel = article.interest
                .takeIf { it.isNotBlank() }
                ?.replaceFirstChar { char -> char.titlecase() }
            chipView.text = chipLabel
            chipView.isVisible = !chipLabel.isNullOrBlank()
            itemView.setOnClickListener { onClick(article) }
        }
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

private fun List<com.digitalturbine.promptnews.data.rss.Article>.toUiArticles(
    interestLabel: String
): List<Article> {
    return map { article ->
        Article(
            title = article.title,
            url = article.link,
            imageUrl = article.imageUrl.orEmpty(),
            logoUrl = "",
            sourceName = article.source,
            ageLabel = TimeLabelFormatter.formatTimeLabel(article.published?.toEpochMilli()),
            interest = interestLabel,
            isFotoscapes = false
        )
    }
}
