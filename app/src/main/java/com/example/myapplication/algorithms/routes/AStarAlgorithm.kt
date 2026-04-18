package com.example.myapplication.algorithms.routes

import com.example.myapplication.data.map.MapData
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

data class Node(
    val x: Int,
    val y: Int,
    var currentCost: Double = 0.0,
    var heuristicEstimation: Double = 0.0,
    var parent: Node? = null
) {
    val commonEstimation: Double get() = currentCost + heuristicEstimation

    override fun equals(other: Any?): Boolean {
        return other is Node && x == other.x && y == other.y
    }

    override fun hashCode(): Int = 31 * x + y
}

data class SearchState(
    val openSet: List<Node>,
    val closedSet: List<Node>,
    val currentPath: List<Node>,
    val finished: Boolean,
    val fullPath: List<Node>?
)

class AStarAlgorithm(private val mapData: MapData) {

    private val diagonalCost = 1.4
    private val straightCost = 1.0
    private val heuristicWeight = 0.25

    private fun heuristic(a: Node, b: Node): Double {
        return max(abs(a.x - b.x), abs(a.y - b.y)).toDouble()
    }

    private fun isWalkable(x: Int, y: Int, userBarriers: Set<Pair<Int, Int>>): Boolean {
        if (!mapData.isAvailable(x, y)) return false
        if (x to y in userBarriers) return false
        return true
    }

    private fun getNeighbors(node: Node, userBarriers: Set<Pair<Int, Int>>): List<Pair<Node, Double>> {
        val neighbors = mutableListOf<Pair<Node, Double>>()

        val moves = listOf(
            Triple(0, 1, false),
            Triple(0, -1, false),
            Triple(1, 0, false),
            Triple(-1, 0, false),
            Triple(1, 1, true),
            Triple(1, -1, true),
            Triple(-1, 1, true),
            Triple(-1, -1, true)
        )

        for ((dx, dy, isDiagonal) in moves) {
            val nx = node.x + dx
            val ny = node.y + dy

            if (nx !in 0 until mapData.width || ny !in 0 until mapData.length) continue

            if (!isWalkable(nx, ny, userBarriers)) continue

            if (isDiagonal) {
                if (!isWalkable(node.x, ny, userBarriers) || !isWalkable(nx, node.y, userBarriers)) {
                    continue
                }
                neighbors.add(Node(nx, ny) to diagonalCost)
            } else {
                neighbors.add(Node(nx, ny) to straightCost)
            }
        }

        return neighbors
    }

