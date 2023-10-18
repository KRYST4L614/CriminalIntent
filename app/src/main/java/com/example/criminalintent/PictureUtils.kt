package com.example.criminalintent

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import kotlin.math.max
import kotlinx.coroutines.*

suspend fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap {
    //Чтение размеров изображения на диске
    var options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    //Выясняем на сколько надо уменьшить
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale = max(heightScale, widthScale)
        inSampleSize = Math.round(sampleScale)
    }

    options = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
    }
    //Чтение и создание окончательного растрового изображения
    return BitmapFactory.decodeFile(path, options)
}

suspend fun getScaledBitmap(path: String, activity: Activity): Bitmap {
    val size = Point()
    activity.windowManager.defaultDisplay.getSize(size)

    return getScaledBitmap(path, size.x, size.y)
}