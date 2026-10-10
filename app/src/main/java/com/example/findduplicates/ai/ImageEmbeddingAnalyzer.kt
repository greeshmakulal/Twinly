
package com.example.findduplicates.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import java.io.IOException

class ImageEmbeddingAnalyzer(context: Context) : AutoCloseable {

    private val model = CompiledModel.create(
        context.assets,
        "mobilenet_v3_embedder.tflite",
        CompiledModel.Options(Accelerator.CPU)
    )

    fun embedImage(context: Context, uri: Uri): FloatArray {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IOException("Unable to decode image: $uri")

        return try {
            embedBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun embedBitmap(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        try {
            val pixels = IntArray(224 * 224)
            resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)

            // NHWC input: [1, 224, 224, 3], RGB values in [0, 1].
            val input = FloatArray(224 * 224 * 3)
            var index = 0

            for (pixel in pixels) {
                input[index++] = ((pixel shr 16) and 0xFF) / 255f
                input[index++] = ((pixel shr 8) and 0xFF) / 255f
                input[index++] = (pixel and 0xFF) / 255f
            }

            val inputBuffers = model.createInputBuffers()
            inputBuffers[0].writeFloat(input)

            val outputBuffers = model.run(inputBuffers)
            return outputBuffers[0].readFloat()
        } finally {
            if (resized !== bitmap) {
                resized.recycle()
            }
        }
    }

    override fun close() {
        model.close()
    }
}
