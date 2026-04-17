package com.example.myapplication.algorithms.ants

import com.example.myapplication.algorithms.clusterization.AStarMetric
import com.example.myapplication.algorithms.clusterization.Point
import kotlin.math.pow
import kotlin.random.Random

data class CoworkingAntParameters(
    val iterations: Int = 80,
    val evaporationRate: Double = 0.35,
    val alpha: Double = 1.1,
    val beta: Double = 2.2,
    val initialPheromone: Double = 1.0,
    val pheromoneDepositQ: Double = 3.0,
    val overflowPenaltyWeight: Double = 40.0
)

data class CoworkingAssignment(
    val venueId: Int,
    val venueName: String,
    val capacity: Int,
    val assignedStudents: Int,
    val travelDistance: Double
)

data class CoworkingAllocationResult(
    val assignments: List<CoworkingAssignment>,
    val totalDistance: Double,
    val unassignedStudents: Int,
    val antTrajectories: List<List<Point>>,
    val antTrajectoriesByIteration: List<List<List<Point>>>
)

class CoworkingAntColonyAllocator(
    private val parameters: CoworkingAntParameters = CoworkingAntParameters()
) {

    suspend fun run(
        startPoint: Point,
        coworkings: List<CoworkingCandidate>,
        studentsCount: Int,
        metric: AStarMetric
    ): CoworkingAllocationResult {
        require(coworkings.isNotEmpty()) { "Нет доступных коворкингов для распределения" }
        require(studentsCount > 0) { "Количество студентов должно быть больше нуля" }

        val distances = coworkings.map { coworking ->
            metric.calculatingDistance(startPoint, coworking.point).let { if (it.isFinite()) it else 1e6 }
        }
        val routeByVenueId = coworkings.associate { coworking ->
            coworking.venueId to buildPolyline(metric, listOf(startPoint, coworking.point))
        }

        val pheromones = DoubleArray(coworkings.size) { parameters.initialPheromone }
        val trajectoriesByIteration = mutableListOf<List<List<Point>>>()

        var bestCounts = IntArray(coworkings.size)
        var bestUnassigned = Int.MAX_VALUE
        var bestCost = Double.POSITIVE_INFINITY

        repeat(parameters.iterations) {
            val iterationCounts = IntArray(coworkings.size)
            val iterationTrajectories = mutableListOf<List<Point>>()
            var iterationUnassigned = 0

            repeat(studentsCount) {
                val selected = chooseCoworking(
                    pheromones = pheromones,
                    distances = distances,
                    capacities = coworkings.map { it.capacity }.toIntArray(),
                    currentLoad = iterationCounts
                )
                if (selected == -1) {
                    iterationUnassigned += 1
                } else {
                    iterationCounts[selected] += 1
                    iterationTrajectories += routeByVenueId[coworkings[selected].venueId] ?: emptyList()
                }
            }

            val iterationCost = objectiveCost(
                counts = iterationCounts,
                capacities = coworkings.map { it.capacity }.toIntArray(),
                distances = distances,
                unassigned = iterationUnassigned
            )
            if (iterationUnassigned < bestUnassigned ||
                (iterationUnassigned == bestUnassigned && iterationCost < bestCost)
            ) {
                bestUnassigned = iterationUnassigned
                bestCost = iterationCost
                bestCounts = iterationCounts.copyOf()
            }

            evaporate(pheromones)
            deposit(
                pheromones = pheromones,
                counts = iterationCounts,
                capacities = coworkings.map { it.capacity }.toIntArray(),
                distances = distances
            )
            trajectoriesByIteration += iterationTrajectories
        }

        val assignments = coworkings.indices.map { idx ->
            CoworkingAssignment(
                venueId = coworkings[idx].venueId,
                venueName = coworkings[idx].name,
                capacity = coworkings[idx].capacity,
                assignedStudents = bestCounts[idx],
                travelDistance = distances[idx]
            )
        }

        val totalDistance = assignments.sumOf { it.assignedStudents * it.travelDistance }
        return CoworkingAllocationResult(
            assignments = assignments,
            totalDistance = totalDistance,
            unassignedStudents = bestUnassigned.coerceAtLeast(0),
            antTrajectories = trajectoriesByIteration.lastOrNull() ?: emptyList(),
            antTrajectoriesByIteration = trajectoriesByIteration
        )
    }

    private fun chooseCoworking(
        pheromones: DoubleArray,
        distances: List<Double>,
        capacities: IntArray,
        currentLoad: IntArray
    ): Int {
        val availableIndexes = currentLoad.indices.filter { idx -> currentLoad[idx] < capacities[idx] }
        if (availableIndexes.isEmpty()) return -1

        val weights = DoubleArray(pheromones.size)
        var sum = 0.0

        for (i in availableIndexes) {
            val distancePart = (1.0 / distances[i].coerceAtLeast(1e-6)).pow(parameters.beta)
            val pheromonePart = pheromones[i].pow(parameters.alpha)
            val loadRatio = currentLoad[i].toDouble() / capacities[i].coerceAtLeast(1)
            val comfortPart = if (loadRatio <= 1.0) {
                (1.0 - 0.75 * loadRatio).coerceAtLeast(0.1)
            } else {
                (0.08 / (1.0 + (loadRatio - 1.0) * 4.0)).coerceAtLeast(0.01)
            }
            val weight = pheromonePart * distancePart * comfortPart
            weights[i] = if (weight.isFinite() && weight > 0.0) weight else 0.0
            sum += weights[i]
        }

        if (sum <= 0.0) return availableIndexes.random()

        val threshold = Random.nextDouble() * sum
        var acc = 0.0
        for (i in weights.indices) {
            acc += weights[i]
            if (acc >= threshold) return i
        }
        return availableIndexes.last()
    }

    private fun objectiveCost(
        counts: IntArray,
        capacities: IntArray,
        distances: List<Double>,
        unassigned: Int
    ): Double {
        var cost = 0.0
        for (i in counts.indices) {
            val assigned = counts[i]
            cost += assigned * distances[i]
        }
        // Strong penalty to prioritize complete distribution when seats are available.
        cost += unassigned * parameters.overflowPenaltyWeight * 100.0
        return cost
    }

    private fun evaporate(pheromones: DoubleArray) {
        val keep = (1.0 - parameters.evaporationRate).coerceIn(0.0, 1.0)
        for (i in pheromones.indices) {
            pheromones[i] = (pheromones[i] * keep).coerceAtLeast(1e-12)
        }
    }

    private fun deposit(
        pheromones: DoubleArray,
        counts: IntArray,
        capacities: IntArray,
        distances: List<Double>
    ) {
        for (i in pheromones.indices) {
            if (counts[i] == 0) continue
            val loadRatio = counts[i].toDouble() / capacities[i].coerceAtLeast(1)
            val loadPenalty = 1.0 + loadRatio
            val delta = (parameters.pheromoneDepositQ * counts[i]) /
                    (distances[i].coerceAtLeast(1e-6) * loadPenalty)
            if (delta.isFinite() && delta > 0.0) {
                pheromones[i] += delta
            }
        }
    }
}

data class CoworkingCandidate(
    val venueId: Int,
    val name: String,
    val capacity: Int,
    val point: Point
)
