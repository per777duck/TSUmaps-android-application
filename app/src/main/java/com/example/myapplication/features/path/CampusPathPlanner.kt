package com.example.myapplication.features.path

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.Node
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.venues.CAMPUS_HEIGHT_KM
import com.example.myapplication.data.venues.CAMPUS_MAP_HEIGHT_PX
import com.example.myapplication.data.venues.CAMPUS_MAP_WIDTH_PX
import com.example.myapplication.data.venues.CAMPUS_WIDTH_KM
import com.example.myapplication.data.venues.FoodVenue
import kotlin.math.hypot

data class CampusRoutingContext(
    val startNode: Node,
    val startDisplayOffset: Offset,
    val venueNodes: Map<Int, Node>,
    val venueDisplayOffsets: Map<Int, Offset>,
    val travelMinutesMatrix: Array<DoubleArray>
)

object CampusPathPlanner {

    private const val WALKING_SPEED_KMH = 5.0
    private const val SERVICE_MINUTES_PER_STOP = 4

    fun buildContext(
        mapData: MapData,
        algorithm: AStarAlgorithm,
        rawStartOffset: Offset,
        venues: List<FoodVenue>
    ): CampusRoutingContext {
        val startSnapped = snapOffsetToWalkable(mapData, rawStartOffset)
        val venueSnapped = venues.associate { v ->
            v.id to snapOffsetToWalkable(mapData, v.mapPosition)
        }

        val startNode = offsetToNearestWalkableNode(mapData, startSnapped)
        val venueNodes = venueSnapped.mapValues { (id, off) ->
            offsetToNearestWalkableNode(mapData, off)
        }

        val n = venues.size + 1
        val matrix = Array(n) { DoubleArray(n) { 0.0 } }
        val nodesList = listOf(startNode) + venues.map { venueNodes.getValue(it.id) }

        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i == j) continue
                val path = algorithm.findPathSync(nodesList[i], nodesList[j])
                val km = if (path != null && path.size >= 2) {
                    pathLengthKm(path, mapData)
                } else {
                    straightLineKm(nodesList[i], nodesList[j], mapData)
                }
                matrix[i][j] = km / (WALKING_SPEED_KMH / 60.0)
            }
        }

        val venueDisplayMap = venueSnapped.mapValues { it.value }
        return CampusRoutingContext(
            startNode = startNode,
            startDisplayOffset = startSnapped,
            venueNodes = venueNodes,
            venueDisplayOffsets = venueDisplayMap,
            travelMinutesMatrix = matrix
        )
    }

    fun buildFullPathPolyline(
        mapData: MapData,
        algorithm: AStarAlgorithm,
        orderedVenueIds: List<Int>,
        context: CampusRoutingContext
    ): List<Offset> {
        if (orderedVenueIds.isEmpty()) return emptyList()

        val points = mutableListOf<Offset>()
        var current = context.startNode

        for (vid in orderedVenueIds) {
            val target = context.venueNodes[vid] ?: continue
            val segment = algorithm.findPathSync(current, target)
            if (segment != null && segment.isNotEmpty()) {
                segment.forEach { node ->
                    val o = nodeToMapOffset(mapData, node)
                    if (points.isEmpty() || points.last() != o) {
                        points.add(o)
                    }
                }
            } else {
                val off = context.venueDisplayOffsets[vid] ?: nodeToMapOffset(mapData, target)
                if (points.isEmpty() || points.last() != off) {
                    points.add(off)
                }
            }
            current = target
        }

        return points
    }

    fun matrixIndexForVenueId(venues: List<FoodVenue>, venueId: Int): Int {
        val idx = venues.indexOfFirst { it.id == venueId }
        return idx + 1
    }

    fun serviceMinutesPerStop(): Int = SERVICE_MINUTES_PER_STOP

    private fun offsetToNearestWalkableNode(mapData: MapData, offset: Offset): Node {
        val gx =
            (offset.x / CAMPUS_MAP_WIDTH_PX * mapData.width).toInt().coerceIn(0, mapData.width - 1)
        val gy = (offset.y / CAMPUS_MAP_HEIGHT_PX * mapData.length).toInt()
            .coerceIn(0, mapData.length - 1)
        val n = findNearestWalkable(mapData, gx, gy)
        return n ?: Node(gx, gy)
    }

    private fun snapOffsetToWalkable(mapData: MapData, offset: Offset): Offset {
        val gx =
            (offset.x / CAMPUS_MAP_WIDTH_PX * mapData.width).toInt().coerceIn(0, mapData.width - 1)
        val gy = (offset.y / CAMPUS_MAP_HEIGHT_PX * mapData.length).toInt()
            .coerceIn(0, mapData.length - 1)
        val n = findNearestWalkable(mapData, gx, gy) ?: return offset
        return nodeToMapOffset(mapData, n)
    }

    private fun findNearestWalkable(mapData: MapData, x: Int, y: Int): Node? {
        if (mapData.width == 0 || mapData.length == 0) return null
        val clampedX = x.coerceIn(0, mapData.width - 1)
        val clampedY = y.coerceIn(0, mapData.length - 1)
        if (mapData.isAvailable(clampedX, clampedY)) return Node(clampedX, clampedY)

        val maxRadius = maxOf(mapData.width, mapData.length)
        for (radius in 1..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val nx = clampedX + dx
                    val ny = clampedY + dy
                    if (nx !in 0 until mapData.width || ny !in 0 until mapData.length) continue
                    if (mapData.isAvailable(nx, ny)) return Node(nx, ny)
                }
            }
        }
        return null
    }

    fun nodeToMapOffset(mapData: MapData, node: Node): Offset {
        val cx = (node.x + 0.5f) / mapData.width * CAMPUS_MAP_WIDTH_PX
        val cy = (node.y + 0.5f) / mapData.length * CAMPUS_MAP_HEIGHT_PX
        return Offset(cx, cy)
    }

    private fun gridCellCenterKm(mapData: MapData, node: Node): Pair<Double, Double> {
        val xKm = (node.x + 0.5) / mapData.width * CAMPUS_WIDTH_KM
        val yKm = (node.y + 0.5) / mapData.length * CAMPUS_HEIGHT_KM
        return xKm to yKm
    }

    private fun pathLengthKm(path: List<Node>, mapData: MapData): Double {
        var sum = 0.0
        for (i in 0 until path.lastIndex) {
            val a = gridCellCenterKm(mapData, path[i])
            val b = gridCellCenterKm(mapData, path[i + 1])
            sum += hypot(b.first - a.first, b.second - a.second)
        }
        return sum
    }

    private fun straightLineKm(a: Node, b: Node, mapData: MapData): Double {
        val pa = gridCellCenterKm(mapData, a)
        val pb = gridCellCenterKm(mapData, b)
        return hypot(pb.first - pa.first, pb.second - pa.second)
    }
}
