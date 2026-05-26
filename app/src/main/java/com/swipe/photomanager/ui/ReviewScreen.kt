package com.swipe.photomanager.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swipe.photomanager.data.MediaType
import com.swipe.photomanager.data.PhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: PhotoViewModel) {
    val allItems by viewModel.currentMonthItems.collectAsState()
    val itemsToDelete by viewModel.itemsToDelete.collectAsState()
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.finishReview()
            viewModel.loadMedia()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisão do Mês") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setFinished(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (itemsToDelete.isNotEmpty()) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // Android 11+
                                val pendingIntent = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    itemsToDelete.map { it.uri }
                                )
                                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                            } else {
                                // Android 10 e anteriores
                                itemsToDelete.forEach { item ->
                                    try {
                                        context.contentResolver.delete(item.uri, null, null)
                                    } catch (securityException: SecurityException) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                                            securityException is RecoverableSecurityException) {
                                            val intentSender = securityException.userAction.actionIntent.intentSender
                                            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                            return@Button
                                        } else {
                                            throw securityException
                                        }
                                    }
                                }
                                viewModel.finishReview()
                                viewModel.loadMedia()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        viewModel.finishReview()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (itemsToDelete.isNotEmpty()) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (itemsToDelete.isNotEmpty()) "Confirmar Exclusão (${itemsToDelete.size})"
                    else "Finalizar Revisão"
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                "Toque na foto para alternar entre manter/deletar",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(allItems) { item ->
                    val isMarkedForDeletion = itemsToDelete.contains(item)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(2.dp)
                            .clickable { viewModel.toggleDeletion(item) }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            alpha = if (isMarkedForDeletion) 0.5f else 1f
                        )
                        if (item.type == MediaType.VIDEO) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                            )
                        }
                        if (isMarkedForDeletion) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Deletar",
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
