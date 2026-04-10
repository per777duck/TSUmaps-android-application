package com.example.myapplication.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.AStarAlgorithm
import com.example.myapplication.Node
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.ui.TGU_Blue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NavigatorScreen(mapData: MapData) {
    val scope = rememberCoroutineScope()
    var startNode by remember { mutableStateOf<Node?>(null) }
    var endNode by remember { mutableStateOf<Node?>(null) }
    var pickStartNext by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Выберите начальную точку") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var currentPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var displayedPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var openNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var closedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }

    val algorithm = remember { AStarAlgorithm(mapData) }

    Column(modifier = Modifier.fillMaxSize()) {
        MapSection(
            mapData = mapData,
            path = displayedPath,
            openSet = openNodes,
            closedSet = closedNodes,
            start = startNode,
            end = endNode,
            modifier = Modifier.weight(1f),
            onMapClick = { node ->
                if (isSearching) return@MapSection
                val nearest = findNearestWalkable(mapData, node.x, node.y) ?: return@MapSection
                if (pickStartNext) {
                    startNode = nearest
                    pickStartNext = false
                    statusText = "Начальная точка выбрана, выберите конечную"
                } else {
                    endNode = nearest
                    pickStartNext = true
                    statusText = "Можно запускать поиск маршрута"
                }
            }
        )

        InputSection(
            startNode = startNode,
            endNode = endNode,
            statusText = statusText,
            isSearching = isSearching,
            onResetClick = {
                searchJob?.cancel()
                isSearching = false
                pickStartNext = true
                startNode = null
                endNode = null
                currentPath = emptyList()
                displayedPath = emptyList()
                openNodes = emptyList()
                closedNodes = emptyList()
                statusText = "Выберите начальную точку"
            },
            onSwapClick = {
                if (!isSearching && startNode != null && endNode != null) {
                    val tmp = startNode
                    startNode = endNode
                    endNode = tmp
                    displayedPath = emptyList()
                    currentPath = emptyList()
                    statusText = "Точки поменяны местами"
                }
            },
            onBuildClick = {
                if (isSearching) return@InputSection
                val start = startNode ?: run {
                    statusText = "Сначала выберите начальную точку"
                    return@InputSection
                }
                val end = endNode ?: run {
                    statusText = "Теперь выберите конечную точку"
                    return@InputSection
                }

                searchJob?.cancel()
                isSearching = true
                statusText = "Идёт поиск маршрута..."
                currentPath = emptyList()
                displayedPath = emptyList()
                openNodes = emptyList()
                closedNodes = emptyList()

                searchJob = scope.launch {
                    withContext(Dispatchers.Default) {
                        algorithm.findPath(
                            start = start,
                            end = end,
                            speedMs = 2L,
                            maxDurationMs = 10_000L,
                            callbackStride = 10
                        ) { state ->
                            scope.launch {
                                openNodes = state.openSet
                                closedNodes = state.closedSet
                                if (state.finished) {
                                    if (state.fullPath != null) {
                                        currentPath = state.fullPath
                                        openNodes = emptyList()
                                        closedNodes = emptyList()

                                        displayedPath = emptyList()
                                        for (i in 1..currentPath.size) {
                                            displayedPath = currentPath.take(i)
                                            delay(8L)
                                        }
                                        statusText = "Путь найден: ${currentPath.size} точек"
                                    } else {
                                        statusText = "Путь не найден или превышен лимит 10 сек"
                                    }
                                    isSearching = false
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MapSection(
    mapData: MapData,
    path: List<Node>,
    openSet: List<Node>,
    closedSet: List<Node>,
    start: Node?,
    end: Node?,
    modifier: Modifier = Modifier,
    onMapClick: (Node) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_spread")
    val openPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openPulse"
    )

    val mapShape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = mapShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        MapRendering.TguMapWrapper(modifier = Modifier.fillMaxSize()) { currentScale, _, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            if (containerSize.width == 0 || containerSize.height == 0) return@detectTapGestures
                            val gridX = (tapOffset.x / containerSize.width * mapData.width).toInt()
                                .coerceIn(0, mapData.width - 1)
                            val gridY = (tapOffset.y / containerSize.height * mapData.length).toInt()
                                .coerceIn(0, mapData.length - 1)
                            onMapClick(Node(gridX, gridY))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / mapData.width
                    val scaleY = size.height / mapData.length

                    closedSet.takeLast(4000).forEach { node ->
                        drawCircle(
                            color = Color(0xFFFFA726).copy(alpha = 0.38f),
                            radius = (2.2f / currentScale).coerceAtLeast(0.9f),
                            center = Offset(node.x * scaleX, node.y * scaleY)
                        )
                    }

                    openSet.takeLast(2000).forEach { node ->
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = openPulse),
                            radius = (2.8f / currentScale).coerceAtLeast(1.1f),
                            center = Offset(node.x * scaleX, node.y * scaleY)
                        )
                    }

                    if (path.isNotEmpty()) {
                        val androidPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(path[0].x * scaleX, path[0].y * scaleY)
                            path.forEach { node ->
                                lineTo(node.x * scaleX, node.y * scaleY)
                            }
                        }
                        drawPath(
                            path = androidPath,
                            color = Color.Red,
                            style = Stroke(width = 5f / currentScale)
                        )
                    }

                    start?.let { node ->
                        drawCircle(
                            color = Color(0xFFFF1744),
                            radius = (9.5f / currentScale).coerceAtLeast(3.8f),
                            center = Offset(node.x * scaleX, node.y * scaleY)
                        )
                    }

                    end?.let { node ->
                        drawCircle(
                            color = Color(0xFF2979FF),
                            radius = (9.5f / currentScale).coerceAtLeast(3.8f),
                            center = Offset(node.x * scaleX, node.y * scaleY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputSection(
    startNode: Node?,
    endNode: Node?,
    statusText: String,
    isSearching: Boolean,
    onResetClick: () -> Unit,
    onSwapClick: () -> Unit,
    onBuildClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Навигация по карте", style = MaterialTheme.typography.titleMedium)
            Text(
                statusText,
                color = TGU_Blue,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(22.dp)
            )
            Text("A: ${startNode?.let { "${it.x},${it.y}" } ?: "не выбрано"}")
            Text("B: ${endNode?.let { "${it.x},${it.y}" } ?: "не выбрано"}")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetClick,
                    enabled = !isSearching,
                    modifier = Modifier.weight(1f).height(46.dp)
                ) { Text("Сброс") }

                OutlinedButton(
                    onClick = onSwapClick,
                    enabled = !isSearching && startNode != null && endNode != null,
                    modifier = Modifier.weight(1f).height(46.dp)
                ) { Text("A ↔ B") }
            }

            Button(
                onClick = onBuildClick,
                enabled = !isSearching,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
            ) {
                Text(if (isSearching) "ПОИСК..." else "ПОСТРОИТЬ ПУТЬ", color = Color.White)
            }
        }
    }
}

private fun findNearestWalkable(mapData: MapData, x: Int, y: Int): Node? {
    if (mapData.width == 0 || mapData.length == 0) return null
    val clampedX = x.coerceIn(0, mapData.width - 1)
    val clampedY = y.coerceIn(0, mapData.length - 1)
    if (mapData.isAvailable(clampedX, clampedY)) return Node(clampedX, clampedY)

    val maxRadius = maxOf(mapData.width, mapData.length)
    for (radius in 1..maxRadius) {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = clampedX + dx
                val ny = clampedY + dy
                if (nx !in 0 until mapData.width || ny !in 0 until mapData.length) continue
                if (mapData.isAvailable(nx, ny)) return Node(nx, ny)
            }
        }
    }
    return null
}
