package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.ui.components.FotoscapesArticleCard
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesArticleActivity
import com.digitalturbine.promptnews.ui.fotoscapes.toFotoscapesArticleUi
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val interests = remember {
        UserInterestRepositoryImpl.getInstance(context).getSelectedInterests().toList()
    }
    val selectedInterest by viewModel.selectedInterest.collectAsState()
    val fotoscapesItems by viewModel.fotoscapesItems.collectAsState()
    val hasFetchedForInterest by viewModel.hasFetchedForInterest.collectAsState()

    LaunchedEffect(interests) {
        if (interests.isNotEmpty() && !interests.contains(selectedInterest)) {
            viewModel.setSelectedInterest(interests.first())
        }
    }

    LaunchedEffect(selectedInterest) {
        viewModel.loadFotoscapesForInterest(selectedInterest)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (interests.isNotEmpty()) {
            val selectedIndex = interests.indexOf(selectedInterest).coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex])
                    )
                }
            ) {
                interests.forEachIndexed { index, interest ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { viewModel.setSelectedInterest(interest) },
                        text = { Text(text = interest.displayName) }
                    )
                }
            }
        }

        when {
            !hasFetchedForInterest -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            fotoscapesItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.home_interest_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(fotoscapesItems) { article ->
                        val ui = remember(article) { article.toFotoscapesArticleUi() }
                        FotoscapesArticleCard(
                            article = ui,
                            onClick = {
                                if (article.lbType.equals("article", ignoreCase = true)) {
                                    FotoscapesArticleActivity.start(context, ui)
                                } else {
                                    val url = article.articleUrl
                                    if (url.isNotBlank()) {
                                        context.startActivity(
                                            Intent(context, ArticleWebViewActivity::class.java)
                                                .putExtra("url", url)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
