package com.example.myapplication.algorithms

import com.example.myapplication.data.venues.FoodItem
import com.example.myapplication.data.venues.FoodVenue
import com.example.myapplication.features.path.CampusPathPlanner
import com.example.myapplication.features.path.CampusRoutingContext
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.random.Random

data class RouteStop(
    val venue: FoodVenue,
    val arrivalMinuteOfDay: Int,
    val departureMinuteOfDay: Int,
    val minutesFromStart: Int,
    val purchasedItems: Set<FoodItem>
)

data class GeneticIterationUpdate(
    val generation: Int,
    val totalGenerations: Int,
    val bestTravelMinutes: Int,
    val route: List<RouteStop>,
    val collectedItems: Set<FoodItem>,
    val missingItems: Set<FoodItem>,
    val fitnessScore: Double
)

data class GeneticMealRouteResult(
    val bestTravelMinutes: Int,
    val route: List<RouteStop>,
    val collectedItems: Set<FoodItem>,
    val missingItems: Set<FoodItem>,
    val fitnessScore: Double
)

object MealRouteGeneticAlgorithm {

    suspend fun optimize(
        venues: List<FoodVenue>,
        requiredItems: Set<FoodItem>,
        routing: CampusRoutingContext,
        currentMinuteOfDay: Int,
        generations: Int = 160,
        populationSize: Int = 56,
        mutationRate: Double = 0.22,
        onIteration: (GeneticIterationUpdate) -> Unit
    ): GeneticMealRouteResult {
        if (requiredItems.isEmpty()) {
            val emptyResult = GeneticMealRouteResult(
                bestTravelMinutes = 0,
                route = emptyList(),
                collectedItems = emptySet(),
                missingItems = emptySet(),
                fitnessScore = 0.0
            )
            onIteration(
                GeneticIterationUpdate(
                    generation = 0,
                    totalGenerations = generations,
                    bestTravelMinutes = 0,
                    route = emptyList(),
                    collectedItems = emptySet(),
                    missingItems = emptySet(),
                    fitnessScore = 0.0
                )
            )
            return emptyResult
        }

        val random = Random(System.currentTimeMillis())
        val baseGenome = venues.map { it.id }
        var population = MutableList(populationSize) { baseGenome.shuffled(random) }

        var bestGenome = population.first()
        var bestEvaluation = evaluateGenome(
            genome = bestGenome,
            venues = venues,
            requiredItems = requiredItems,
            routing = routing,
            currentMinuteOfDay = currentMinuteOfDay
        )

        repeat(generations) { generation ->
            val scoredPopulation = population.map { genome ->
                genome to evaluateGenome(
                    genome = genome,
                    venues = venues,
                    requiredItems = requiredItems,
                    routing = routing,
                    currentMinuteOfDay = currentMinuteOfDay
                )
            }.sortedBy { it.second.fitness }

            val generationBest = scoredPopulation.first()
            if (generationBest.second.fitness < bestEvaluation.fitness) {
                bestGenome = generationBest.first
                bestEvaluation = generationBest.second
            }

            onIteration(
                GeneticIterationUpdate(
                    generation = generation + 1,
                    totalGenerations = generations,
                    bestTravelMinutes = bestEvaluation.totalMinutes,
                    route = bestEvaluation.route,
                    collectedItems = bestEvaluation.collectedItems,
                    missingItems = bestEvaluation.missingItems,
                    fitnessScore = bestEvaluation.fitness
                )
            )

            val eliteCount = max(2, populationSize / 8)
            val elites = scoredPopulation.take(eliteCount).map { it.first }
            val nextPopulation = mutableListOf<List<Int>>()
            nextPopulation.addAll(elites)

            while (nextPopulation.size < populationSize) {
                val parentA = tournamentSelection(scoredPopulation, random)
                val parentB = tournamentSelection(scoredPopulation, random)
                var child = orderedCrossover(parentA, parentB, random)
                child = mutate(child, random, mutationRate)
                nextPopulation += child
            }

            population = nextPopulation.toMutableList()
            if (generation % 8 == 0) {
                yield()
            }
        }

        return GeneticMealRouteResult(
            bestTravelMinutes = bestEvaluation.totalMinutes,
            route = bestEvaluation.route,
            collectedItems = bestEvaluation.collectedItems,
            missingItems = bestEvaluation.missingItems,
            fitnessScore = bestEvaluation.fitness
        )
    }

    private data class GenomeEvaluation(
        val fitness: Double,
        val totalMinutes: Int,
        val route: List<RouteStop>,
        val collectedItems: Set<FoodItem>,
        val missingItems: Set<FoodItem>
    )

