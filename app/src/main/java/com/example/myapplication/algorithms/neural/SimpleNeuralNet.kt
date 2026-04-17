package com.example.myapplication.algorithms.neural

import kotlin.math.exp

class SimpleNeuralNet(
    private val weights: Array<FloatArray>,
    private val biases: FloatArray
) {
    init {
        require(weights.size == biases.size) {
            "Weights and biases sizes must match"
        }
        require(weights.all { it.size == NeuralPreprocessor.InputSize }) {
            "Each neuron must have ${NeuralPreprocessor.InputSize} weights"
        }
    }

    fun predict(input: FloatArray): DigitPrediction {
        require(input.size == NeuralPreprocessor.InputSize) {
            "Expected ${NeuralPreprocessor.InputSize} inputs, got ${input.size}"
        }

        val logits = FloatArray(weights.size) { digit ->
            val weightedSum = weights[digit].indices.sumOf { index ->
                (weights[digit][index] * input[index]).toDouble()
            }.toFloat()
            weightedSum + biases[digit]
        }

        val probabilities = softmax(logits)
        var bestIndex = 0
        for (index in 1 until probabilities.size) {
            if (probabilities[index] > probabilities[bestIndex]) {
                bestIndex = index
            }
        }
        return DigitPrediction(digit = bestIndex, confidence = probabilities[bestIndex])
    }

    private fun softmax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0f
        val exps = FloatArray(values.size) { index ->
            exp((values[index] - max).toDouble()).toFloat()
        }
        val total = exps.sum().coerceAtLeast(1e-6f)
        return FloatArray(values.size) { index -> exps[index] / total }
    }
}
