package com.dailynotes.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

object MediaUtils {
    
    fun createImageFile(context: Context): File {
        val imageFileName = "JPEG_${Date().time}"
        val storageDir = File(context.getExternalFilesDir(null), "Pictures")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "$imageFileName.jpg")
    }
    
    fun createAudioFile(context: Context): File {
        val audioFileName = "AUDIO_${Date().time}"
        val storageDir = File(context.getExternalFilesDir(null), "Audio")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "$audioFileName.mp3")
    }
    
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    fun stringToPathList(pathsString: String): List<String> {
        if (pathsString.isEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(pathsString, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun pathListToString(paths: List<String>): String {
        return Gson().toJson(paths)
    }
}