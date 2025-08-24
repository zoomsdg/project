package com.dailynotes.ui.screens.note_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailynotes.data.NoteEntity
import com.dailynotes.data.NoteRepository
import com.dailynotes.utils.MediaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()
    
    private var originalNote: NoteEntity? = null
    
    fun loadNote(noteId: Long) {
        if (noteId == 0L) return
        
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            note?.let {
                originalNote = it
                _uiState.value = _uiState.value.copy(
                    title = it.title,
                    content = it.content,
                    category = it.category,
                    imagePaths = MediaUtils.stringToPathList(it.imagePaths),
                    audioPaths = MediaUtils.stringToPathList(it.audioPaths)
                )
            }
        }
    }
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }
    
    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }
    
    fun addImagePath(path: String) {
        val currentPaths = _uiState.value.imagePaths.toMutableList()
        currentPaths.add(path)
        _uiState.value = _uiState.value.copy(imagePaths = currentPaths)
    }
    
    fun removeImagePath(path: String) {
        val currentPaths = _uiState.value.imagePaths.toMutableList()
        currentPaths.remove(path)
        _uiState.value = _uiState.value.copy(imagePaths = currentPaths)
    }
    
    fun addAudioPath(path: String) {
        val currentPaths = _uiState.value.audioPaths.toMutableList()
        currentPaths.add(path)
        _uiState.value = _uiState.value.copy(audioPaths = currentPaths)
    }
    
    fun removeAudioPath(path: String) {
        val currentPaths = _uiState.value.audioPaths.toMutableList()
        currentPaths.remove(path)
        _uiState.value = _uiState.value.copy(audioPaths = currentPaths)
    }
    
    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }
    
    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }
    
    fun saveNote(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        
        if (currentState.title.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            val now = Date()
            val note = originalNote?.copy(
                title = currentState.title,
                content = currentState.content,
                category = currentState.category,
                updatedAt = now,
                imagePaths = MediaUtils.pathListToString(currentState.imagePaths),
                audioPaths = MediaUtils.pathListToString(currentState.audioPaths)
            ) ?: NoteEntity(
                title = currentState.title,
                content = currentState.content,
                category = currentState.category,
                createdAt = now,
                updatedAt = now,
                imagePaths = MediaUtils.pathListToString(currentState.imagePaths),
                audioPaths = MediaUtils.pathListToString(currentState.audioPaths)
            )
            
            if (originalNote == null) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
            }
            
            onSuccess()
        }
    }
}

data class NoteEditUiState(
    val title: String = "",
    val content: String = "",
    val category: String = "daily",
    val imagePaths: List<String> = emptyList(),
    val audioPaths: List<String> = emptyList(),
    val isRecording: Boolean = false
)