package com.dailynotes.ui.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dailynotes.R
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioPlayer(
    audioPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    
    LaunchedEffect(audioPath) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioPath)
            prepareAsync()
            setOnPreparedListener {
                duration = it.duration
            }
            setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
            }
        }
    }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                currentPosition = mediaPlayer?.currentPosition ?: 0
                delay(100)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            if (isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    }
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                
                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            player.seekTo(0)
                            if (player.isPlaying) {
                                player.pause()
                            }
                            isPlaying = false
                            currentPosition = 0
                        }
                    }
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
                
                Text(
                    text = File(audioPath).name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (duration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = currentPosition.toFloat() / duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}