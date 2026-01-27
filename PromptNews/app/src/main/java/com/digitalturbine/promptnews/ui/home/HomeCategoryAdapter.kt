package com.digitalturbine.promptnews.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Article
import com.google.android.material.button.MaterialButton

class HomeCategoryAdapter(
    private val onArticleClick: (Article) -> Unit,
    private val onCtaClick: () -> Unit
) : ListAdapter<HomeFeedItem, RecyclerView.ViewHolder>(HomeFeedDiff) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeFeedItem.Hero -> VIEW_TYPE_HERO
            is HomeFeedItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is HomeFeedItem.SmallCard -> VIEW_TYPE_SMALL_CARD
            is HomeFeedItem.FeedCard -> VIEW_TYPE_FEED_CARD
            is HomeFeedItem.TrendingPulse -> VIEW_TYPE_TRENDING_PULSE
            is HomeFeedItem.CtaButton -> VIEW_TYPE_CTA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HERO -> {
                val view = inflater.inflate(R.layout.item_home_hero, parent, false)
                HeroViewHolder(view, onArticleClick)
            }
            VIEW_TYPE_SECTION_HEADER -> {
                val view = inflater.inflate(R.layout.item_home_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            VIEW_TYPE_SMALL_CARD -> {
                val view = inflater.inflate(R.layout.item_local_article, parent, false)
                SmallCardViewHolder(view, onArticleClick)
            }
            VIEW_TYPE_FEED_CARD -> {
                val view = inflater.inflate(R.layout.item_home_feed_card, parent, false)
                FeedCardViewHolder(view, onArticleClick)
            }
            VIEW_TYPE_TRENDING_PULSE -> {
                val view = inflater.inflate(R.layout.item_trending_pulse, parent, false)
                TrendingPulseViewHolder(view, onArticleClick)
            }
            VIEW_TYPE_CTA -> {
                val view = inflater.inflate(R.layout.item_home_cta_button, parent, false)
                CtaViewHolder(view, onCtaClick)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeFeedItem.Hero -> (holder as HeroViewHolder).bind(item)
            is HomeFeedItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item)
            is HomeFeedItem.SmallCard -> (holder as SmallCardViewHolder).bind(item.article)
            is HomeFeedItem.FeedCard -> (holder as FeedCardViewHolder).bind(item.article)
            is HomeFeedItem.TrendingPulse -> (holder as TrendingPulseViewHolder).bind(item)
            is HomeFeedItem.CtaButton -> (holder as CtaViewHolder).bind(item)
        }
    }

    private class HeroViewHolder(
        itemView: View,
        private val onClick: (Article) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.home_hero_image)
        private val titleView: TextView = itemView.findViewById(R.id.home_hero_title)
        private val metaView: TextView = itemView.findViewById(R.id.home_hero_meta)
        private val logoView: ImageView = itemView.findViewById(R.id.home_hero_logo)
        private val sourceView: TextView = itemView.findViewById(R.id.home_hero_source)
        private val badgeView: TextView = itemView.findViewById(R.id.home_hero_badge)

        fun bind(item: HomeFeedItem.Hero) {
            val article = item.article
            titleView.text = article.title
            metaView.text = item.meta
            metaView.isVisible = item.meta.isNotBlank()
            sourceView.text = article.sourceName.orEmpty()
            sourceView.isVisible = sourceView.text.isNotBlank()
            badgeView.text = itemView.context.getString(R.string.home_top_story_badge)
            bindImage(imageView, article.imageUrl)
            bindImage(logoView, article.logoUrl)
            itemView.setOnClickListener { onClick(article) }
        }
    }

    private class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.home_section_title)

        fun bind(item: HomeFeedItem.SectionHeader) {
            titleView.text = item.title
        }
    }

    private class SmallCardViewHolder(
        itemView: View,
        private val onClick: (Article) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.local_article_title)
        private val metaView: TextView = itemView.findViewById(R.id.local_article_meta)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.local_article_thumbnail)
        private val logoView: ImageView = itemView.findViewById(R.id.local_article_source_logo)
        private val sourceNameView: TextView = itemView.findViewById(R.id.local_article_source_name)
        private val sourceRowView: View = itemView.findViewById(R.id.local_article_source_row)
        private val chipView: TextView = itemView.findViewById(R.id.local_article_chip)

        fun bind(article: Article) {
            titleView.text = article.title
            sourceNameView.text = article.sourceName.orEmpty()
            sourceNameView.isVisible = sourceNameView.text.isNotBlank()
            metaView.text = article.ageLabel.orEmpty()
            metaView.isVisible = metaView.text.isNotBlank()
            bindImage(thumbnailView, article.imageUrl)
            bindImage(logoView, article.logoUrl)
            sourceRowView.isVisible = sourceNameView.isVisible || logoView.isVisible
            chipView.isVisible = false
            itemView.setOnClickListener { onClick(article) }
        }
    }

    private class FeedCardViewHolder(
        itemView: View,
        private val onClick: (Article) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.home_feed_image)
        private val titleView: TextView = itemView.findViewById(R.id.home_feed_title)
        private val metaView: TextView = itemView.findViewById(R.id.home_feed_meta)
        private val logoView: ImageView = itemView.findViewById(R.id.home_feed_logo)
        private val sourceView: TextView = itemView.findViewById(R.id.home_feed_source)
        private val ctaButton: MaterialButton = itemView.findViewById(R.id.home_feed_cta)

        fun bind(article: Article) {
            titleView.text = article.title
            metaView.text = article.ageLabel.orEmpty()
            metaView.isVisible = metaView.text.isNotBlank()
            sourceView.text = article.sourceName.orEmpty()
            sourceView.isVisible = sourceView.text.isNotBlank()
            bindImage(imageView, article.imageUrl)
            bindImage(logoView, article.logoUrl)
            ctaButton.isVisible = article.url.isNotBlank()
            ctaButton.setOnClickListener { onClick(article) }
            itemView.setOnClickListener { onClick(article) }
        }
    }

    private class TrendingPulseViewHolder(
        itemView: View,
        private val onClick: (Article) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val rankView: TextView = itemView.findViewById(R.id.trending_pulse_rank)
        private val titleView: TextView = itemView.findViewById(R.id.trending_pulse_title)
        private val metaView: TextView = itemView.findViewById(R.id.trending_pulse_meta)
        private val indicatorView: TextView = itemView.findViewById(R.id.trending_pulse_indicator)

        fun bind(item: HomeFeedItem.TrendingPulse) {
            val article = item.article
            rankView.text = item.rank.toString()
            titleView.text = article.title
            metaView.text = listOfNotNull(article.sourceName, article.ageLabel)
                .joinToString(" • ")
            metaView.isVisible = metaView.text.isNotBlank()
            indicatorView.text = when (item.indicator) {
                TrendIndicator.UP -> "▲"
                TrendIndicator.DOWN -> "▼"
                TrendIndicator.FLAT -> "■"
            }
            val color = when (item.indicator) {
                TrendIndicator.UP -> R.color.trending_up
                TrendIndicator.DOWN -> R.color.trending_down
                TrendIndicator.FLAT -> R.color.trending_flat
            }
            indicatorView.setTextColor(ContextCompat.getColor(itemView.context, color))
            itemView.setOnClickListener { onClick(article) }
        }
    }

    private class CtaViewHolder(
        itemView: View,
        private val onClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val button: MaterialButton = itemView.findViewById(R.id.home_cta_button)

        fun bind(item: HomeFeedItem.CtaButton) {
            button.text = item.label
            button.setOnClickListener { onClick() }
        }
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
                onError = { _, _ -> imageView.isVisible = false }
            )
        }
    }

    companion object {
        private const val VIEW_TYPE_HERO = 0
        private const val VIEW_TYPE_SECTION_HEADER = 1
        private const val VIEW_TYPE_SMALL_CARD = 2
        private const val VIEW_TYPE_FEED_CARD = 3
        private const val VIEW_TYPE_TRENDING_PULSE = 4
        private const val VIEW_TYPE_CTA = 5
    }
}

private object HomeFeedDiff : DiffUtil.ItemCallback<HomeFeedItem>() {
    override fun areItemsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
        return when {
            oldItem is HomeFeedItem.Hero && newItem is HomeFeedItem.Hero ->
                oldItem.article.url == newItem.article.url
            oldItem is HomeFeedItem.SectionHeader && newItem is HomeFeedItem.SectionHeader ->
                oldItem.title == newItem.title
            oldItem is HomeFeedItem.SmallCard && newItem is HomeFeedItem.SmallCard ->
                oldItem.article.url == newItem.article.url
            oldItem is HomeFeedItem.FeedCard && newItem is HomeFeedItem.FeedCard ->
                oldItem.article.url == newItem.article.url
            oldItem is HomeFeedItem.TrendingPulse && newItem is HomeFeedItem.TrendingPulse ->
                oldItem.article.url == newItem.article.url && oldItem.rank == newItem.rank
            oldItem is HomeFeedItem.CtaButton && newItem is HomeFeedItem.CtaButton ->
                oldItem.label == newItem.label
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
        return oldItem == newItem
    }
}
