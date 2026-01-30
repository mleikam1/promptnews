package com.digitalturbine.promptnews.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.HomeCategoryRepository
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.InterestCatalog
import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeCategoryRepository: HomeCategoryRepository = HomeCategoryRepository()
) : ViewModel() {
    private val _selectedInterest = MutableStateFlow(InterestCatalog.interests.first())
    val selectedInterest: StateFlow<Interest> = _selectedInterest.asStateFlow()

    private val _fotoscapesItems = MutableStateFlow<List<FotoscapesArticle>>(emptyList())
    val fotoscapesItems: StateFlow<List<FotoscapesArticle>> = _fotoscapesItems.asStateFlow()

    private val _hasFetchedForInterest = MutableStateFlow(false)
    val hasFetchedForInterest: StateFlow<Boolean> = _hasFetchedForInterest.asStateFlow()

    fun setSelectedInterest(interest: Interest) {
        if (_selectedInterest.value == interest) return
        _selectedInterest.value = interest
        _hasFetchedForInterest.value = false
    }

    fun loadFotoscapesForInterest(interest: Interest) {
        viewModelScope.launch {
            _hasFetchedForInterest.value = false
            val items = homeCategoryRepository.loadFotoscapesInterest(HomeCategory.interest(interest))
            _fotoscapesItems.value = items
            _hasFetchedForInterest.value = true
            Log.d("FS_UI", "Emitting ${items.size} items for $interest")
        }
    }
}
