package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapMatrixLoader

class MainActivity : AppCompatActivity()
{
    private lateinit var mapData: MapData
    private lateinit var mapMatrixLoader: MapMatrixLoader

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapMatrixLoader = MapMatrixLoader(this)
        mapData = mapMatrixLoader.mapToMatrix(R.drawable.map)

    }
}