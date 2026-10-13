
package com.example.findduplicates.data.local.converters

import androidx.room3.ColumnTypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmbeddingTypeConverter {

    @ColumnTypeConverter
    fun fromFloatArray(array: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(array.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.asFloatBuffer().put(array)
        return buffer.array()
    }

    @ColumnTypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)

        val floatBuffer = buffer.asFloatBuffer()
        val array = FloatArray(floatBuffer.remaining())
        floatBuffer.get(array)
        return array
    }
}
