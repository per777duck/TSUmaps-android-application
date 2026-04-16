package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.ui.TGU_Blue
import com.example.myapplication.ui.TGU_Gold

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsContent(
    selectedType: VenueType?,
    onVenueTypeChange: (VenueType?) -> Unit,
    selectedMetric: MetricType,
    onMetricChange: (MetricType) -> Unit,
    clusterCount: Int,
    onClusterCountChange: (Int) -> Unit,
    isComparisonMode: Boolean,
    onComparisonModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("Настройки кластеризации", style = MaterialTheme.typography.headlineSmall, color = TGU_Blue)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Тип заведений:", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onVenueTypeChange(null) },
                label = { Text("Все") }
            )
            VenueType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onVenueTypeChange(type) },
                    label = {
                        when (type) {
                            VenueType.FOOD -> Text("Поесть")
                            VenueType.COWORKING -> Text("Рабочая зона")
                            VenueType.SIGHTSEEING -> Text("Достопримечетельности")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Метрика расстояния:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricType.entries.forEach { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { onMetricChange(metric) },
                    label = {
                        when (metric) {
                            MetricType.EUCLIDEAN -> Text("По прямой")
                            MetricType.ASTAR -> Text("По дорожам")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text("Количество кластеров: $clusterCount", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = clusterCount.toFloat(),
            onValueChange = { onClusterCountChange(it.toInt().coerceIn(2, 10)) },
            valueRange = 2f..10f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onComparisonModeChange(!isComparisonMode) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isComparisonMode) TGU_Gold else TGU_Blue
            )
        ) {
            Icon(Icons.Default.Compare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isComparisonMode) "Выключить сравнение" else "Сравнить Евклид vs A*")
        }
    }
}

val ClusterColors = listOf(
    Color(0xFFEF5350),
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFFFEE58),
    Color(0xFFAB47BC),
    Color(0xFFFFA726),
    Color(0xFF26C6DA),
    Color(0xFF78909C),
    Color(0xFF8D6E63),
    Color(0xFFEC407A),
    Color(0xFF26A69A),
    Color(0xFFD4E157),
    Color(0xFF5C6BC0),
    Color(0xFFFF7043),
    Color(0xFF9CCC65)
)
