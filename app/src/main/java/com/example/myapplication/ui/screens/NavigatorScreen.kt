package com.example.myapplication.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.algorithms.AStarAlgorithm
import com.example.myapplication.algorithms.Node
import com.example.myapplication.data.map.MapCoordinateTransformer
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.data.venues.CAMPUS_MAP_HEIGHT_PX
import com.example.myapplication.data.venues.CAMPUS_MAP_WIDTH_PX
import com.example.myapplication.ui.TGU_Blue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NavigatorScreen(
    mapData: MapData,
    venueFocusMapPosition: Offset? = null,
    onVenueFocusHandled: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var startNode by remember { mutableStateOf<Node?>(null) }
    var endNode by remember { mutableStateOf<Node?>(null) }
    var statusText by remember { mutableStateOf("Выберите начальную точку") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var currentPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var displayedPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var openNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var closedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isControlPanelVisible by remember { mutableStateOf(false) }

    val algorithm = remember { AStarAlgorithm(mapData) }

    LaunchedEffect(venueFocusMapPosition) {
        val pos = venueFocusMapPosition ?: return@LaunchedEffect
        val node = mapPixelOffsetToWalkableNode(mapData, pos) ?: return@LaunchedEffect
        searchJob?.cancel()
        isSearching = false
        startNode = null
        endNode = node
        currentPath = emptyList()
        displayedPath = emptyList()
        openNodes = emptyList()
        closedNodes = emptyList()
        statusText = "Место из подсказки отмечено синей точкой. Выберите начало маршрута на карте."
        isControlPanelVisible = true
        onVenueFocusHandled()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapSection(
            mapData = mapData,
            path = displayedPath,
            openSet = openNodes,
            closedSet = closedNodes,
            start = startNode,
            end = endNode,
            modifier = Modifier.fillMaxSize(),
            onMapClick = { node ->
                if (isSearching) return@MapSection
                val nearest = findNearestWalkable(mapData, node.x, node.y) ?: return@MapSection
                when {
                    startNode == null -> {
                        startNode = nearest
                        statusText = if (endNode != null) {
                            "Старт выбран. Конечная точка сохранена (синяя). Можно строить путь."
                        } else {
                            "Начальная точка выбрана, выберите конечную"
                        }
                    }
                    endNode == null -> {
                        endNode = nearest
                        statusText = "Можно запускать поиск маршрута"
                    }
                    else -> {
                        // Keep focused destination; repeated tap updates only start point.
                        startNode = nearest
                        statusText = "Старт обновлён, конечная точка сохранена (синяя)."
                    }
                }
            }
        )

        if (isControlPanelVisible) {
            InputSection(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                startNode = startNode,
                endNode = endNode,
                statusText = statusText,
                isSearching = isSearching,
                onResetClick = {
                    searchJob?.cancel()
                    isSearching = false
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

        FloatingActionButton(
            onClick = { isControlPanelVisible = !isControlPanelVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TGU_Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Панель навигации")
        }
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

    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    MapRendering.TguMapWrapper(
        mapData = mapData,
        modifier = modifier.clip(MaterialTheme.shapes.extraLarge)
    ) { currentScale, _, _ ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { contentSize = it }
                .pointerInput(mapData, contentSize) {
                    detectTapGestures { tapOffset ->
                        if (contentSize.width == 0 || contentSize.height == 0) return@detectTapGestures
                        val node = MapCoordinateTransformer.tapToGrid(
                            tapOffset = tapOffset,
                            canvasWidth = contentSize.width.toFloat(),
                            canvasHeight = contentSize.height.toFloat(),
                            mapData = mapData
                        )
                        onMapClick(node)
                    }
                }
        ) {
            val scaleX = size.width / mapData.width.toFloat()
            val scaleY = size.height / mapData.length.toFloat()

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

@Composable
fun InputSection(
    modifier: Modifier = Modifier,
    startNode: Node?,
    endNode: Node?,
    statusText: String,
    isSearching: Boolean,
    onResetClick: () -> Unit,
    onSwapClick: () -> Unit,
    onBuildClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
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
            Text(
                "Легенда: красная — старт, синяя — цель, красная линия — маршрут.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(Color(0xFFFF1744), "Старт (A)")
                LegendItem(Color(0xFF2979FF), "Цель (B)")
                LegendItem(Color.Red, "Путь")
            }

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

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun mapPixelOffsetToWalkableNode(mapData: MapData, offset: Offset): Node? {
    val gx = (offset.x / CAMPUS_MAP_WIDTH_PX * mapData.width).toInt().coerceIn(0, mapData.width - 1)
    val gy = (offset.y / CAMPUS_MAP_HEIGHT_PX * mapData.length).toInt().coerceIn(0, mapData.length - 1)
    return findNearestWalkable(mapData, gx, gy)
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