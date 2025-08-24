package com.dailynotes.utils

import android.content.Context
import android.net.Uri
import com.dailynotes.data.NoteEntity
import com.dailynotes.data.NoteRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportImportManager @Inject constructor(
    private val repository: NoteRepository
) {
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    suspend fun exportData(context: Context): Result<File> {
        return try {
            val notes = repository.getAllNotes().first()
            val exportData = ExportData(
                version = 1,
                exportDate = Date(),
                notes = notes
            )
            
            val jsonString = gson.toJson(exportData)
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val exportDir = File(context.getExternalFilesDir(null), "Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val zipFile = File(exportDir, "daily_notes_backup_$timestamp.zip")
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                val jsonEntry = ZipEntry("notes.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonString.toByteArray())
                zipOut.closeEntry()
                
                notes.forEach { note ->
                    val imagePaths = MediaUtils.stringToPathList(note.imagePaths)
                    imagePaths.forEach { imagePath ->
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val imageEntry = ZipEntry("images/${imageFile.name}")
                            zipOut.putNextEntry(imageEntry)
                            FileInputStream(imageFile).use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                    
                    val audioPaths = MediaUtils.stringToPathList(note.audioPaths)
                    audioPaths.forEach { audioPath ->
                        val audioFile = File(audioPath)
                        if (audioFile.exists()) {
                            val audioEntry = ZipEntry("audios/${audioFile.name}")
                            zipOut.putNextEntry(audioEntry)
                            FileInputStream(audioFile).use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importData(context: Context, uri: Uri): Result<Int> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file"))
            
            val tempFile = File(context.cacheDir, "temp_import.zip")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            var importedCount = 0
            val imageDir = File(context.getExternalFilesDir(null), "Pictures")
            val audioDir = File(context.getExternalFilesDir(null), "Audio")
            
            if (!imageDir.exists()) imageDir.mkdirs()
            if (!audioDir.exists()) audioDir.mkdirs()
            
            ZipInputStream(FileInputStream(tempFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                var exportData: ExportData? = null
                
                while (entry != null) {
                    when {
                        entry.name == "notes.json" -> {
                            val jsonString = zipIn.readBytes().toString(Charsets.UTF_8)
                            exportData = gson.fromJson(jsonString, ExportData::class.java)
                        }
                        entry.name.startsWith("images/") -> {
                            val fileName = entry.name.substringAfter("images/")
                            val targetFile = File(imageDir, fileName)
                            FileOutputStream(targetFile).use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                        entry.name.startsWith("audios/") -> {
                            val fileName = entry.name.substringAfter("audios/")
                            val targetFile = File(audioDir, fileName)
                            FileOutputStream(targetFile).use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                exportData?.notes?.forEach { note ->
                    val updatedImagePaths = MediaUtils.stringToPathList(note.imagePaths)
                        .map { path ->
                            val fileName = File(path).name
                            File(imageDir, fileName).absolutePath
                        }
                    
                    val updatedAudioPaths = MediaUtils.stringToPathList(note.audioPaths)
                        .map { path ->
                            val fileName = File(path).name
                            File(audioDir, fileName).absolutePath
                        }
                    
                    val importedNote = note.copy(
                        id = 0,
                        imagePaths = MediaUtils.pathListToString(updatedImagePaths),
                        audioPaths = MediaUtils.pathListToString(updatedAudioPaths)
                    )
                    
                    repository.insertNote(importedNote)
                    importedCount++
                }
            }
            
            tempFile.delete()
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ExportData(
    val version: Int,
    val exportDate: Date,
    val notes: List<NoteEntity>
)