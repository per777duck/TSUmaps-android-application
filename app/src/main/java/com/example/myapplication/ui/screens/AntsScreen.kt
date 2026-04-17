package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.algorithms.ants.ACOParameters
import com.example.myapplication.algorithms.ants.ACOAlgorithm
import com.example.myapplication.algorithms.ants.CoworkingAntColonyAllocator
import com.example.myapplication.algorithms.ants.CoworkingAssignment
import com.example.myapplication.algorithms.ants.CoworkingCandidate
import com.example.myapplication.algorithms.clusterization.AStarMetric
import com.example.myapplication.algorithms.clusterization.Point
import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.Node
import com.example.myapplication.data.map.MapCoordinateTransformer
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering.TguMapWrapper
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.data.venues.listOfVenues
import com.example.myapplication.features.path.StartNodeResolveStatus
import com.example.myapplication.features.path.UserLocationStartResolver
import com.example.myapplication.ui.components.drawAntTrajectories
import com.example.myapplication.ui.components.TGU_Blue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AntPanelMode {
    COWORKING,
    SIGHTSEEING
}

@Composable
fun AntsScreen(mapData: MapData) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val algorithm = remember { AStarAlgorithm(mapData) }
    var isControlPanelVisible by remember { mutableStateOf(false) }
    var isBuildingRoute by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Определяем текущее местоположение...") }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var antTrajectories by remember { mutableStateOf<List<List<Point>>>(emptyList()) }
    var antTrajectoriesByIteration by remember { mutableStateOf<List<List<List<Point>>>>(emptyList()) }
    var coworkingAssignments by remember { mutableStateOf<List<CoworkingAssignment>>(emptyList()) }
    var currentIterationIndex by remember { mutableIntStateOf(0) }
    var startNode by remember { mutableStateOf<Node?>(null) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var panelMode by remember { mutableStateOf(AntPanelMode.SIGHTSEEING) }
    var studentsCountInput by remember { mutableStateOf("") }
    val antAnimation = rememberInfiniteTransition(label = "ants_route_animation")
    val antPhase by antAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3200, easing = LinearEasing)),
        label = "ant_phase"
    )
    val selectedVenueIds = remember { mutableStateListOf<Int>() }
    val availableSightseeings = remember {
        listOfVenues
            .filter { it.type == VenueType.SIGHTSEEING && it.id in 4..12 }
            .sortedBy { it.id }
    }
    val availableCoworkings = remember {
        listOfVenues
            .filter { it.type == VenueType.COWORKING }
            .sortedBy { it.id }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            statusText = "Геопозиция недоступна. Нажмите на карту, чтобы выбрать старт вручную"
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                UserLocationStartResolver.resolveStartNode(context, algorithm)
            }
            when (result.status) {
                StartNodeResolveStatus.SUCCESS -> {
                    startNode = result.node
                    statusText = "Старт определен автоматически"
                }
                StartNodeResolveStatus.OUT_OF_MAP_BOUNDS -> {
                    statusText = "Геопозиция вне карты. Нажмите на карту и выберите старт вручную"
                }
                StartNodeResolveStatus.LOCATION_UNAVAILABLE,
                StartNodeResolveStatus.PERMISSION_DENIED -> {
                    statusText = "Геопозиция недоступна. Нажмите на карту, чтобы выбрать старт вручную"
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
                statusText = "Старт определен автоматически"
            }
            StartNodeResolveStatus.OUT_OF_MAP_BOUNDS -> {
                statusText = "Геопозиция вне карты. Нажмите на карту и выберите старт вручную"
            }
            StartNodeResolveStatus.LOCATION_UNAVAILABLE,
            StartNodeResolveStatus.PERMISSION_DENIED -> {
                statusText = "Геопозиция недоступна. Нажмите на карту, чтобы выбрать старт вручную"
            }
        }
    }

    LaunchedEffect(antTrajectoriesByIteration) {
        if (antTrajectoriesByIteration.isEmpty()) return@LaunchedEffect

        for (index in antTrajectoriesByIteration.indices) {
            currentIterationIndex = index
            antTrajectories = antTrajectoriesByIteration[index]
            kotlinx.coroutines.delay(90L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        TguMapWrapper(
            mapData = mapData,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
        ) { currentScale, _, _ ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { contentSize = it }
                    .pointerInput(mapData, contentSize) {
                        detectTapGestures { tapOffset ->
                            if (contentSize.width == 0 || contentSize.height == 0) return@detectTapGestures
                            val rawNode = MapCoordinateTransformer.tapToGrid(
                                tapOffset = tapOffset,
                                canvasWidth = contentSize.width.toFloat(),
                                canvasHeight = contentSize.height.toFloat(),
                                mapData = mapData
                            )
                            val nearest = algorithm.nearestWalkable(rawNode.x, rawNode.y) ?: return@detectTapGestures
                            startNode = nearest
                            routePoints = emptyList()
                            antTrajectories = emptyList()
                            antTrajectoriesByIteration = emptyList()
                            coworkingAssignments = emptyList()
                            currentIterationIndex = 0
                            statusText = "Старт выбран вручную"
                        }
                    }
            ) {
                val scaleX = size.width / mapData.width.toFloat()
                val scaleY = size.height / mapData.length.toFloat()

                val visibleVenues = if (panelMode == AntPanelMode.COWORKING) {
                    availableCoworkings
                } else {
                    availableSightseeings
                }

                visibleVenues.forEach { venue ->
                    val center = Offset(venue.x * scaleX, venue.y * scaleY)
                    val assignment = coworkingAssignments.find { it.venueId == venue.id }
                    val isSelected = selectedVenueIds.contains(venue.id)
                    val baseColor = when {
                        panelMode == AntPanelMode.SIGHTSEEING && isSelected -> TGU_Blue
                        panelMode == AntPanelMode.COWORKING && assignment != null && assignment.assignedStudents > 0 -> Color(0xFF2E7D32)
                        else -> Color.Gray.copy(alpha = 0.5f)
                    }
                    drawCircle(
                        color = baseColor,
                        radius = if (isSelected) 9f / currentScale.coerceAtLeast(1f) else 6f / currentScale.coerceAtLeast(1f),
                        center = center
                    )
                }

                if (routePoints.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(
                            (routePoints.first().x.toFloat()) * scaleX,
                            (routePoints.first().y.toFloat()) * scaleY
                        )
                        routePoints.drop(1).forEach { point ->
                            lineTo(
                                point.x.toFloat() * scaleX,
                                point.y.toFloat() * scaleY
                            )
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Red,
                        style = Stroke(width = 5f / currentScale.coerceAtLeast(1f))
                    )
                }

                drawAntTrajectories(
                    trajectories = antTrajectories,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    currentScale = currentScale,
                    phase = antPhase
                )

                startNode?.let { node ->
                    drawCircle(
                        color = Color(0xFFFF1744),
                        radius = 9f / currentScale.coerceAtLeast(1f),
                        center = Offset(node.x * scaleX, node.y * scaleY)
                    )
                }
            }
        }

        if (isControlPanelVisible) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (panelMode == AntPanelMode.COWORKING) "Распределение по коворкингам" else "Выбрать достопримечательности",
                        style = MaterialTheme.typography.titleMedium,
                        color = TGU_Blue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                panelMode = AntPanelMode.COWORKING
                                selectedVenueIds.clear()
                                routePoints = emptyList()
                                antTrajectories = emptyList()
                                antTrajectoriesByIteration = emptyList()
                                coworkingAssignments = emptyList()
                                currentIterationIndex = 0
                                statusText = "Режим коворкинга: введите количество студентов"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (panelMode == AntPanelMode.COWORKING) TGU_Blue else Color.Gray
                            )
                        ) {
                            Text(text = "Выбрать коворкинг", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(0.dp).weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                panelMode = AntPanelMode.SIGHTSEEING
                                routePoints = emptyList()
                                antTrajectories = emptyList()
                                antTrajectoriesByIteration = emptyList()
                                coworkingAssignments = emptyList()
                                currentIterationIndex = 0
                                statusText = "Выберите минимум 2 достопримечательности"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (panelMode == AntPanelMode.SIGHTSEEING) TGU_Blue else Color.Gray
                            )
                        ) {
                            Text(text = "Выбрать достопримечательности", color = Color.White)
                        }
                    }

                    if (antTrajectoriesByIteration.isNotEmpty()) {
                        Text(
                            text = "Итерация: ${currentIterationIndex + 1}/${antTrajectoriesByIteration.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TGU_Blue
                        )
                    }

                    if (panelMode == AntPanelMode.COWORKING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Введите количество студентов:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TGU_Blue
                        )
                        OutlinedTextField(
                            value = studentsCountInput,
                            onValueChange = { input ->
                                studentsCountInput = input.filter { it.isDigit() }
                            },
                            singleLine = true,
                            label = { Text("Количество студентов") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val start = startNode
                                if (start == null) {
                                    statusText = "Сначала определите старт (авто или тап по карте)"
                                    return@Button
                                }
                                val studentsCount = studentsCountInput.toIntOrNull()
                                if (studentsCount == null || studentsCount <= 0) {
                                    statusText = "Введите корректное количество студентов"
                                    return@Button
                                }
                                if (availableCoworkings.isEmpty()) {
                                    statusText = "В списке нет коворкингов для распределения"
                                    return@Button
                                }

                                val startPoint = Point(
                                    id = -1,
                                    x = start.x.toDouble(),
                                    y = start.y.toDouble()
                                )
                                val coworkingCandidates = availableCoworkings.map { venue ->
                                    CoworkingCandidate(
                                        venueId = venue.id,
                                        name = venue.name,
                                        capacity = venue.capacity ?: 0,
                                        point = Point(
                                            id = venue.id,
                                            x = venue.x.toDouble(),
                                            y = venue.y.toDouble()
                                        )
                                    )
                                }

                                scope.launch {
                                    isBuildingRoute = true
                                    statusText = "Распределяем поток студентов муравьиным алгоритмом..."
                                    routePoints = emptyList()
                                    antTrajectories = emptyList()
                                    antTrajectoriesByIteration = emptyList()
                                    coworkingAssignments = emptyList()
                                    currentIterationIndex = 0
                                    try {
                                        val result = withContext(Dispatchers.Default) {
                                            val metric = AStarMetric(algorithm)
                                            CoworkingAntColonyAllocator().run(
                                                startPoint = startPoint,
                                                coworkings = coworkingCandidates,
                                                studentsCount = studentsCount,
                                                metric = metric
                                            )
                                        }
                                        antTrajectories = result.antTrajectories
                                        antTrajectoriesByIteration = result.antTrajectoriesByIteration
                                        coworkingAssignments = result.assignments
                                            .sortedByDescending { it.assignedStudents }
                                        val allocationSuffix = if (result.unassignedStudents > 0) {
                                            " Нераспределено: ${result.unassignedStudents}."
                                        } else {
                                            ""
                                        }
                                        statusText = "Распределение готово. Суммарный путь: %.1f.%s"
                                            .format(result.totalDistance, allocationSuffix)
                                    } catch (e: Exception) {
                                        antTrajectories = emptyList()
                                        antTrajectoriesByIteration = emptyList()
                                        coworkingAssignments = emptyList()
                                        currentIterationIndex = 0
                                        statusText = e.message ?: "Не удалось распределить студентов"
                                    } finally {
                                        isBuildingRoute = false
                                    }
                                }
                            },
                            enabled = !isBuildingRoute,
                            colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
                        ) {
                            Text(
                                text = if (isBuildingRoute) "РАСЧЕТ..." else "РАСПРЕДЕЛИТЬ СТУДЕНТОВ",
                                color = Color.White
                            )
                        }

                        if (coworkingAssignments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Распределение:",
                                style = MaterialTheme.typography.titleSmall,
                                color = TGU_Blue
                            )
                            coworkingAssignments.forEach { assignment ->
                                Text(
                                    text = "${assignment.venueName}: ${assignment.assignedStudents}/${assignment.capacity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    } else {
                        availableSightseeings.forEach { venue ->
                            val isSelected = selectedVenueIds.contains(venue.id)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedVenueIds.contains(venue.id)) {
                                                selectedVenueIds.add(venue.id)
                                            }
                                        } else {
                                            selectedVenueIds.remove(venue.id)
                                        }
                                        routePoints = emptyList()
                                        antTrajectories = emptyList()
                                        antTrajectoriesByIteration = emptyList()
                                        coworkingAssignments = emptyList()
                                        currentIterationIndex = 0
                                    }
                                )
                                Text(
                                    text = "${venue.id}. ${venue.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (selectedVenueIds.size < 2) {
                                    statusText = "Нужно выбрать минимум 2 достопримечательности"
                                    return@Button
                                }
                                val start = startNode
                                if (start == null) {
                                    statusText = "Сначала определите старт (авто или тап по карте)"
                                    return@Button
                                }

                                val selectedPoints = availableSightseeings
                                    .filter { selectedVenueIds.contains(it.id) }
                                    .map { Point(id = it.id, x = it.x.toDouble(), y = it.y.toDouble()) }
                                val routeInputPoints = listOf(
                                    Point(id = -1, x = start.x.toDouble(), y = start.y.toDouble())
                                ) + selectedPoints

                                scope.launch {
                                    isBuildingRoute = true
                                    statusText = "Строим маршрут муравьиным алгоритмом..."
                                    routePoints = emptyList()
                                    antTrajectories = emptyList()
                                    antTrajectoriesByIteration = emptyList()
                                    coworkingAssignments = emptyList()
                                    currentIterationIndex = 0
                                    try {
                                        val result = withContext(Dispatchers.Default) {
                                            val metric = AStarMetric(algorithm)
                                            ACOAlgorithm(
                                                ACOParameters(
                                                    returnToStart = false,
                                                    fixedStartIndex = 0
                                                )
                                            ).run(routeInputPoints, metric)
                                        }
                                        routePoints = result.orderedPoints
                                        antTrajectories = result.antTrajectories
                                        antTrajectoriesByIteration = result.antTrajectoriesByIteration
                                        statusText = "Маршрут построен. Длина: %.1f".format(result.distance)
                                    } catch (e: Exception) {
                                        antTrajectories = emptyList()
                                        antTrajectoriesByIteration = emptyList()
                                        currentIterationIndex = 0
                                        statusText = e.message ?: "Не удалось построить маршрут"
                                    } finally {
                                        isBuildingRoute = false
                                    }
                                }
                            },
                            enabled = !isBuildingRoute,
                            colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
                        ) {
                            Text(
                                text = if (isBuildingRoute) "ПОИСК..." else "ПОСТРОИТЬ МАРШРУТ",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isControlPanelVisible = !isControlPanelVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TGU_Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Выбор достопримечательностей")
        }
    }
}
