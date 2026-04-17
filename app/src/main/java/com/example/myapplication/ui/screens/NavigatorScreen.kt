package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.Node
import com.example.myapplication.data.map.MapCoordinateTransformer
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.data.map.inkStrokesToBarrierCells
import com.example.myapplication.data.venues.CAMPUS_MAP_HEIGHT_PX
import com.example.myapplication.data.venues.CAMPUS_MAP_WIDTH_PX
import com.example.myapplication.ui.components.TGU_Blue
import com.example.myapplication.ui.components.TGU_Gold
import com.example.myapplication.features.path.StartNodeResolveStatus
import com.example.myapplication.features.path.UserLocationStartResolver
import com.example.myapplication.ui.TGU_Blue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun NavigatorScreen(
    mapData: MapData,
    venueFocusMapPosition: Offset? = null,
    onVenueFocusHandled: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val statusChooseStart = stringResource(R.string.nav_status_choose_start)
    val statusPlaceMarked = stringResource(R.string.nav_status_place_marked)
    val statusStartSelectedEndSaved = stringResource(R.string.nav_status_start_selected_end_saved)
    val statusChooseEnd = stringResource(R.string.nav_status_choose_end)
    val statusCanBuild = stringResource(R.string.nav_status_can_build)
    val statusStartUpdated = stringResource(R.string.nav_status_start_updated)
    val statusSwapped = stringResource(R.string.nav_status_swapped)
    val statusSelectStartFirst = stringResource(R.string.nav_status_select_start_first)
    val statusSelectEndNow = stringResource(R.string.nav_status_select_end_now)
    val statusSearching = stringResource(R.string.nav_status_searching)
    val statusPathNotFound = stringResource(R.string.nav_status_path_not_found)
    val drawingModeLabel = stringResource(R.string.nav_drawing_mode)
    val clearDrawingLabel = stringResource(R.string.nav_clear_drawing)
    var startNode by remember { mutableStateOf<Node?>(null) }
    var endNode by remember { mutableStateOf<Node?>(null) }
    var statusText by remember(statusChooseStart) { mutableStateOf(statusChooseStart) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var currentPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var displayedPath by remember { mutableStateOf<List<Node>>(emptyList()) }
    var openNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var closedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isControlPanelVisible by remember { mutableStateOf(false) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var inkStrokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var mapCanvasSize by remember { mutableStateOf(IntSize.Zero) }

    val barrierCells = remember(inkStrokes, mapCanvasSize, mapData.width, mapData.length) {
        if (mapCanvasSize.width == 0 || mapCanvasSize.height == 0) {
            emptySet()
        } else {
            inkStrokesToBarrierCells(inkStrokes, mapCanvasSize, mapData.width, mapData.length)
        }
    }

    val algorithm = remember { AStarAlgorithm(mapData) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            statusText = "Геопозиция недоступна. Выберите начальную точку вручную"
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                UserLocationStartResolver.resolveStartNode(context, algorithm)
            }
            when (result.status) {
                StartNodeResolveStatus.SUCCESS -> {
                    startNode = result.node
                    statusText = "Начальная точка определена автоматически, выберите конечную"
                }
                StartNodeResolveStatus.OUT_OF_MAP_BOUNDS -> {
                    statusText = "Геопозиция вне карты. Выберите начальную точку вручную"
                }
                StartNodeResolveStatus.LOCATION_UNAVAILABLE,
                StartNodeResolveStatus.PERMISSION_DENIED -> {
                    statusText = "Геопозиция недоступна. Выберите начальную точку вручную"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!UserLocationStartResolver.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@LaunchedEffect
        }

        val result = withContext(Dispatchers.Default) {
            UserLocationStartResolver.resolveStartNode(context, algorithm)
        }
        when (result.status) {
            StartNodeResolveStatus.SUCCESS -> {
                startNode = result.node
                statusText = "Начальная точка определена автоматически, выберите конечную"
            }
            StartNodeResolveStatus.OUT_OF_MAP_BOUNDS -> {
                statusText = "Геопозиция вне карты. Выберите начальную точку вручную"
            }
            StartNodeResolveStatus.LOCATION_UNAVAILABLE,
            StartNodeResolveStatus.PERMISSION_DENIED -> {
                statusText = "Геопозиция недоступна. Выберите начальную точку вручную"
            }
        }
    }

    LaunchedEffect(venueFocusMapPosition, barrierCells) {
        val pos = venueFocusMapPosition ?: return@LaunchedEffect
        val node = mapPixelOffsetToWalkableNode(mapData, pos, barrierCells) ?: return@LaunchedEffect
        searchJob?.cancel()
        isSearching = false
        startNode = null
        endNode = node
        currentPath = emptyList()
        displayedPath = emptyList()
        openNodes = emptyList()
        closedNodes = emptyList()
        statusText = statusPlaceMarked
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
            inkStrokes = inkStrokes,
            isDrawingMode = isDrawingMode && !isSearching,
            modifier = Modifier.fillMaxSize(),
            onMapClick = { node ->
                if (isSearching || isDrawingMode) return@MapSection
                val nearest = findNearestWalkable(mapData, node.x, node.y, barrierCells)
                    ?: return@MapSection
                when {
                    startNode == null -> {
                        startNode = nearest
                        statusText = if (endNode != null) {
                            statusStartSelectedEndSaved
                        } else {
                            statusChooseEnd
                        }
                    }

                    endNode == null -> {
                        endNode = nearest
                        statusText = statusCanBuild
                    }

                    else -> {
                        startNode = nearest
                        statusText = statusStartUpdated
                    }
                }
            },
            onInkStrokeStart = { offset ->
                inkStrokes = inkStrokes + listOf(listOf(offset))
            },
            onInkStrokeAppend = { point ->
                if (inkStrokes.isEmpty()) return@MapSection
                val lastStroke = inkStrokes.last()
                inkStrokes = inkStrokes.dropLast(1) + listOf(lastStroke + listOf(point))
            },
            onCanvasSizeChanged = { mapCanvasSize = it }
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
                    statusText = statusChooseStart
                },
                onSwapClick = {
                    if (!isSearching && startNode != null && endNode != null) {
                        val tmp = startNode
                        startNode = endNode
                        endNode = tmp
                        displayedPath = emptyList()
                        currentPath = emptyList()
                        statusText = statusSwapped
                    }
                },
                onBuildClick = {
                    if (isSearching) return@InputSection
                    val start = startNode ?: run {
                        statusText = statusSelectStartFirst
                        return@InputSection
                    }
                    val end = endNode ?: run {
                        statusText = statusSelectEndNow
                        return@InputSection
                    }

                    searchJob?.cancel()
                    isSearching = true
                    statusText = statusSearching
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
                                callbackStride = 10,
                                userBarriers = barrierCells
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
                                            statusText = context.getString(
                                                R.string.nav_status_path_found,
                                                currentPath.size
                                            )
                                        } else {
                                            statusText = statusPathNotFound
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

        ExtendedFloatingActionButton(
            onClick = { isDrawingMode = !isDrawingMode },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics { contentDescription = drawingModeLabel },
            containerColor = if (isDrawingMode) TGU_Gold else Color.White,
            contentColor = TGU_Blue,
            expanded = true,
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = { Text(drawingModeLabel) }
        )

        OutlinedButton(
            onClick = { inkStrokes = emptyList() },
            enabled = inkStrokes.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .semantics { contentDescription = clearDrawingLabel },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TGU_Blue,
                disabledContentColor = TGU_Blue.copy(alpha = 0.38f)
            )
        ) {
            Text(clearDrawingLabel)
        }

        FloatingActionButton(
            onClick = { isControlPanelVisible = !isControlPanelVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TGU_Blue,
            contentColor = Color.White
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = stringResource(R.string.content_desc_nav_panel)
            )
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
    inkStrokes: List<List<Offset>>,
    isDrawingMode: Boolean,
    modifier: Modifier = Modifier,
    onMapClick: (Node) -> Unit,
    onInkStrokeStart: (Offset) -> Unit,
    onInkStrokeAppend: (Offset) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit = {}
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
                .onSizeChanged {
                    contentSize = it
                    onCanvasSizeChanged(it)
                }
                .pointerInput(mapData, contentSize, isDrawingMode) {
                    if (isDrawingMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (contentSize.width == 0 || contentSize.height == 0) return@detectDragGestures
                                onInkStrokeStart(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (contentSize.width == 0 || contentSize.height == 0) return@detectDragGestures
                                onInkStrokeAppend(change.position)
                            }
                        )
                    } else {
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
                }
        ) {
            val scaleX = size.width / mapData.width.toFloat()
            val scaleY = size.height / mapData.length.toFloat()
            val penWidth = 7f / currentScale

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

            val inkStyle = Stroke(
                width = penWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            inkStrokes.forEach { stroke ->
                when {
                    stroke.isEmpty() -> Unit
                    stroke.size == 1 -> {
                        val c = stroke[0]
                        drawCircle(
                            color = TGU_Blue.copy(alpha = 0.92f),
                            radius = (penWidth * 0.55f).coerceAtLeast(1.5f),
                            center = c
                        )
                    }
                    else -> {
                        val androidPath = Path().apply {
                            moveTo(stroke[0].x, stroke[0].y)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x, stroke[i].y)
                            }
                        }
                        drawPath(
                            path = androidPath,
                            color = TGU_Blue.copy(alpha = 0.92f),
                            style = inkStyle
                        )
                    }
                }
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
    val notSelected = stringResource(R.string.nav_coord_not_selected)
    val coordA = startNode?.let { "${it.x},${it.y}" } ?: notSelected
    val coordB = endNode?.let { "${it.x},${it.y}" } ?: notSelected
    var contentOffsetY by remember { mutableStateOf(0f) }
    var viewportHeightPx by remember { mutableStateOf(0f) }
    var contentHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val extraScrollLimitPx = with(density) { 40.dp.toPx() }
    val overflowPx = (contentHeightPx - viewportHeightPx).coerceAtLeast(0f)
    val minOffset = -(overflowPx + extraScrollLimitPx)
    val maxOffset = extraScrollLimitPx

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 232.dp)
                    .clipToBounds()
                    .onSizeChanged {
                        viewportHeightPx = it.height.toFloat()
                        contentOffsetY = contentOffsetY.coerceIn(minOffset, maxOffset)
                    }
                    .pointerInput(minOffset, maxOffset) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            contentOffsetY =
                                (contentOffsetY + dragAmount).coerceIn(minOffset, maxOffset)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged {
                            contentHeightPx = it.height.toFloat()
                            contentOffsetY = contentOffsetY.coerceIn(minOffset, maxOffset)
                        }
                        .offset { IntOffset(0, contentOffsetY.roundToInt()) },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.nav_panel_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        statusText,
                        color = TGU_Blue,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    )
                    Text(stringResource(R.string.nav_coord_format, coordA))
                    Text(stringResource(R.string.nav_coord_b_format, coordB))
                    Text(
                        stringResource(R.string.nav_legend_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LegendItem(Color(0xFFFF1744), stringResource(R.string.nav_legend_start))
                        LegendItem(Color(0xFF2979FF), stringResource(R.string.nav_legend_end))
                        LegendItem(Color.Red, stringResource(R.string.nav_legend_path))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetClick,
                            enabled = !isSearching,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) { Text(stringResource(R.string.common_reset)) }

                        OutlinedButton(
                            onClick = onSwapClick,
                            enabled = !isSearching && startNode != null && endNode != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) { Text(stringResource(R.string.common_swap_ab)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBuildClick,
                enabled = !isSearching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
            ) {
                Text(
                    if (isSearching) stringResource(R.string.common_searching_upper) else stringResource(
                        R.string.common_build_route_upper
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun mapPixelOffsetToWalkableNode(
    mapData: MapData,
    offset: Offset,
    barrierCells: Set<Pair<Int, Int>> = emptySet()
): Node? {
    val gx = (offset.x / CAMPUS_MAP_WIDTH_PX * mapData.width).toInt().coerceIn(0, mapData.width - 1)
    val gy =
        (offset.y / CAMPUS_MAP_HEIGHT_PX * mapData.length).toInt().coerceIn(0, mapData.length - 1)
    return findNearestWalkable(mapData, gx, gy, barrierCells)
}

private fun findNearestWalkable(
    mapData: MapData,
    x: Int,
    y: Int,
    barrierCells: Set<Pair<Int, Int>> = emptySet()
): Node? {
    if (mapData.width == 0 || mapData.length == 0) return null

    fun isFree(cx: Int, cy: Int) =
        mapData.isAvailable(cx, cy) && (cx to cy) !in barrierCells

    val clampedX = x.coerceIn(0, mapData.width - 1)
    val clampedY = y.coerceIn(0, mapData.length - 1)
    if (isFree(clampedX, clampedY)) return Node(clampedX, clampedY)

    val maxRadius = maxOf(mapData.width, mapData.length)
    for (radius in 1..maxRadius) {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = clampedX + dx
                val ny = clampedY + dy
                if (nx !in 0 until mapData.width || ny !in 0 until mapData.length) continue
                if (isFree(nx, ny)) return Node(nx, ny)
            }
        }
    }
    return null
}