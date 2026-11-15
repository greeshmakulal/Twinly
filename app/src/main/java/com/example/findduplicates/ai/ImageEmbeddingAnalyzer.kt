
package com.example.findduplicates.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import java.io.IOException
import kotlin.math.sqrt

/**
 * MobileNetV3 feature extractor running on-device via Google LiteRT.
 * Extracts a normalized 1D embedding vector for visual similarity comparison.
 */
class ImageEmbeddingAnalyzer(context: Context) : AutoCloseable {

    companion object {
        const val TARGET_INPUT_WIDTH = 224
        const val TARGET_INPUT_HEIGHT = 224
    }

    private val model = CompiledModel.create(
        context.assets,
        "mobilenet_v3_embedder.tflite",
        CompiledModel.Options(Accelerator.CPU)
    )

    /**
     * Efficiently loads and embeds an image from MediaStore URI.
     * Uses inSampleSize subsampling to avoid loading multi-megapixel bitmaps into RAM.
     */
    fun embedImage(context: Context, uri: Uri): FloatArray {
        val resolver = context.contentResolver

    // Step 1: Read image dimensions without allocating bitmap memory
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    val boundsStream = resolver.openInputStream(uri)
        ?: throw IOException("Unable to open image stream: $uri")

    boundsStream.use { stream ->
        // Expected to return null because inJustDecodeBounds = true.
        BitmapFactory.decodeStream(stream, null, options)
    }

    // Step 2: Validate image dimensions
    if (options.outWidth <= 0 || options.outHeight <= 0) {
        throw IOException(
            "Invalid image dimensions: ${options.outWidth}x${options.outHeight}, URI: $uri"
        )
    }

    // Step 3: Calculate optimal sample size
    options.inSampleSize = calculateInSampleSize(
        options.outWidth,
        options.outHeight,
        TARGET_INPUT_WIDTH * 2,
        TARGET_INPUT_HEIGHT * 2
    )

    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.ARGB_8888

    // Step 4: Decode actual subsampled bitmap
    val subsampledBitmap = resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: throw IOException("Unable to decode image bitmap: $uri")

    // Step 5: Generate MobileNetV3 embedding
    return try {
        embedBitmap(subsampledBitmap)
    } finally {
        subsampledBitmap.recycle()
    }
}


    /**
     * Rescales a bitmap to 224x224, prepares input tensor, executes LiteRT inference,
     * and L2-normalizes the output embedding vector.
     */
    fun embedBitmap(bitmap: Bitmap): FloatArray {
        val resized = if (bitmap.width == TARGET_INPUT_WIDTH && bitmap.height == TARGET_INPUT_HEIGHT) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, TARGET_INPUT_WIDTH, TARGET_INPUT_HEIGHT, true)
        }

        try {
            val pixels = IntArray(TARGET_INPUT_WIDTH * TARGET_INPUT_HEIGHT)
            resized.getPixels(pixels, 0, TARGET_INPUT_WIDTH, 0, 0, TARGET_INPUT_WIDTH, TARGET_INPUT_HEIGHT)

            // NHWC input: [1, 224, 224, 3], RGB values normalized to [0.0, 1.0].
            val input = FloatArray(TARGET_INPUT_WIDTH * TARGET_INPUT_HEIGHT * 3)
            var index = 0

            for (pixel in pixels) {
                input[index++] = ((pixel shr 16) and 0xFF) / 255f
                input[index++] = ((pixel shr 8) and 0xFF) / 255f
                input[index++] = (pixel and 0xFF) / 255f
            }

            val inputBuffers = model.createInputBuffers()
            inputBuffers[0].writeFloat(input)

            val outputBuffers = model.run(inputBuffers)
            val rawEmbedding = outputBuffers[0].readFloat()

            // L2-normalize the embedding vector so that ||v|| = 1.0
            // This simplifies cosine similarity to a direct dot product (u · v)
            return l2Normalize(rawEmbedding)
        } finally {
            if (resized !== bitmap) {
                resized.recycle()
            }
        }
    }

    private fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (value in vector) {
            sumSquares += (value * value).toDouble()
        }
        val magnitude = sqrt(sumSquares).toFloat()
        if (magnitude > 1e-6f) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        return vector
    }

    override fun close() {
        model.close()
    }
}
