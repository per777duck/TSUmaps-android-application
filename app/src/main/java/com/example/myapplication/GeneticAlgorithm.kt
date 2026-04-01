package com.example.myapplication

import kotlinx.coroutines.delay

class FoodGeneticAlgorithm(
    private val requiredItems: Set<FoodItem>,
    private val startNode: Node,
    private val currentHour: Int,
    private val grid: Array<IntArray>
) {
    private val popSize = 40
    private val speedKmH = 5.0
    private val aStar = AStarAlgorithm(grid)


    private val pathCache = mutableMapOf<String, List<Node>>()
    private val distanceMatrix = mutableMapOf<String, Double>()

    private suspend fun prepareDistances() {
        val allPoints = tguVenues + Venue(-1, "Start", startNode.x, startNode.y, emptySet(), 24)

        for (v1 in allPoints) {
            for (v2 in allPoints) {
                if (v1 == v2) continue
                val key = "${v1.id}_${v2.id}"
                if (!pathCache.containsKey(key)) {
                    val path = aStar.findPath(Node(v1.x, v1.y), Node(v2.x, v2.y), speedMs = 0) { }
                    if (path != null) {
                        pathCache[key] = path
                        distanceMatrix[key] = path.size.toDouble()
                    } else {
                        distanceMatrix[key] = 10000.0
                    }
                }
            }
        }
    }

    data class Individual(val route: List<Venue>, var fitness: Double = 0.0)

    fun calculateFitness(ind: Individual): Double {
        var dist = 0.0
        var time = currentHour.toDouble()
        val gathered = mutableSetOf<FoodItem>()
        var lastId = -1

        for (venue in ind.route) {
            if (gathered.containsAll(requiredItems)) break

            val d = distanceMatrix["${lastId}_${venue.id}"] ?: 1000.0
            dist += d
            time += (d / 10.0) / speedKmH

            if (time >= venue.closingHour) dist += 5000.0
            if (venue.closingHour - time < 1.0) dist -= 10.0

            gathered.addAll(venue.menu.intersect(requiredItems))
            lastId = venue.id
        }

        if (!gathered.containsAll(requiredItems)) dist += 20000.0
        return 1.0 / (dist + 1.0)
    }

    suspend fun solve(onUpdate: (List<Node>) -> Unit): List<Node> {
        prepareDistances()

        var population = List(popSize) { Individual(tguVenues.shuffled()) }

        repeat(60) {
            population.forEach { it.fitness = calculateFitness(it) }
            val sorted = population.sortedByDescending { it.fitness }

            onUpdate(constructFullPath(sorted[0]))

            val nextGen = mutableListOf<Individual>()
            nextGen.add(sorted[0])

            while (nextGen.size < popSize) {
                val p1 = sorted.take(10).random()
                nextGen.add(mutate(p1))
            }
            population = nextGen
            delay(30)
        }
        return constructFullPath(population.maxBy { it.fitness })
    }

    private fun constructFullPath(ind: Individual): List<Node> {
        val fullPath = mutableListOf<Node>()
        var lastId = -1
        val gathered = mutableSetOf<FoodItem>()

        for (v in ind.route) {
            val segment = pathCache["${lastId}_${v.id}"] ?: emptyList()
            fullPath.addAll(segment)

            gathered.addAll(v.menu.intersect(requiredItems))
            if (gathered.containsAll(requiredItems)) break
            lastId = v.id
        }
        return fullPath
    }

    private fun mutate(ind: Individual): Individual {
        val list = ind.route.toMutableList()
        if (list.size < 2) return ind
        val i = list.indices.random()
        val j = list.indices.random()
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
        return Individual(list)
    }
}