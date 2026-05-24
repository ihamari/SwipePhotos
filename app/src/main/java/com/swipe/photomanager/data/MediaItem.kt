package com.swipe.photomanager.data

import android.net.Uri

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long, // seconds
    val type: MediaType,
    val month: String // Formatted as "yyyy-MM" or similar for grouping
)