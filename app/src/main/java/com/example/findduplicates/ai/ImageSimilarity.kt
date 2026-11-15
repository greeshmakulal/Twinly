package com.example.findduplicates.ai

import kotlin.math.sqrt

/**
 * High-performance cosine similarity computation for image & video feature embeddings.
 */
object ImageSimilarity {

    /**
     * Fast dot-product cosine similarity for pre-normalized L2 unit vectors.
     * When ||u|| = 1 and ||v|| = 1, cos(theta) = u · v.
     * Eliminates two square roots and division on every pairwise calculation.
     */
    fun dotProductSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {
        if (first.isEmpty() || second.isEmpty() || first.size != second.size) return 0f

        var dotProduct = 0.0f
        for (i in first.indices) {
            dotProduct += first[i] * second[i]
        }
        return dotProduct.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Standard cosine similarity with magnitude calculation.
     * Backward-compatible with non-normalized vectors.
     */
    fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {
        if (first.isEmpty() || second.isEmpty() || first.size != second.size) {
            return 0f
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

        val denominator = sqrt(firstMagnitude) * sqrt(secondMagnitude)
        if (denominator == 0.0) return 0f

        return (dotProduct / denominator).toFloat().coerceIn(-1.0f, 1.0f)
    }

    /**
     * Checks if two embeddings exceed the similarity threshold.
     */
    fun areSimilar(
        first: FloatArray,
        second: FloatArray,
        threshold: Float = 0.90f
    ): Boolean {
        return dotProductSimilarity(first, second) >= threshold
    }
}