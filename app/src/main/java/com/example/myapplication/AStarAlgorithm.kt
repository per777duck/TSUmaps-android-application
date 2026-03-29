package com.example.myapplication
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.abs

data class Node(val x: Int, val y: Int, var currentCost: Double = 0.0,
                var heuristicEstimation: Double = 0.0, var parent: Node? = null)
{
    val commonEstimation: Double get() = currentCost + heuristicEstimation
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Node
        return x == other.x && y == other.y
    }
    override fun hashCode(): Int
    {
        var result = x;
        result =  31* result + y
        return result
    }
}

data class searchState( val openSet: List<Node>, val closedSet: List<Node>,
                        val currentPath: List<Node>, val finished: Boolean, val fullPath: List<Node>?)

class AStarAlgorithm(private val grid: Array<IntArray>, private val rows: Int=100,
                     private val cols: Int=100)
{
    private val diagonalCost = 1.4
    private val straightCost = 1.0;

    private fun chebishevHeuristic(a: Node, b:Node): Double
    {
        return max(abs(a.x - b.x), abs(a.y-b.y)).toDouble()
    }
    private fun getNeighbors(node: Node): List<Pair<Node, Double>>
    {
        val neighbors = mutableListOf<Pair<Node, Double>>()

        val moves = listOf(
            Triple(0, 1, false),
            Triple(0, -1, false),
            Triple(1, 0, false),
            Triple(-1,0,false),
            Triple(1, 1, true),
            Triple(1,-1,true),
            Triple(-1, 1, true),
            Triple(-1,-1, true))

        for ((dx, dy, isDiagonal) in moves)
        {
            val nx = node.x + dx
            val ny = node.y + dy

            if (nx in 0 until cols && ny in 0 until rows)
            {

                if (grid[ny][nx] != 0)
                {
                    if (isDiagonal)
                    {
                        if (grid[ny][node.x] != 0 && grid[node.y][nx] != 0)
                        {
                            neighbors.add(Node(nx, ny) to diagonalCost)
                        }
                    }
                    else
                    {
                        neighbors.add(Node(nx, ny) to straightCost)
                    }
                }
            }
        }
        return neighbors
    }

    suspend fun findPath(start:Node, end: Node, speedMs:Long = 50L,
                         callback: (searchState)->Unit ): List<Node>?
    {
        val openSet = mutableListOf<Node>()
        val closedSet = mutableSetOf<Node>()

        start.heuristicEstimation = chebishevHeuristic(start, end)
        openSet.add(start)

        while (openSet.isNotEmpty())
        {
            val current = openSet.minByOrNull { it.commonEstimation } ?: break

            if (current.x == end.x && current.y == end.y)
            {
                val path = reconstructPath(current)
                callback(searchState(openSet, closedSet.toList(), path, true, path))
                return path
            }
            openSet.remove(current)
            closedSet.add(current)

            val currentPath = reconstructPath(current)
            callback(searchState(
                openSet.toList(),
                closedSet.toList(),
                currentPath,
                false,
                null))
            for ((neighbor, moveCost) in getNeighbors(current))
            {
                if (neighbor in closedSet) continue
                val tentativeG = current.currentCost + moveCost

                val existingNode = openSet.find { it.x == neighbor.x && it.y == neighbor.y }

                if (existingNode == null)
                {
                    neighbor.currentCost = tentativeG
                    neighbor.heuristicEstimation = chebishevHeuristic(neighbor, end)
                    neighbor.parent = current
                    openSet.add(neighbor)
                }
                else if (tentativeG < existingNode.currentCost)
                {
                    existingNode.currentCost = tentativeG
                    existingNode.parent = current
                }
            }

            delay(speedMs)
        }

        callback(searchState(openSet, closedSet.toList(), emptyList(), true, null))
        return null
    }

    private fun reconstructPath(current: Node): List<Node>
    {
        val path = mutableListOf<Node>()
        var temp: Node? = current
        while (temp != null)
        {
            path.add(temp)
            temp = temp.parent
        }
        return path.reversed()
    }
}