package com.example.myapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.myapplication.algorithms.ACOParameters
import com.example.myapplication.algorithms.ACOAlgorithm
import com.example.myapplication.algorithms.AStarAlgorithm
import com.example.myapplication.algorithms.Node
import com.example.myapplication.algorithms.models.AStarMetric
import com.example.myapplication.algorithms.models.Point
import com.example.myapplication.data.map.MapCoordinateTransformer
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering.TguMapWrapper
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.data.venues.listOfVenues
import com.example.myapplication.features.path.StartNodeResolveStatus
import com.example.myapplication.features.path.UserLocationStartResolver
import com.example.myapplication.ui.TGU_Blue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AntsScreen(mapData: MapData) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val algorithm = remember { AStarAlgorithm(mapData) }
    var isControlPanelVisible by remember { mutableStateOf(false) }
    var isBuildingRoute by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Определяем текущее местоположение...") }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var startNode by remember { mutableStateOf<Node?>(null) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val selectedVenueIds = remember { mutableStateListOf<Int>() }
    val availableSightseeings = remember {
        listOfVenues
            .filter { it.type == VenueType.SIGHTSEEING && it.id in 4..12 }
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
                    statusText = "Старт определен автоматически. Выберите минимум 2 достопримечательности"
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
                statusText = "Старт определен автоматически. Выберите минимум 2 достопримечательности"
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
                            statusText = "Старт выбран вручную. Выберите минимум 2 достопримечательности"
                        }
                    }
            ) {
                val scaleX = size.width / mapData.width.toFloat()
                val scaleY = size.height / mapData.length.toFloat()

                availableSightseeings.forEach { venue ->
                    val center = Offset(venue.x * scaleX, venue.y * scaleY)
                    val isSelected = selectedVenueIds.contains(venue.id)
                    drawCircle(
                        color = if (isSelected) TGU_Blue else Color.Gray.copy(alpha = 0.5f),
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
                        text = "Выбрать достопримечательности",
                        style = MaterialTheme.typography.titleMedium,
                        color = TGU_Blue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

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
                                    statusText = "Маршрут построен. Длина: %.1f".format(result.distance)
                                } catch (e: Exception) {
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
