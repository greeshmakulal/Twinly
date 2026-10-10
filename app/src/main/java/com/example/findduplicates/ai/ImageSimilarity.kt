package com.example.findduplicates.ai

import kotlin.math.sqrt

object ImageSimilarity {

    fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {

        require(first.isNotEmpty() && second.isNotEmpty()) {
            "Embeddings cannot be empty"
        }

        require(first.size == second.size) {
            "Embedding dimensions must match"
        }

        var dotProduct = 0.0
        var firstMagnitude = 0.0
        var secondMagnitude = 0.0

        for (i in first.indices) {
            val a = first[i].toDouble()
            val b = second[i].toDouble()

            dotProduct += a * b
            firstMagnitude += a * a
            secondMagnitude += b * b
        }

        val denominator =
            sqrt(firstMagnitude) * sqrt(secondMagnitude)

        if (denominator == 0.0) return 0f

        return (dotProduct / denominator).toFloat()
    }

    fun areSimilar(
        first: FloatArray,
        second: FloatArray,
        threshold: Float = 0.90f
    ): Boolean {
        return cosineSimilarity(first, second) >= threshold
    }
}