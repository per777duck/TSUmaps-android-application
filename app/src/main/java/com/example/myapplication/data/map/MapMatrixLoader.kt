package com.example.myapplication.data.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlin.math.ceil
import androidx.core.graphics.get

class MapData(val matrix: Array<IntArray>, val width: Int, val length: Int)
{
    fun isAvailable(x:Int, y:Int): Boolean
    {
        if (x !in 0..<width || y !in 0..<length) return false
        return matrix[y][x] == 1
    }

    fun getCell(x:Int, y:Int): Int
    {
        if (x !in 0..<width || y !in 0..<length) return 0
        return matrix[y][x]
    }
}


class MapMatrixLoader(private val context: Context)
{
    fun mapToMatrix(pictureId: Int): MapData
    {
        val picture: Bitmap = BitmapFactory.decodeResource(context.resources, pictureId)

        val cellSize = 8
        val matrixWidth = ceil(picture.width.toDouble() / cellSize).toInt()
        val matrixHeight = ceil(picture.height.toDouble() / cellSize).toInt()

        val matrix = Array(matrixHeight){ IntArray(matrixWidth) }

        for (y in 0 until matrixHeight)
        {
            for (x in 0 until matrixWidth)
            {
                val centerX = x * cellSize + cellSize / 2
                val centerY = y * cellSize + cellSize / 2

                if (centerX < picture.width && centerY < picture.height){
                    val pixel = picture[centerX, centerY]

                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)

                    val brightness = (red + green + blue) / 3
                    matrix[x][y] = if (brightness > 128) 1 else 0
                }
            }
        }

        picture.recycle()
        return MapData(matrix, matrixWidth, matrixHeight)
    }
}

