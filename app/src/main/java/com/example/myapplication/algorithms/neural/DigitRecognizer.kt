package com.example.myapplication.algorithms.neural

private const val MinActivePixels = 3
private const val MinConfidence = 0.40f

class DigitRecognizer(
    private val network: SimpleNeuralNet = SimpleNeuralNet(
        weights = TrainedDigitModel.weights,
        biases = TrainedDigitModel.biases
    )
) {
    fun predict(pixels: List<Boolean>): DigitPrediction? {
        if (pixels.count { it } < MinActivePixels) return null
        val input = NeuralPreprocessor.toInputVector(pixels)
        val prediction = network.predict(input)
        return prediction.takeIf { it.confidence >= MinConfidence }
    }
}
