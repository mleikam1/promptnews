package com.digitalturbine.promptnews.ui.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SportsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SportsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
