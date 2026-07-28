package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import java.io.InputStream

object ImagePreprocessor {

    /**
     * Preprocesses a bitmap for OCR and vision models by converting to grayscale,
     * enhancing contrast, and adjusting brightness so text on paper/receipts becomes clear.
     *
     * @param source The original input Bitmap
     * @param contrast Factor for contrast enhancement (1.0 = normal, 1.4 = high contrast)
     * @param brightness Offset for brightness (+10f to brighten dark paper background)
     */
    fun preprocessForOcr(
        source: Bitmap,
        contrast: Float = 1.45f,
        brightness: Float = 10f
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 1. Grayscale Matrix (Removes color noise from background/paper tint)
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        // 2. High Contrast & Brightness Matrix
        // Formula: Output = contrast * (Input - 128) + 128 + brightness
        val contrastMatrix = ColorMatrix()
        val c = contrast
        val offset = (128f * (1f - c)) + brightness

        contrastMatrix.set(
            floatArrayOf(
                c, 0f, 0f, 0f, offset,
                0f, c, 0f, 0f, offset,
                0f, 0f, c, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Combine Grayscale and Contrast adjustment
        val combinedMatrix = ColorMatrix()
        combinedMatrix.postConcat(grayscaleMatrix)
        combinedMatrix.postConcat(contrastMatrix)

        // Draw onto a high quality ARGB_8888 bitmap canvas
        val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(processedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)
        return processedBitmap
    }

    /**
     * Helper to load, scale, and preprocess image from Uri.
     */
    fun loadAndPreprocessUri(context: Context, uri: Uri, maxDimension: Int = 1280): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream) ?: return null

            val width = original.width
            val height = original.height

            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val aspectRatio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int

                if (width > height) {
                    newWidth = maxDimension
                    newHeight = (maxDimension / aspectRatio).toInt()
                } else {
                    newHeight = maxDimension
                    newWidth = (maxDimension * aspectRatio).toInt()
                }
                Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
            } else {
                original
            }

            preprocessForOcr(scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