    suspend fun findPath(
        start: Node,
        end: Node,
        speedMs: Long = 2L,
        maxDurationMs: Long = 10_000L,
        callbackStride: Int = 10,
        visualizationLimit: Int = 3500,
        userBarriers: Set<Pair<Int, Int>> = emptySet(),
        callback: (SearchState) -> Unit
    ): List<Node>? {
        if (!isWalkable(start.x, start.y, userBarriers) || !isWalkable(end.x, end.y, userBarriers)) {
            callback(SearchState(emptyList(), emptyList(), emptyList(), true, null))
            return null
        }

        val openSet = mutableListOf<Node>()
        val closedSet = mutableSetOf<Node>()
        val closedTrace = mutableListOf<Node>()
        val bestG = mutableMapOf<Pair<Int, Int>, Double>()
        val nodesByCord = mutableMapOf<Pair<Int, Int>, Node>()
        val startTimeNanos = System.nanoTime()

        start.currentCost = 0.0
        start.heuristicEstimation = heuristic(start, end) * heuristicWeight
        start.parent = null
        openSet.add(start)
        bestG[start.x to start.y] = 0.0
        nodesByCord[start.x to start.y] = start

        var iteration = 0
        while (openSet.isNotEmpty()) {
            val current = openSet.minWithOrNull(
                compareBy<Node> { it.commonEstimation }.thenBy { it.currentCost }
            ) ?: break

            if (current.x == end.x && current.y == end.y) {
                val path = reconstructPath(current)
                callback(
                    SearchState(
                        openSet.takeLast(visualizationLimit),
                        closedTrace.takeLast(visualizationLimit),
                        path,
                        true,
                        path
                    )
                )
                return path
            }

            openSet.remove(current)
            closedSet.add(current)
            closedTrace.add(current)

            iteration += 1
            if (iteration % callbackStride == 0) {
                callback(
                    SearchState(
                        openSet.takeLast(visualizationLimit),
                        closedTrace.takeLast(visualizationLimit),
                        emptyList(),
                        false,
                        null
                    )
                )
            }
            for ((neighbor, moveCost) in getNeighbors(current, userBarriers)) {
                if (neighbor in closedSet) continue

                val tentativeG = current.currentCost + moveCost
                val key = neighbor.x to neighbor.y
                val knownG = bestG[key]

                if (knownG != null && tentativeG >= knownG) continue

                bestG[key] = tentativeG

                val existingNode = nodesByCord[key]
                if (existingNode == null) {
                    neighbor.currentCost = tentativeG
                    neighbor.heuristicEstimation = heuristic(neighbor, end) * heuristicWeight
                    neighbor.parent = current
                    openSet.add(neighbor)
                    nodesByCord[key] = neighbor
                } else {
                    existingNode.currentCost = tentativeG
                    existingNode.parent = current
                    if (existingNode !in openSet) {
                        openSet.add(existingNode)
                    }
                }
            }

            val elapsedMs = (System.nanoTime() - startTimeNanos) / 1_000_000
            if (elapsedMs >= maxDurationMs) {
                callback(
                    SearchState(
                        openSet.takeLast(visualizationLimit),
                        closedTrace.takeLast(visualizationLimit),
                        emptyList(),
                        true,
                        null
                    )
                )
                return null
            }

            if (speedMs > 0L && iteration % callbackStride == 0) {
                delay(speedMs)
            }
        }

        callback(
            SearchState(
                openSet.takeLast(visualizationLimit),
                closedTrace.takeLast(visualizationLimit),
                emptyList(),
                true,
                null
            )
        )
        return null
    }

    private fun reconstructPath(current: Node): List<Node> {
        val path = mutableListOf<Node>()
        var temp: Node? = current

        while (temp != null) {
            path.add(temp)
            temp = temp.parent
        }

        return path.reversed()
    }

    fun findPathSync(
        start: Node,
        end: Node,
        userBarriers: Set<Pair<Int, Int>> = emptySet()
    ): List<Node>? {
        if (!isWalkable(start.x, start.y, userBarriers) || !isWalkable(end.x, end.y, userBarriers)) {
            return null
        }

        val openSet = mutableListOf<Node>()
        val closedSet = mutableSetOf<Node>()
        val bestG = mutableMapOf<Pair<Int, Int>, Double>()
        val nodesByCoord = mutableMapOf<Pair<Int, Int>, Node>()

        start.currentCost = 0.0
        start.heuristicEstimation = heuristic(start, end) * heuristicWeight
        start.parent = null
        openSet.add(start)
        bestG[start.x to start.y] = 0.0
        nodesByCoord[start.x to start.y] = start

        while (openSet.isNotEmpty()) {
            val current = openSet.minWithOrNull(
                compareBy<Node> { it.commonEstimation }.thenBy { it.currentCost }
            ) ?: break

            if (current.x == end.x && current.y == end.y) {
                return reconstructPath(current)
            }

            openSet.remove(current)
            closedSet.add(current)

            for ((neighbor, moveCost) in getNeighbors(current, userBarriers)) {
                if (neighbor in closedSet) continue

                val tentativeG = current.currentCost + moveCost
                val key = neighbor.x to neighbor.y
                val knownG = bestG[key]

                if (knownG != null && tentativeG >= knownG) continue

                bestG[key] = tentativeG

                val existingNode = nodesByCoord[key]
                if (existingNode == null) {
                    neighbor.currentCost = tentativeG
                    neighbor.heuristicEstimation = heuristic(neighbor, end) * heuristicWeight
                    neighbor.parent = current
                    openSet.add(neighbor)
                    nodesByCoord[key] = neighbor
                } else {
                    existingNode.currentCost = tentativeG
                    existingNode.parent = current
                    if (existingNode !in openSet) {
                        openSet.add(existingNode)
                    }
                }
            }
        }

        return null
    }
}