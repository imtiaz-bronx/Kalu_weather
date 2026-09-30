package com.imtiazmahmud.KaluWeather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

// =====================================================================
//  THE USER'S LOCATION - using only Android's built-in LocationManager.
//  No Google Play Services, so the app is 100% open source and can be
//  published on F-Droid (and it still works the same on Play Store).
//
//  1) Permission must be listed in AndroidManifest.xml AND asked for.
//  2) We ask the phone for a fresh location (wait max 10 seconds).
//     If that fails, we use the last location the phone remembers.
//  3) Android's Geocoder turns the numbers into a place name.
// =====================================================================
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun currentPlace(context: Context): Place? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val location = withTimeoutOrNull(10_000) { freshLocation(context, manager) }
            ?: lastKnownLocation(manager)
            ?: return null

        val (name, region) = placeName(context, location.latitude, location.longitude)
        return Place(name, region, location.latitude, location.longitude)
    }

    /** Which location source to use: "fused" (newer phones), then network, then GPS. */
    private fun bestProvider(manager: LocationManager): String? {
        val available = try {
            manager.getProviders(true)      // only enabled ones we're allowed to use
        } catch (e: Exception) {
            emptyList()
        }
        return listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { it in available }
    }

    @SuppressLint("MissingPermission") // checked in currentPlace()
    private suspend fun freshLocation(context: Context, manager: LocationManager): Location? {
        val provider = bestProvider(manager) ?: return null

        return suspendCancellableCoroutine { cont ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11 and newer
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                } else {
                    // Android 7 - 10
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (cont.isActive) cont.resume(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    cont.invokeOnCancellation { manager.removeUpdates(listener) }
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission") // checked in currentPlace()
    private fun lastKnownLocation(manager: LocationManager): Location? =
        try {
            manager.getProviders(true)
                .mapNotNull { provider ->
                    try {
                        manager.getLastKnownLocation(provider)
                    } catch (e: Exception) {
                        null
                    }
                }
                .maxByOrNull { it.time }   // the newest one
        } catch (e: Exception) {
            null
        }

    private suspend fun placeName(context: Context, lat: Double, lon: Double): Pair<String, String> =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
                (a?.subLocality ?: a?.locality ?: "My location") to (a?.adminArea ?: "")
            } catch (e: Exception) {
                "My location" to ""
            }
        }
}
