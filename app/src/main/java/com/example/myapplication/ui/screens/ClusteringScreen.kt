package com.example.myapplication.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering.TguMapWrapper
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.features.clustering.ClusteringCoordinator
import com.example.myapplication.ui.components.ClusterColors
import com.example.myapplication.ui.components.FilterSettingsContent
import com.example.myapplication.ui.TGU_Blue
import com.example.myapplication.features.clustering.ClusterAlgorithmType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClusteringScreen(
    venues: List<Venue>,
    selectedType: VenueType?,
    selectedMetric: MetricType?,
    clusterCount: Int,
    isComparisonMode: Boolean = false,
    mapData: MapData,
    onVenueTypeChange: (VenueType?) -> Unit,
    onMetricChange: (MetricType) -> Unit,
    onClusterCountChange: (Int) -> Unit,
    onComparisonModeChange: (Boolean) -> Unit
) {
    var primaryClusters by remember { mutableStateOf<List<Pair<Venue, Int>>>(emptyList()) }
    var secondaryClusters by remember { mutableStateOf<List<Pair<Venue, Int>>>(emptyList()) }
    var selectedAlgorithm by remember { mutableStateOf(ClusterAlgorithmType.KMEANS) }
    var isControlPanelVisible by remember { mutableStateOf(false) }

    LaunchedEffect(
        venues,
        selectedMetric,
        selectedType,
        selectedAlgorithm,
        clusterCount,
        isComparisonMode
    ) {
        val (primary, secondary) = withContext(Dispatchers.Default) {
            ClusteringCoordinator.findingClusters(
                venues = venues,
                selectedType = selectedType,
                selectedMetric = selectedMetric,
                selectedAlgorithm = selectedAlgorithm,
                clusterCount = clusterCount,
                isComparisonMode = isComparisonMode,
                mapData = mapData
            )
        }
        primaryClusters = primary
        secondaryClusters = secondary
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        TguMapWrapper(
            mapData = mapData,
            modifier = Modifier.fillMaxSize()
        ) { currentScale, _, _ ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / mapData.width.toFloat()
                val scaleY = size.height / mapData.length.toFloat()

                primaryClusters.forEach { (venue, clusterId) ->
                    val color1 = if (clusterId != -1) {
                        ClusterColors[clusterId % ClusterColors.size]
                    } else {
                        Color.Gray
                    }
                    val venueCenter = Offset(
                        x = venue.x * scaleX,
                        y = venue.y * scaleY
                    )

                    if (isComparisonMode) {
                        val clusterId2 = secondaryClusters.find { it.first.id == venue.id }?.second ?: -1
                        val color2 = if (clusterId2 != -1) {
                            ClusterColors[clusterId2 % ClusterColors.size]
                        } else {
                            Color.Gray
                        }

                        drawCircle(
                            color = color2,
                            radius = 22f / currentScale.coerceAtLeast(1f),
                            center = venueCenter,
                            style = Stroke(width = 6f / currentScale.coerceAtLeast(1f))
                        )
                        drawCircle(
                            color = color1,
                            radius = 12f / currentScale.coerceAtLeast(1f),
                            center = venueCenter
                        )
                    } else {
                        drawCircle(
                            color = color1,
                            radius = 15f / currentScale.coerceAtLeast(1f),
                            center = venueCenter
                        )
                    }
                }
            }
        }

        if (isControlPanelVisible) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.8f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                FilterSettingsContent(
                    selectedType = selectedType,
                    onVenueTypeChange = onVenueTypeChange,
                    selectedMetric = selectedMetric ?: MetricType.EUCLIDEAN,
                    onMetricChange = onMetricChange,
                    selectedAlgorithm = selectedAlgorithm,
                    onAlgorithmChange = { selectedAlgorithm = it },
                    clusterCount = clusterCount,
                    onClusterCountChange = onClusterCountChange,
                    isComparisonMode = isComparisonMode,
                    onComparisonModeChange = onComparisonModeChange
                )
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
            Icon(Icons.Default.FilterList, contentDescription = "Фильтры кластеризации")
        }
    }
}
