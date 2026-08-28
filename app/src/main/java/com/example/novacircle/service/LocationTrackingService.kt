package com.example.novacircle.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.novacircle.R
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var client: SupabaseClient

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { pushLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startLocationUpdates() {
        android.util.Log.d("LocationService", "Starting location updates...")
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30_000L
        ).setMinUpdateIntervalMillis(15_000L).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, mainLooper
            )
            android.util.Log.i("LocationService", "Location updates requested successfully")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "Missing location permission", e)
            stopSelf()
        }
    }

    private fun pushLocation(location: Location) {
        val currentUser = client.auth.currentUserOrNull()
        val uid = currentUser?.id ?: run {
            android.util.Log.w("LocationService", "No user logged in, skipping push")
            return
        }

        android.util.Log.d("LocationService", "Preparing to push location for $uid. Accuracy: ${location.accuracy}")

        serviceScope.launch {
            runCatching {
                val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    java.time.Instant.now().toString()
                } else {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    sdf.format(java.util.Date())
                }

                val payload = mapOf(
                    "user_id" to uid,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "battery_percent" to getBatteryPercent(),
                    "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "updated_at" to timestamp
                )
                android.util.Log.d("LocationService", "Payload: $payload")
                
                client.postgrest["locations"].upsert(payload) { onConflict = "user_id" }
            }.onSuccess {
                android.util.Log.i("LocationService", "Location successfully pushed to Supabase at ${System.currentTimeMillis()}")
            }.onFailure { e ->
                android.util.Log.e("LocationService", "CRITICAL: Failed to push location", e)
                e.printStackTrace()
            }
        }
    }

    private fun getBatteryPercent(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun buildNotification(): Notification {
        val channelId = "nova_location_tracking"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Location Sharing", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NovaCircle")
            .setContentText("Sharing your location with your circles")
            .setSmallIcon(R.drawable.ic_nova_logo)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}