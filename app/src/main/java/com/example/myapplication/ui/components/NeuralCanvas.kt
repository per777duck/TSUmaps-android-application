package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.venues.FoodVenue
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.foodVenues
import com.example.myapplication.data.venues.listOfVenues
import kotlin.math.floor
import kotlin.math.min

private const val GridSize = 5
private const val TotalPixels = GridSize * GridSize

private data class PlaceOption(
    val key: String,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralCanvas(
    modifier: Modifier = Modifier,
    foodVenueOptions: List<FoodVenue> = foodVenues,
    venueOptions: List<Venue> = listOfVenues
) {
    val options = remember(foodVenueOptions, venueOptions) {
        buildList {
            foodVenueOptions.forEach { venue ->
                add(
                    PlaceOption(
                        key = "food-${venue.id}",
                        title = venue.name
                    )
                )
            }
            venueOptions.forEach { venue ->
                add(
                    PlaceOption(
                        key = "venue-${venue.id}",
                        title = venue.name
                    )
                )
            }
        }
    }

    var selectedPlace by remember(options) { mutableStateOf(options.firstOrNull()) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var pixels by remember { mutableStateOf(List(TotalPixels) { false }) }
    var toggledInGesture by remember { mutableStateOf(emptySet<Int>()) }

    fun indexFromOffset(touch: Offset, sizePx: Float): Int? {
        if (sizePx <= 0f) return null
        val cellSize = sizePx / GridSize
        val column = floor(touch.x / cellSize).toInt()
        val row = floor(touch.y / cellSize).toInt()
        if (row !in 0 until GridSize || column !in 0 until GridSize) return null
        return row * GridSize + column
    }

    fun togglePixel(index: Int) {
        pixels = pixels.toMutableList().also { current ->
            current[index] = !current[index]
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = isMenuExpanded,
            onExpandedChange = { isMenuExpanded = !isMenuExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedPlace?.title ?: "Нет заведений",
                onValueChange = {},
                label = { Text("Выберите заведение") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) }
            )

            ExposedDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title) },
                        onClick = {
                            selectedPlace = option
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Закрашено пикселей: ${pixels.count { it }}/$TotalPixels",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6B7A)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF7F9FC)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val sizePx = min(size.width.toFloat(), size.height.toFloat())
                            val normalizedOffset = Offset(
                                tapOffset.x.coerceIn(0f, sizePx),
                                tapOffset.y.coerceIn(0f, sizePx)
                            )
                            indexFromOffset(normalizedOffset, sizePx)?.let(::togglePixel)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragStart ->
                                toggledInGesture = emptySet()
                                val sizePx = min(size.width.toFloat(), size.height.toFloat())
                                val normalizedOffset = Offset(
                                    dragStart.x.coerceIn(0f, sizePx),
                                    dragStart.y.coerceIn(0f, sizePx)
                                )
                                indexFromOffset(normalizedOffset, sizePx)?.let { index ->
                                    togglePixel(index)
                                    toggledInGesture = toggledInGesture + index
                                }
                            },
                            onDragEnd = { toggledInGesture = emptySet() },
                            onDragCancel = { toggledInGesture = emptySet() }
                        ) { change, _ ->
                            val sizePx = min(size.width.toFloat(), size.height.toFloat())
                            val normalizedOffset = Offset(
                                change.position.x.coerceIn(0f, sizePx),
                                change.position.y.coerceIn(0f, sizePx)
                            )
                            indexFromOffset(normalizedOffset, sizePx)?.let { index ->
                                if (index !in toggledInGesture) {
                                    togglePixel(index)
                                    toggledInGesture = toggledInGesture + index
                                }
                            }
                        }
                    }
            ) {
                val sizePx = min(size.width, size.height)
                val origin = Offset(
                    x = (size.width - sizePx) / 2f,
                    y = (size.height - sizePx) / 2f
                )
                val cellSize = sizePx / GridSize

                for (row in 0 until GridSize) {
                    for (column in 0 until GridSize) {
                        val pixelIndex = row * GridSize + column
                        val color = if (pixels[pixelIndex]) Color.Black else Color.White

                        drawRect(
                            color = color,
                            topLeft = Offset(
                                x = origin.x + column * cellSize,
                                y = origin.y + row * cellSize
                            ),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                        )
                    }
                }

                for (line in 0..GridSize) {
                    val lineOffset = line * cellSize
                    drawLine(
                        color = Color(0xFF9AA7B8),
                        start = Offset(origin.x + lineOffset, origin.y),
                        end = Offset(origin.x + lineOffset, origin.y + sizePx),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF9AA7B8),
                        start = Offset(origin.x, origin.y + lineOffset),
                        end = Offset(origin.x + sizePx, origin.y + lineOffset),
                        strokeWidth = 2f
                    )
                }

                drawRect(
                    color = TGU_Blue,
                    topLeft = origin,
                    size = androidx.compose.ui.geometry.Size(sizePx, sizePx),
                    style = Stroke(width = 3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
