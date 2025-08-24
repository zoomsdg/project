package com.dailynotes.ui.screens.note_list

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailynotes.data.NoteEntity
import com.dailynotes.data.NoteRepository
import com.dailynotes.utils.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val exportImportManager: ExportImportManager
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    val categories = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val notes = combine(
        searchQuery,
        selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repository.searchNotes(query)
            category != "all" -> repository.getNotesByCategory(category)
            else -> repository.getAllNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }
    
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
    
    suspend fun exportData(context: Context) = exportImportManager.exportData(context)
    
    suspend fun importData(context: Context, uri: Uri) = exportImportManager.importData(context, uri)
}