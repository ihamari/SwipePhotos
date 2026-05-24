package com.swipe.photomanager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swipe.photomanager.data.MediaItem
import com.swipe.photomanager.data.MediaType
import com.swipe.photomanager.data.PhotoViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(viewModel: PhotoViewModel) {
    val items by viewModel.currentMonthItems.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(month ?: "") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectMonth(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (currentIndex > 0) {
                        IconButton(onClick = { currentIndex-- }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Desfazer")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (items.isNotEmpty()) {
                // Previews area at the top
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Previous
                    if (currentIndex > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Anterior", fontSize = 10.sp)
                            AsyncImage(
                                model = items[currentIndex - 1].uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Box(modifier = Modifier.size(20.dp)) // Spacer

                    // Preview Next
                    if (currentIndex + 1 < items.size) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Próxima", fontSize = 10.sp)
                            AsyncImage(
                                model = items[currentIndex + 1].uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (currentIndex < items.size) {
                    // Background card (Next)
                    val nextIndex = currentIndex + 1
                    if (nextIndex < items.size) {
                        key(items[nextIndex].id) {
                            SwipeCard(item = items[nextIndex], onSwiped = {}, isNext = true)
                        }
                    }

                    // Foreground card (Current)
                    key(items[currentIndex].id) {
                        SwipeCard(
                            item = items[currentIndex],
                            onSwiped = { direction ->
                                if (direction == SwipeDirection.LEFT) {
                                    viewModel.markForDeletion(items[currentIndex])
                                } else {
                                    viewModel.markToKeep(items[currentIndex])
                                }
                                currentIndex++
                                if (currentIndex >= items.size) {
                                    viewModel.setFinished(true)
                                }
                            },
                            isNext = false
                        )
                    }
                } else {
                    Text("Fim do mês!")
                    viewModel.setFinished(true)
                }
            } else {
                Text("Nenhuma foto encontrada")
            }
        }
    }
}

enum class SwipeDirection { LEFT, RIGHT }

@Composable
fun SwipeCard(
    item: MediaItem,
    onSwiped: (SwipeDirection) -> Unit,
    isNext: Boolean
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val swipeThreshold = 100.dp
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxSize(0.75f) // Reduced size to show previews above
            .offset(y = 40.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = offsetX.value / 20f
                alpha = if (isNext) 0.6f else 1f
                scaleX = if (isNext) 0.9f else 1f
                scaleY = if (isNext) 0.9f else 1f
            }
            .pointerInput(isNext) {
                if (isNext) return@pointerInput
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > swipeThreshold.toPx()) {
                                offsetX.animateTo(screenWidth.toPx() * 2, tween(300))
                                onSwiped(SwipeDirection.RIGHT)
                            } else if (offsetX.value < -swipeThreshold.toPx()) {
                                offsetX.animateTo(-screenWidth.toPx() * 2, tween(300))
                                onSwiped(SwipeDirection.LEFT)
                            } else {
                                offsetX.animateTo(0f, tween(300))
                            }
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.type == MediaType.VIDEO && !isNext) {
                VideoPlayer(uri = item.uri, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (offsetX.value > 50f) {
                Text(
                    "MANTER",
                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                    color = Color.Green,
                    fontSize = 32.sp,
                    style = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
            } else if (offsetX.value < -50f) {
                Text(
                    "DELETAR",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
                    color = Color.Red,
                    fontSize = 32.sp,
                    style = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
            }
        }
    }
}
