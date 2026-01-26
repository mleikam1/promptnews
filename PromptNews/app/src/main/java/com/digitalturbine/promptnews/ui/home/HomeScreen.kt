package com.digitalturbine.promptnews.ui.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesRepository
import com.digitalturbine.promptnews.data.fotoscapes.InterestSectionResult
import com.digitalturbine.promptnews.ui.PromptNewsTopBar
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.ui.onboarding.OnboardingActivity
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

private sealed class FollowingFeedState {
    data object Loading : FollowingFeedState()
    data class Ready(val sections: List<InterestSectionResult>) : FollowingFeedState()
    data object Empty : FollowingFeedState()
    data class Error(val message: String) : FollowingFeedState()
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val repo = remember { UserInterestRepositoryImpl.getInstance(ctx) }
    val selected = remember { repo.getSelectedInterests() }

    LaunchedEffect(selected) {
        if (selected.isEmpty()) {
            ctx.startActivity(Intent(ctx, OnboardingActivity::class.java))
            (ctx as? Activity)?.finish()
        }
    }

    val feedState by produceState<FollowingFeedState>(
        initialValue = FollowingFeedState.Loading,
        key1 = selected
    ) {
        if (selected.isEmpty()) {
            value = FollowingFeedState.Empty
            return@produceState
        }
        val repository = FotoscapesRepository()
        value = runCatching { repository.fetchInterestSections(selected) }
            .fold(
                onSuccess = { sections ->
                    if (sections.all { it.articles.isEmpty() }) {
                        FollowingFeedState.Empty
                    } else {
                        FollowingFeedState.Ready(sections)
                    }
                },
                onFailure = { err -> FollowingFeedState.Error(err.message ?: "Unable to load feed.") }
            )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PromptNewsTopBar(
            title = "Following",
            showBack = false,
            onBack = {},
            showProfileIcon = true
        )
        when (val state = feedState) {
            FollowingFeedState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            FollowingFeedState.Empty -> {
                EmptyFollowingState(onPickInterests = {
                    ctx.startActivity(Intent(ctx, OnboardingActivity::class.java))
                    (ctx as? Activity)?.finish()
                })
            }
            is FollowingFeedState.Error -> {
                ErrorFollowingState(message = state.message)
            }
            is FollowingFeedState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    state.sections.forEach { section ->
                        if (section.articles.isNotEmpty()) {
                            val hero = section.articles.first()
                            val rows = section.articles.drop(1)
                            item(key = "header-${section.interest.id}") {
                                Text(
                                    text = section.interest.displayName,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            item(key = "hero-${section.interest.id}") {
                                HeroCard(article = hero) {
                                    openArticle(ctx, hero.url)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            items(rows, key = { article -> "${section.interest.id}-${article.url}" }) { article ->
                                RowCard(a = article) { openArticle(ctx, article.url) }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                            item(key = "footer-${section.interest.id}") {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyFollowingState(onPickInterests: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Pick at least 3 interests to personalize your feed.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPickInterests) {
                Text(text = "Choose interests")
            }
        }
    }
}

@Composable
private fun ErrorFollowingState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun openArticle(ctx: android.content.Context, url: String) {
    if (url.isBlank()) return
    ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
}
