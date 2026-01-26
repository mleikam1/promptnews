package com.digitalturbine.promptnews.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView

@Composable
fun HomeFragmentHost() {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity ?: return
    val fragmentManager = fragmentActivity.supportFragmentManager
    val containerId = remember { View.generateViewId() }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                if (fragmentManager.findFragmentById(containerId) == null) {
                    fragmentManager
                        .beginTransaction()
                        .replace(containerId, HomeFragment.newInstance())
                        .commit()
                }
            }
        }
    )
}
