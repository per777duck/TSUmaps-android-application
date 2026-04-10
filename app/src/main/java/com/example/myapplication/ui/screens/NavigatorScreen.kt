package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.AStarAlgorithm
import com.example.myapplication.Node
import com.example.myapplication.R
import com.example.myapplication.data.map.MapCoordinateTransformer
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapMatrixLoader
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.features.path.PathfindingCoordinator
import com.example.myapplication.ui.TGU_Blue
import com.example.myapplication.ui.TGU_Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NavigatorScreen() {
    val scope = rememberCoroutineScope()

    var fromText by rememberSaveable { mutableStateOf("0,0") }
    var toText by remember { mutableStateOf("50,50") }
    var activeInput by remember { mutableStateOf(0) }
    var currentPath by remember { mutableStateOf<List<Node>>(emptyList()) }

    val context = LocalContext.current
    val mapData = remember {
        MapMatrixLoader(context).mapToMatrix(R.drawable.map_mask)
    }
    val algorithm = remember { AStarAlgorithm(mapData) }

    Column(modifier = Modifier.fillMaxSize()) {
        MapSection(
            mapData = mapData,
            path = currentPath,
            modifier = Modifier.weight(1f),
            onMapClick = { node ->
                val coordinates = "${node.x},${node.y}"
                if (activeInput == 0) {
                    fromText = coordinates
                    activeInput = 1
                } else {
                    toText = coordinates
                }
            }
        )
        InputSection(
            fromText = fromText,
            onFromChange = { fromText = it },
            toText = toText,
            onToChange = { toText = it },
            activeInput = activeInput,
            onActiveFieldChange = { activeInput = it },
            onBuildClick = {
                scope.launch(Dispatchers.Default) {
                    PathfindingCoordinator.buildPath(
                        algorithm = algorithm,
                        fromText = fromText,
                        toText = toText,
                        speedMs = 10L,
                        onPathUpdate = { currentPath = it }
                    )
                }
            }
        )
    }
}

@Composable
fun MapSection(
    mapData: MapData,
    path: List<Node>,
    modifier: Modifier = Modifier,
    onMapClick: (Node) -> Unit
) {
    val mapShape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = mapShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        MapRendering.TguMapWrapper(modifier = Modifier.fillMaxSize()) { currentScale, _, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            onMapClick(
                                MapCoordinateTransformer.tapToGrid(
                                    tapOffset = tapOffset,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat(),
                                    mapData = mapData
                                )
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (path.isNotEmpty()) {
                        val scaleX = size.width / mapData.width
                        val scaleY = size.height / mapData.length

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
                }
            }
        }
    }
}

@Composable
fun InputSection(
    fromText: String,
    onFromChange: (String) -> Unit,
    toText: String,
    onToChange: (String) -> Unit,
    activeInput: Int,
    onActiveFieldChange: (Int) -> Unit,
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
            Text("Построить маршрут", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = fromText,
                onValueChange = onFromChange,
                label = { Text("Откуда (кликните на карту)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onActiveFieldChange(0) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (activeInput == 0) TGU_Blue else TGU_Gold,
                    unfocusedBorderColor = if (activeInput == 0) TGU_Blue else Color.Gray,
                    cursorColor = TGU_Blue
                )
            )

            OutlinedTextField(
                value = toText,
                onValueChange = onToChange,
                label = { Text("Куда (кликните на карту)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onActiveFieldChange(1) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (activeInput == 1) TGU_Blue else TGU_Gold,
                    unfocusedBorderColor = if (activeInput == 1) TGU_Blue else Color.Gray,
                    cursorColor = TGU_Blue
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBuildClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
            ) {
                Text("ПОСТРОИТЬ ПУТЬ", color = Color.White)
            }
        }
    }
}
