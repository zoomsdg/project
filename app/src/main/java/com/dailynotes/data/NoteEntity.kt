package com.dailynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "daily",
    val createdAt: Date,
    val updatedAt: Date,
    val imagePaths: String = "", // JSON string of image paths
    val audioPaths: String = ""  // JSON string of audio paths
)