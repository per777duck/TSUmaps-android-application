package com.example.myapplication.data.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

data class MapData(val matrix: Array<IntArray>, val width: Int, val length: Int)
{
    fun isAvailable(x:Int, y:Int): Boolean
    {
        if (x < 0 || x >= width || y < 0 || y >= length) return false
        return matrix[y][x] == 1
    }

    fun getCell(x:Int, y:Int): Int
    {
        if (x < 0 || x >= width || y < 0 || y >= length) return 0
        return matrix[y][x]
    }
}


class MapMatrixLoader(private val context: Context)
{
    fun mapToMatrix(pictureId: Int): MapData
    {
        val picture: Bitmap = BitmapFactory.decodeResource(context.resources, pictureId)
        val matrixSize = 100
        val matrix = Array(matrixSize){ IntArray(matrixSize) }

        for (y in 0 until matrixSize)
        {
            for (x in 0 until matrixSize)
            {
                val pixel = picture.getPixel(x, y)
                val brightness = Color.red(pixel)
                matrix[y][x] = (if (brightness > 128) 1 else 0)
            }
        }

        picture.recycle()
        return MapData(matrix, matrixSize, matrixSize)
    }
}

