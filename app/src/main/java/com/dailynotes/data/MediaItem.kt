package com.dailynotes.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val path: String,
    val type: MediaType,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

enum class MediaType {
    IMAGE, AUDIO
}