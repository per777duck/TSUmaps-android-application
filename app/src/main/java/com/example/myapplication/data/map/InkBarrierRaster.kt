package com.example.myapplication.data.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Converts ink strokes in canvas coordinates to grid cells treated as impassable for A*.
 * Samples along segments and thickens by a neighborhood to approximate the on-screen pen width.
 */
fun inkStrokesToBarrierCells(
    strokes: List<List<Offset>>,
    canvasSize: IntSize,
    mapWidth: Int,
    mapHeight: Int
): Set<Pair<Int, Int>> {
    if (canvasSize.width <= 0 || canvasSize.height <= 0 || mapWidth <= 0 || mapHeight <= 0) {
        return emptySet()
    }
    val cw = canvasSize.width.toFloat()
    val ch = canvasSize.height.toFloat()
    val cellW = cw / mapWidth
    val cellH = ch / mapHeight
    val penHalfPx = 3.5f
    val radiusCells = max(
        1,
        min(3, (penHalfPx / min(cellW, cellH)).roundToInt())
    )
    val out = mutableSetOf<Pair<Int, Int>>()

    fun addDisk(px: Float, py: Float) {
        val gx = (px / cw * mapWidth).toInt().coerceIn(0, mapWidth - 1)
        val gy = (py / ch * mapHeight).toInt().coerceIn(0, mapHeight - 1)
        val r2 = radiusCells * radiusCells
        for (dy in -radiusCells..radiusCells) {
            for (dx in -radiusCells..radiusCells) {
                if (dx * dx + dy * dy > r2) continue
                val nx = gx + dx
                val ny = gy + dy
                if (nx in 0 until mapWidth && ny in 0 until mapHeight) {
                    out.add(nx to ny)
                }
            }
        }
    }

    fun strokeSegment(a: Offset, b: Offset) {
        val len = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        val steps = max(1, (len / 2f).roundToInt().coerceAtMost(12_000))
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = a.x + (b.x - a.x) * t
            val y = a.y + (b.y - a.y) * t
            addDisk(x, y)
        }
    }

    for (stroke in strokes) {
        when {
            stroke.isEmpty() -> Unit
            stroke.size == 1 -> addDisk(stroke[0].x, stroke[0].y)
            else -> {
                for (i in 0 until stroke.lastIndex) {
                    strokeSegment(stroke[i], stroke[i + 1])
                }
            }
        }
    }
    return out
}
