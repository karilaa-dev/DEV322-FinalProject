package com.bananaginger.noisedetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bananaginger.noisedetector.data.repository.AnomalyRepository

// BananaGinger/Kyryl: Factory injects repository without adding a dependency injection framework.
class AnomalyViewModelFactory(
    private val repository: AnomalyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnomalyViewModel::class.java)) {
            return AnomalyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
