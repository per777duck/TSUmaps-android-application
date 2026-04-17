package com.example.myapplication.data.map

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.algorithms.routes.Node

object MapCoordinateTransformer {
    fun tapToGrid(
        tapOffset: Offset,
        canvasWidth: Float,
        canvasHeight: Float,
        mapData: MapData
    ): Node {
        val gridX = (tapOffset.x / canvasWidth * mapData.width).toInt()
            .coerceIn(0, mapData.width - 1)
        val gridY = (tapOffset.y / canvasHeight * mapData.length).toInt()
            .coerceIn(0, mapData.length - 1)
        return Node(gridX, gridY)
    }
}