    private fun evaluateGenome(
        genome: List<Int>,
        venues: List<FoodVenue>,
        requiredItems: Set<FoodItem>,
        routing: CampusRoutingContext,
        currentMinuteOfDay: Int
    ): GenomeEvaluation {
        val byId = venues.associateBy { it.id }
        val collected = mutableSetOf<FoodItem>()
        val routeStops = mutableListOf<RouteStop>()
        var currentMatrixIndex = 0
        var elapsedMinutes = 0
        var urgencyReward = 0.0
        var operationalPenalty = 0.0
        val matrix = routing.travelMinutesMatrix
        val serviceMinutes = CampusPathPlanner.serviceMinutesPerStop()

        for (venueId in genome) {
            if (collected.containsAll(requiredItems)) break
            val venue = byId[venueId] ?: continue
            val neededHere = venue.menu.intersect(requiredItems - collected)
            if (neededHere.isEmpty()) continue

            val toIndex = CampusPathPlanner.matrixIndexForVenueId(venues, venueId)
            val travelMinutes = matrix[currentMatrixIndex][toIndex]
                .toInt()
                .coerceAtLeast(1)
            val arrival = currentMinuteOfDay + elapsedMinutes + travelMinutes
            val minuteOfDay = normalizeMinute(arrival)
            val isOpen = isVenueOpen(venue, minuteOfDay)

            if (!isOpen) {
                operationalPenalty += 320.0
                continue
            }

            elapsedMinutes += travelMinutes + serviceMinutes
            collected += neededHere
            currentMatrixIndex = toIndex
            routeStops += RouteStop(
                venue = venue,
                arrivalMinuteOfDay = minuteOfDay,
                departureMinuteOfDay = normalizeMinute(arrival + serviceMinutes),
                minutesFromStart = elapsedMinutes,
                purchasedItems = neededHere
            )

            val minutesLeftToClose = minutesUntilClose(venue, minuteOfDay)
            val urgency = ((90 - minutesLeftToClose).coerceAtLeast(0)) / 90.0
            urgencyReward += urgency * neededHere.size * 14.0
        }

        val missingItems = requiredItems - collected
        val missingPenalty = missingItems.size * 900.0
        val extraStopsPenalty = routeStops.size * 14.0
        val timePenalty = elapsedMinutes.toDouble()
        val fitness = timePenalty + missingPenalty + extraStopsPenalty + operationalPenalty - urgencyReward

        return GenomeEvaluation(
            fitness = fitness,
            totalMinutes = elapsedMinutes,
            route = routeStops,
            collectedItems = collected,
            missingItems = missingItems
        )
    }

    private fun isVenueOpen(venue: FoodVenue, minuteOfDay: Int): Boolean {
        return minuteOfDay in venue.openFromMinutes until venue.closeAtMinutes
    }

    private fun minutesUntilClose(venue: FoodVenue, minuteOfDay: Int): Int {
        return venue.closeAtMinutes - minuteOfDay
    }

    private fun normalizeMinute(minutes: Int): Int {
        var value = minutes % (24 * 60)
        if (value < 0) value += 24 * 60
        return value
    }

    private fun tournamentSelection(
        scoredPopulation: List<Pair<List<Int>, GenomeEvaluation>>,
        random: Random,
        tournamentSize: Int = 4
    ): List<Int> {
        val candidates = List(tournamentSize) { scoredPopulation[random.nextInt(scoredPopulation.size)] }
        return candidates.minByOrNull { it.second.fitness }!!.first
    }

    private fun orderedCrossover(parentA: List<Int>, parentB: List<Int>, random: Random): List<Int> {
        val size = parentA.size
        if (size <= 2) return parentA

        val from = random.nextInt(size)
        val to = random.nextInt(from, size)
        val slice = parentA.subList(from, to + 1).toSet()
        val child = MutableList(size) { -1 }

        for (i in from..to) {
            child[i] = parentA[i]
        }

        var pointer = 0
        for (gene in parentB) {
            if (gene in slice) continue
            while (child[pointer] != -1) {
                pointer++
            }
            child[pointer] = gene
        }
        return child
    }

    private fun mutate(genome: List<Int>, random: Random, mutationRate: Double): List<Int> {
        if (random.nextDouble() > mutationRate || genome.size < 2) return genome

        val mutableGenome = genome.toMutableList()
        val i = random.nextInt(mutableGenome.size)
        var j = random.nextInt(mutableGenome.size)
        while (j == i) {
            j = random.nextInt(mutableGenome.size)
        }
        val temp = mutableGenome[i]
        mutableGenome[i] = mutableGenome[j]
        mutableGenome[j] = temp
        return mutableGenome
    }
}
