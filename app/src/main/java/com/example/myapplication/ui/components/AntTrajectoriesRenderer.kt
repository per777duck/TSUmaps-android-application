package com.example.myapplication.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication.algorithms.clusterization.Point
import kotlin.math.hypot

fun DrawScope.drawAntTrajectories(
    trajectories: List<List<Point>>,
    scaleX: Float,
    scaleY: Float,
    currentScale: Float,
    phase: Float
) {
    if (trajectories.isEmpty()) return

    val clampedScale = currentScale.coerceAtLeast(1f)
    val antCount = trajectories.size.coerceAtLeast(1)

    trajectories.forEachIndexed { index, route ->
        if (route.size < 2) return@forEachIndexed

        val routeOffsets = route.map { point ->
            Offset(
                x = point.x.toFloat() * scaleX,
                y = point.y.toFloat() * scaleY
            )
        }

        val routePath = Path().apply {
            moveTo(routeOffsets.first().x, routeOffsets.first().y)
            routeOffsets.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
        }
        drawPath(
            path = routePath,
            color = Color(0xFF00ACC1).copy(alpha = 0.18f),
            style = Stroke(width = 2f / clampedScale)
        )

        val antPhase = ((phase + index.toFloat() / antCount) % 1f + 1f) % 1f
        val antPosition = pointOnPolyline(routeOffsets, antPhase)
        drawCircle(
            color = Color(0xFFFFC107),
            radius = 4f / clampedScale,
            center = antPosition
        )
    }
}

private fun pointOnPolyline(points: List<Offset>, t: Float): Offset {
    if (points.isEmpty()) return Offset.Zero
    if (points.size == 1) return points.first()

    var totalLength = 0f
    val segmentLengths = FloatArray(points.lastIndex)
    for (i in 0 until points.lastIndex) {
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        val length = hypot(dx, dy)
        segmentLengths[i] = length
        totalLength += length
    }

    if (totalLength <= 0f) return points.first()
    val target = totalLength * t.coerceIn(0f, 1f)

    var passed = 0f
    for (i in segmentLengths.indices) {
        val segLength = segmentLengths[i]
        if (segLength <= 0f) continue
        if (passed + segLength >= target) {
            val local = (target - passed) / segLength
            return Offset(
                x = points[i].x + (points[i + 1].x - points[i].x) * local,
                y = points[i].y + (points[i + 1].y - points[i].y) * local
            )
        }
        passed += segLength
    }

    return points.last()
}
