package com.dailynotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dailynotes.ui.screens.note_list.NoteListScreen
import com.dailynotes.ui.screens.note_edit.NoteEditScreen

@Composable
fun DailyNotesNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "note_list"
    ) {
        composable("note_list") {
            NoteListScreen(
                onNavigateToEdit = { noteId ->
                    navController.navigate("note_edit/$noteId")
                },
                onNavigateToAdd = {
                    navController.navigate("note_edit/0")
                }
            )
        }
        
        composable("note_edit/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
            NoteEditScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}