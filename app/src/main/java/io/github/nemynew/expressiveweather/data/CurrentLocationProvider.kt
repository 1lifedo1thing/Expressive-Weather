package io.github.nemynew.expressiveweather.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import io.github.nemynew.expressiveweather.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * 高精度な現在地情報および詳細な地域名を取得するプロバイダー
 */
class CurrentLocationProvider(context: Context) {

    private val appContext: Context = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            hasLocationPermission()
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) ?: false
        } else {
            true
        }
    }

    companion object {
        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (!BuildConfig.ALLOW_BATTERY_OPTIMIZATION_REQUEST_DIALOG) {
                openAppLocationSettings(context)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }
        }

        fun openAppLocationSettings(context: Context) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(forceFresh: Boolean = false): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        val lastGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val lastNetwork = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val lastPassive = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
        val allKnown = listOfNotNull(lastGps, lastNetwork, lastPassive)
        val freshestKnown = allKnown.maxByOrNull { it.time }
        val bestLastKnown = allKnown
            .filter { (System.currentTimeMillis() - it.time) < 180_000L }
            .minByOrNull { it.accuracy } ?: freshestKnown

        if (!forceFresh && freshestKnown != null && (System.currentTimeMillis() - freshestKnown.time) < 3_000L) {
            return@withContext freshestKnown
        }

        val candidateProviders = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    candidateProviders.add(LocationManager.FUSED_PROVIDER)
                }
            } catch (_: Exception) {}
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                candidateProviders.add(LocationManager.GPS_PROVIDER)
            }
        } catch (_: Exception) {}
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                candidateProviders.add(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Exception) {}

        if (candidateProviders.isNotEmpty()) {
            val freshLocation = withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    @Suppress("DEPRECATION")
                    val cancellationSignal = androidx.core.os.CancellationSignal()
                    cont.invokeOnCancellation { cancellationSignal.cancel() }
                    val returned = AtomicBoolean(false)
                    val remaining = AtomicInteger(candidateProviders.size)

                    for (provider in candidateProviders) {
                        try {
                            @Suppress("DEPRECATION")
                            LocationManagerCompat.getCurrentLocation(
                                locationManager,
                                provider,
                                cancellationSignal,
                                ContextCompat.getMainExecutor(appContext)
                            ) { location ->
                                if (location != null && returned.compareAndSet(false, true)) {
                                    cancellationSignal.cancel()
                                    if (cont.isActive) cont.resume(location)
                                } else if (remaining.decrementAndGet() == 0 && returned.compareAndSet(false, true)) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                        } catch (_: Exception) {
                            if (remaining.decrementAndGet() == 0 && returned.compareAndSet(false, true)) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    }
                }
            }
            if (freshLocation != null) {
                return@withContext freshLocation
            }
        }

        bestLastKnown ?: allKnown.maxByOrNull { it.time }
    }

    private fun resolveUpdateProviders(): List<String> {
        val lm = locationManager ?: return emptyList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    return listOf(LocationManager.FUSED_PROVIDER)
                }
            } catch (_: Exception) {}
        }
        val providers = mutableListOf<String>()
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {}
        try {
            if (providers.isEmpty() && lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                providers.add(LocationManager.PASSIVE_PROVIDER)
            }
        } catch (_: Exception) {}
        return providers
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        minTime: Long = 5_000L,
        minDistance: Float = 30f,
        onLocationChanged: (Location) -> Unit
    ): () -> Unit {
        if (!hasLocationPermission() || locationManager == null) return {}

        val listener = android.location.LocationListener { location ->
            onLocationChanged(location)
        }

        for (provider in resolveUpdateProviders()) {
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    minTime,
                    minDistance,
                    listener,
                    appContext.mainLooper
                )
            } catch (_: Exception) {}
        }

        return {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun registerBackgroundLocationUpdates(
        pendingIntent: android.app.PendingIntent,
        minTime: Long = 5 * 60 * 1000L,
        minDistance: Float = 250f
    ) {
        if (!hasLocationPermission() || !hasBackgroundLocationPermission() || locationManager == null) return

        for (provider in resolveUpdateProviders()) {
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    minTime,
                    minDistance,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun unregisterBackgroundLocationUpdates(pendingIntent: android.app.PendingIntent) {
        if (locationManager == null) return
        try {
            locationManager.removeUpdates(pendingIntent)
        } catch (_: Exception) {}
    }

    suspend fun resolveCity(location: Location): City = withContext(Dispatchers.IO) {
        val lat = location.latitude
        val lon = location.longitude
        val inJapan = isLocationOrCountryInJapan(null, lat, lon)

        var displayName = ""
        var englishName = ""
        var country = if (inJapan) "日本" else ""

        try {
            coroutineScope {
                val jaDeferred = async {
                    withTimeoutOrNull(2000L) {
                        val jaGeocoder = Geocoder(appContext, Locale.JAPAN)
                        reverseGeocodeSingle(jaGeocoder, lat, lon)
                    }
                }
                val enDeferred = async {
                    withTimeoutOrNull(1500L) {
                        val enGeocoder = Geocoder(appContext, Locale.US)
                        reverseGeocodeSingle(enGeocoder, lat, lon)
                    }
                }

                jaDeferred.await()?.let { addr ->
                    displayName = resolveDetailedName(addr)
                    country = addr.countryName ?: addr.adminArea ?: country
                }

                enDeferred.await()?.let { addr ->
                    englishName = resolveDetailedName(addr)
                    if (country.isBlank()) {
                        country = addr.countryName ?: addr.adminArea ?: ""
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Geocoder 失敗時はフォールバック
        }

        if (inJapan && (displayName.isBlank() || displayName == "現在地")) {
            val jmaInfo = JmaAreaResolver.resolve(lat, lon)
            displayName = jmaInfo.stationName.ifBlank { jmaInfo.officeName }
            if (englishName.isBlank() || englishName == "Current Location") {
                englishName = displayName
            }
        }

        if (englishName.isBlank() || englishName == "Current Location") {
            englishName = if (displayName.isNotBlank() && displayName != "現在地") displayName else "Current Location"
        }

        if (displayName.isBlank()) {
            displayName = "現在地"
        }

        val resolved = City(
            id = "current",
            name = displayName,
            nameEn = englishName,
            country = country,
            latitude = lat,
            longitude = lon,
            isFavorite = false
        )
        WeatherRepository.setLastResolvedLocationCity(appContext, resolved)
        resolved
    }

    private suspend fun reverseGeocodeSingle(
        geocoder: Geocoder,
        lat: Double,
        lon: Double
    ): android.location.Address? {
        if (!Geocoder.isPresent()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(
                    lat,
                    lon,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        }
    }

    private fun resolveDetailedName(addr: android.location.Address): String {
        val admin = addr.adminArea?.trim().orEmpty()
        val locality = addr.locality?.trim().orEmpty()
        val subLocality = addr.subLocality?.trim().orEmpty()
        val thoroughfare = addr.thoroughfare?.trim().orEmpty()

        return when {
            locality.endsWith("市") && subLocality.endsWith("区") -> "$locality $subLocality"
            locality.endsWith("区") && subLocality.isNotBlank() && !subLocality.endsWith("市") -> "$locality $subLocality"
            locality.isNotBlank() && subLocality.isNotBlank() && !subLocality.endsWith("市") -> "$locality $subLocality"
            locality.isNotBlank() -> locality
            subLocality.isNotBlank() -> subLocality
            admin.isNotBlank() && thoroughfare.isNotBlank() -> "$admin $thoroughfare"
            admin.isNotBlank() -> admin
            else -> "現在地"
        }
    }
}
