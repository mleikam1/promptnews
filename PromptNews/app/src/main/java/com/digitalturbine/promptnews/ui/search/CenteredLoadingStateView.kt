package com.digitalturbine.promptnews.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight

@Composable
fun CenteredLoadingStateView(
    query: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // The shimmer animation is created only while visible; it stops when loading ends.
        val transition = rememberInfiniteTransition(label = "loadingShimmer")
        val shimmerShift = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerShift"
        )

        val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        val shimmerWidth = 320f
        val shimmerStart = shimmerShift.value * (shimmerWidth * 2f) - shimmerWidth

        Box(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Finding news and information for “$query”",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, highlightColor, baseColor),
                        start = Offset(shimmerStart, 0f),
                        end = Offset(shimmerStart + shimmerWidth, 0f)
                    )
                )
            )
        }
    }
}
