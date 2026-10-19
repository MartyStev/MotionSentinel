package io.github.martystev.motionsentinel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

class GpsProvider(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun getCurrentLocation(timeoutMs: Long = 10_000, onResult: (Location?) -> Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            onResult(null)
            return
        }

        val criteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
        }

        val handler = Handler(Looper.getMainLooper())
        var delivered = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (delivered) return
                delivered = true
                locationManager.removeUpdates(this)
                onResult(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
            }
        }

        handler.postDelayed({
            if (!delivered) {
                delivered = true
                locationManager.removeUpdates(listener)
                onResult(null)
            }
        }, timeoutMs)

        try {
            locationManager.requestSingleUpdate(criteria, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            if (!delivered) {
                delivered = true
                onResult(null)
            }
        }
    }
}
