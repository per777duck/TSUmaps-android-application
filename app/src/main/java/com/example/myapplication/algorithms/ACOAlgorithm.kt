package com.example.myapplication.algorithms

import com.example.myapplication.algorithms.models.IDistanceMetrics
import com.example.myapplication.algorithms.models.Point
import kotlin.math.pow
import kotlin.random.Random

data class ACOParameters(
    val antCount: Int = 30,
    val iterations: Int = 120,
    val alpha: Double = 1.0,
    val beta: Double = 3.0,
    val evaporationRate: Double = 0.45,
    val pheromoneDepositQ: Double = 120.0,
    val initialPheromone: Double = 1.0,
    val returnToStart: Boolean = true
)

data class ACORouteResult(
    val orderedPoints: List<Point>,
    val orderedPointIds: List<Int>,
    val distance: Double
)

class ACOAlgorithm(private val parameters: ACOParameters = ACOParameters())
{

    suspend fun run(points: List<Point>, metric: IDistanceMetrics): ACORouteResult
    {
        require(points.isNotEmpty()) { "Список не должен быть пустым!" }
        require(points.size >= 2) { "Необходимо выбрать минимум 2 точки!" }

        val distanceMatrix = computeDistanceMatrix(points, metric)
        val pheromones = Array(points.size) { DoubleArray(points.size) { parameters.initialPheromone } }

        var globalBestRoute: List<Int>? = null
        var globalBestDistance = Double.POSITIVE_INFINITY

        repeat(parameters.iterations)
        {
            val iterationRoutes = mutableListOf<Pair<List<Int>, Double>>()

            repeat(parameters.antCount)
            {
                val route = buildAntRoute(points.size, pheromones, distanceMatrix)
                val distance = routeDistance(route, distanceMatrix, parameters.returnToStart)
                iterationRoutes += route to distance

                if (distance < globalBestDistance)
                {
                    globalBestDistance = distance
                    globalBestRoute = route
                }
            }

            evaporatePheromones(pheromones, parameters.evaporationRate)
            depositPheromones(pheromones, iterationRoutes, parameters)
        }

        val bestRoute = globalBestRoute
            ?: throw IllegalStateException("ACO could not build any route")

        val orderedPoints = bestRoute.map { index -> points[index] }
        val routeWithReturn = if (parameters.returnToStart)
        {
            orderedPoints + orderedPoints.first()
        }
        else {
            orderedPoints
        }

        return ACORouteResult(
            orderedPoints = routeWithReturn,
            orderedPointIds = routeWithReturn.map { it.id },
            distance = globalBestDistance
        )
    }

    private suspend fun computeDistanceMatrix(
        points: List<Point>,
        metric: IDistanceMetrics
    ): Array<DoubleArray>
    {
        val n = points.size
        val matrix = Array(n) { DoubleArray(n) { Double.POSITIVE_INFINITY } }

        for (i in 0 until n)
        {
            matrix[i][i] = 0.0
            for (j in i + 1 until n)
            {
                val dist = metric.calculatingDistance(points[i], points[j])
                matrix[i][j] = dist
                matrix[j][i] = dist
            }
        }
        return matrix
    }

    private fun buildAntRoute(
        size: Int,
        pheromones: Array<DoubleArray>,
        distances: Array<DoubleArray>
    ): List<Int>
    {
        val start = Random.nextInt(size)
        val route = mutableListOf(start)
        val visited = BooleanArray(size)
        visited[start] = true

        while (route.size < size)
        {
            val current = route.last()
            val next = chooseNextNode(current, visited, pheromones, distances)
            route += next
            visited[next] = true
        }

        return route
    }

    private fun chooseNextNode(
        current: Int,
        visited: BooleanArray,
        pheromones: Array<DoubleArray>,
        distances: Array<DoubleArray>
    ): Int
    {
        val candidates = mutableListOf<Int>()
        val weights = mutableListOf<Double>()
        var weightSum = 0.0

        for (next in visited.indices)
        {
            if (visited[next]) continue

            val distance = distances[current][next]
            if (!distance.isFinite() || distance <= 0.0) continue

            val pheromonePart = pheromones[current][next].pow(parameters.alpha)
            val heuristicPart = (1.0 / distance).pow(parameters.beta)
            val weight = pheromonePart * heuristicPart

            if (weight <= 0.0 || !weight.isFinite()) continue

            candidates += next
            weights += weight
            weightSum += weight
        }

        if (candidates.isEmpty())
        {
            return visited.indices.first { !visited[it] }
        }

        val threshold = Random.nextDouble() * weightSum
        var cumulative = 0.0
        for (i in candidates.indices)
        {
            cumulative += weights[i]
            if (cumulative >= threshold)
            {
                return candidates[i]
            }
        }

        return candidates.last()
    }

    private fun routeDistance(
        route: List<Int>,
        distances: Array<DoubleArray>,
        returnToStart: Boolean
    ): Double
    {
        var total = 0.0

        for (i in 0 until route.lastIndex)
        {
            total += distances[route[i]][route[i + 1]]
        }

        if (returnToStart && route.size > 1)
        {
            total += distances[route.last()][route.first()]
        }

        return total
    }

    private fun evaporatePheromones(pheromones: Array<DoubleArray>, evaporationRate: Double)
    {
        val keepRatio = (1.0 - evaporationRate).coerceIn(0.0, 1.0)
        for (i in pheromones.indices)
        {
            for (j in pheromones[i].indices)
            {
                pheromones[i][j] = (pheromones[i][j] * keepRatio).coerceAtLeast(1e-12)
            }
        }
    }

    private fun depositPheromones(
        pheromones: Array<DoubleArray>,
        routes: List<Pair<List<Int>, Double>>,
        params: ACOParameters
    )
    {
        routes.forEach { (route, distance) ->
            if (!distance.isFinite() || distance <= 0.0) return@forEach

            val delta = params.pheromoneDepositQ / distance
            for (i in 0 until route.lastIndex)
            {
                val from = route[i]
                val to = route[i + 1]
                pheromones[from][to] += delta
                pheromones[to][from] += delta
            }

            if (params.returnToStart && route.size > 1)
            {
                val last = route.last()
                val first = route.first()
                pheromones[last][first] += delta
                pheromones[first][last] += delta
            }
        }
    }
}
