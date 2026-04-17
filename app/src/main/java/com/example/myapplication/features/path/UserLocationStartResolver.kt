package com.example.myapplication.features.path

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.GeoBounds
import com.example.myapplication.algorithms.routes.Node
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class StartNodeResolveStatus {
    SUCCESS,
    PERMISSION_DENIED,
    OUT_OF_MAP_BOUNDS,
    LOCATION_UNAVAILABLE
}

data class StartNodeResolveResult(
    val node: Node?,
    val status: StartNodeResolveStatus
)

object UserLocationStartResolver {
    // These bounds should contain the whole university grove.
    // If your map image/bounds change, update these values.
    private val CAMPUS_GEO_BOUNDS = GeoBounds(
        northLatitude = 56.47720,
        southLatitude = 56.46870,
        westLongitude = 84.94450,
        eastLongitude = 84.96280
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun resolveStartNode(context: Context, algorithm: AStarAlgorithm): StartNodeResolveResult {
        if (!hasLocationPermission(context)) {
            return StartNodeResolveResult(
                node = null,
                status = StartNodeResolveStatus.PERMISSION_DENIED
            )
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = fetchCurrentLocation(client) ?: fetchLastLocation(client)
            ?: return StartNodeResolveResult(
                node = null,
                status = StartNodeResolveStatus.LOCATION_UNAVAILABLE
            )

        val node = algorithm.geoToWalkableNode(
            latitude = location.latitude,
            longitude = location.longitude,
            bounds = CAMPUS_GEO_BOUNDS
        )
        return if (node != null) {
            StartNodeResolveResult(node = node, status = StartNodeResolveStatus.SUCCESS)
        } else {
            StartNodeResolveResult(node = null, status = StartNodeResolveStatus.OUT_OF_MAP_BOUNDS)
        }
    }

    private suspend fun fetchCurrentLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }

        continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
    }

    private suspend fun fetchLastLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
    }
}
