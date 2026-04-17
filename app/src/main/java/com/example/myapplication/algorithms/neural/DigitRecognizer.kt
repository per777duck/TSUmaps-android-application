package com.example.myapplication.algorithms.neural

private const val ActivePixelWeight = 1.15f
private const val InactivePixelWeight = -0.45f
private const val MinActivePixels = 3
private const val MinConfidence = 0.40f

class DigitRecognizer(
    private val network: SimpleNeuralNet = SimpleNeuralNet(
        weights = buildTemplateWeights(),
        biases = FloatArray(10) { -4.5f }
    )
) {
    fun predict(pixels: List<Boolean>): DigitPrediction? {
        if (pixels.count { it } < MinActivePixels) return null
        val input = NeuralPreprocessor.toInputVector(pixels)
        val prediction = network.predict(input)
        return prediction.takeIf { it.confidence >= MinConfidence }
    }
}

private fun buildTemplateWeights(): Array<FloatArray> {
    val templates = listOf(
        "11111|10001|10001|10001|11111",
        "00100|01100|00100|00100|01110",
        "11111|00001|11111|10000|11111",
        "11111|00001|01111|00001|11111",
        "10001|10001|11111|00001|00001",
        "11111|10000|11111|00001|11111",
        "11111|10000|11111|10001|11111",
        "11111|00001|00010|00100|00100",
        "11111|10001|11111|10001|11111",
        "11111|10001|11111|00001|11111"
    )

    return templates.map(::templateToWeights).toTypedArray()
}

private fun templateToWeights(template: String): FloatArray {
    val compact = template.replace("|", "")
    require(compact.length == NeuralPreprocessor.InputSize) {
        "Template must contain ${NeuralPreprocessor.InputSize} cells"
    }
    return FloatArray(compact.length) { index ->
        if (compact[index] == '1') ActivePixelWeight else InactivePixelWeight
    }
}
