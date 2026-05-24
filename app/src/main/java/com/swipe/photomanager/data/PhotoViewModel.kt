package com.swipe.photomanager.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    private val _mediaByMonth = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val mediaByMonth: StateFlow<Map<String, List<MediaItem>>> = _mediaByMonth.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _currentMonthItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentMonthItems = _currentMonthItems.asStateFlow()

    private val _itemsToDelete = MutableStateFlow<Set<MediaItem>>(emptySet())
    val itemsToDelete = _itemsToDelete.asStateFlow()

    private val _completedMonths = MutableStateFlow<Set<String>>(emptySet())
    val completedMonths = _completedMonths.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished = _isFinished.asStateFlow()

    fun loadMedia() {
        viewModelScope.launch {
            val allMedia = repository.fetchMedia()
            _mediaByMonth.value = allMedia.groupBy { it.month }
        }
    }

    fun selectMonth(month: String?) {
        _selectedMonth.value = month
        if (month == null) {
            _currentMonthItems.value = emptyList()
        } else {
            _currentMonthItems.value = _mediaByMonth.value[month] ?: emptyList()
        }
        _itemsToDelete.value = emptySet()
        _isFinished.value = false
    }

    fun markForDeletion(item: MediaItem) {
        _itemsToDelete.value = _itemsToDelete.value + item
    }

    fun toggleDeletion(item: MediaItem) {
        if (_itemsToDelete.value.contains(item)) {
            _itemsToDelete.value = _itemsToDelete.value - item
        } else {
            _itemsToDelete.value = _itemsToDelete.value + item
        }
    }

    fun markToKeep(item: MediaItem) {
        _itemsToDelete.value = _itemsToDelete.value - item
    }

    fun finishReview() {
        selectedMonth.value?.let {
            _completedMonths.value = _completedMonths.value + it
        }
        _isFinished.value = false
        _selectedMonth.value = null
    }

    fun setFinished(finished: Boolean) {
        _isFinished.value = finished
    }
}