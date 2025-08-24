package com.dailynotes.ui.screens.note_edit

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dailynotes.R
import com.dailynotes.ui.components.AudioPlayer
import com.dailynotes.utils.MediaUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )
    
    var currentPhotoPath by remember { mutableStateOf("") }
    var currentAudioPath by remember { mutableStateOf("") }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoPath.isNotEmpty()) {
            viewModel.addImagePath(currentPhotoPath)
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = saveUriToFile(context, it)
            if (path.isNotEmpty()) {
                viewModel.addImagePath(path)
            }
        }
    }
    
    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            viewModel.loadNote(noteId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (noteId == 0L) stringResource(R.string.add_note) 
                        else stringResource(R.string.edit_note)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveNote { onNavigateBack() }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CategoryDropdown(
                selectedCategory = uiState.category,
                onCategorySelected = viewModel::updateCategory
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::updateContent,
                label = { Text(stringResource(R.string.content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            val photoFile = MediaUtils.createImageFile(context)
                            currentPhotoPath = photoFile.absolutePath
                            val photoUri = MediaUtils.getFileUri(context, photoFile)
                            cameraLauncher.launch(photoUri)
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("拍照")
                }
                
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("相册")
                }
                
                Button(
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            if (uiState.isRecording) {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                viewModel.stopRecording()
                                if (currentAudioPath.isNotEmpty()) {
                                    viewModel.addAudioPath(currentAudioPath)
                                }
                            } else {
                                val audioFile = MediaUtils.createAudioFile(context)
                                currentAudioPath = audioFile.absolutePath
                                mediaRecorder = MediaRecorder().apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setOutputFile(audioFile.absolutePath)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    prepare()
                                    start()
                                }
                                viewModel.startRecording()
                            }
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                ) {
                    Icon(
                        if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isRecording) "停止" else "录音")
                }
            }
            
            if (uiState.imagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "图片",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(uiState.imagePaths) { imagePath ->
                        ImageItem(
                            imagePath = imagePath,
                            onRemove = { viewModel.removeImagePath(imagePath) }
                        )
                    }
                }
            }
            
            if (uiState.audioPaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "音频",
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    uiState.audioPaths.forEach { audioPath ->
                        Box {
                            AudioPlayer(audioPath = audioPath)
                            IconButton(
                                onClick = { viewModel.removeAudioPath(audioPath) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("daily", "work", "travel", "mood", "other")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = when (selectedCategory) {
                "daily" -> stringResource(R.string.daily)
                "work" -> stringResource(R.string.work)
                "travel" -> stringResource(R.string.travel)
                "mood" -> stringResource(R.string.mood)
                "other" -> stringResource(R.string.other)
                else -> selectedCategory
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            when (category) {
                                "daily" -> stringResource(R.string.daily)
                                "work" -> stringResource(R.string.work)
                                "travel" -> stringResource(R.string.travel)
                                "mood" -> stringResource(R.string.mood)
                                "other" -> stringResource(R.string.other)
                                else -> category
                            }
                        ) 
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ImageItem(
    imagePath: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(100.dp)
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


private fun saveUriToFile(context: Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputFile = MediaUtils.createImageFile(context)
        outputFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        outputFile.absolutePath
    } catch (e: Exception) {
        ""
    }
}