package com.example.myapplication.algorithms.neural

object NeuralPreprocessor {
    const val InputWidth = 5
    const val InputHeight = 5
    const val InputSize = InputWidth * InputHeight

    fun toInputVector(pixels: List<Boolean>): FloatArray {
        require(pixels.size == InputSize) {
            "Ожидалось $InputSize пикселей, получил ${pixels.size}"
        }
        return FloatArray(InputSize) { index ->
            if (pixels[index]) 1f else 0f
        }
    }
}
