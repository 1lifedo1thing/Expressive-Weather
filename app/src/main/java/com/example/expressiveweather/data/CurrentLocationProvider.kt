package com.example.expressiveweather.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 高精度な現在地情報および詳細な地域名を取得するプロバイダー
 */
class CurrentLocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * 位置情報権限が付与されているかチェック
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * バックグラウンド位置情報権限 (常に許可) が付与されているかチェック
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            hasLocationPermission()
        }
    }

    /**
     * バッテリーの最適化（省電力制限）が解除（制限なし）されているかチェック
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    companion object {
        /**
         * バッテリー最適化の解除要求インテントを発行
         */
        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }
        }

        /**
         * アプリの位置情報権限設定画面を開く（常に許可の選択へ誘導）
         */
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

    /**
     * GPS / Network / Fused から端末のリアルタイム現在地を高精度かつ高速に取得
     * @param forceFresh true の場合、直近のキャッシュをスキップして即座に最新測位を試行する
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(forceFresh: Boolean = false): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        // 1. 直近の精度の高い既知の位置情報を確認
        val lastGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val lastNetwork = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val lastPassive = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
        val allKnown = listOfNotNull(lastGps, lastNetwork, lastPassive)
        val freshestKnown = allKnown.maxByOrNull { it.time }
        val bestLastKnown = allKnown
            .filter { (System.currentTimeMillis() - it.time) < 180_000L } // 3分以内
            .minByOrNull { it.accuracy } ?: freshestKnown

        // forceFreshでなく、かつ直近3秒以内の超最新位置情報があれば、即座に返却（短期連続呼び出しの重複測位をスキップ）
        if (!forceFresh && freshestKnown != null && (System.currentTimeMillis() - freshestKnown.time) < 3_000L) {
            return@withContext freshestKnown
        }

        // 2. 利用可能なプロバイダーを収集
        val candidateProviders = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    candidateProviders.add(LocationManager.FUSED_PROVIDER)
                }
            } catch (_: Exception) {}
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            candidateProviders.add(LocationManager.GPS_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            candidateProviders.add(LocationManager.NETWORK_PROVIDER)
        }

        // 3. 最新の高精度位置情報を全プロバイダー並行・最速完了方式で取得（最大2000ms）
        if (candidateProviders.isNotEmpty()) {
            val freshLocation = withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cancellationSignal = androidx.core.os.CancellationSignal()
                    cont.invokeOnCancellation { cancellationSignal.cancel() }
                    val returned = java.util.concurrent.atomic.AtomicBoolean(false)

                    for (provider in candidateProviders) {
                        LocationManagerCompat.getCurrentLocation(
                            locationManager,
                            provider,
                            cancellationSignal,
                            ContextCompat.getMainExecutor(context)
                        ) { location ->
                            if (location != null && returned.compareAndSet(false, true)) {
                                if (cont.isActive) {
                                    cont.resume(location)
                                }
                            }
                        }
                    }
                }
            }
            if (freshLocation != null) {
                return@withContext freshLocation
            }
        }

        // 4. 新規測位がタイムアウトした場合は直近の位置情報、または過去の最良位置をフォールバック
        bestLastKnown ?: allKnown.maxByOrNull { it.time }
    }

    /**
     * 移動（30m以上）や時間経過（5秒以上）に伴う継続的な位置情報更新リスナーを登録
     * 返り値のラムダを実行することで購読解除が可能
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationChanged: (Location) -> Unit): () -> Unit {
        if (!hasLocationPermission() || locationManager == null) return {}

        val listener = android.location.LocationListener { location ->
            onLocationChanged(location)
        }

        val providers = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    providers.add(LocationManager.FUSED_PROVIDER)
                }
            } catch (_: Exception) {}
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            providers.add(LocationManager.PASSIVE_PROVIDER)
        }

        for (provider in providers) {
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    5_000L,
                    30f,
                    listener,
                    context.mainLooper
                )
            } catch (_: Exception) {}
        }

        return {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    /**
     * 取得した位置情報 (緯度・経度) から詳細な地名（区・市・町）を逆ジオコーディングして City オブジェクトを構築
     */
    suspend fun resolveCity(location: Location): City = withContext(Dispatchers.IO) {
        val lat = location.latitude
        val lon = location.longitude

        var displayName = ""
        var englishName = ""
        var country = "日本"

        try {
            // 日本語地名の取得（タイムアウト付きでハング防止）
            val jaAddresses = withTimeoutOrNull(2000L) {
                val jaGeocoder = Geocoder(context, Locale.JAPAN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                        jaGeocoder.getFromLocation(lat, lon, 1) { list ->
                            if (cont.isActive) cont.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    jaGeocoder.getFromLocation(lat, lon, 1)
                }
            }

            jaAddresses?.firstOrNull()?.let { addr ->
                displayName = resolveDetailedName(addr)
                country = addr.countryName ?: (addr.adminArea ?: "日本")
            }

            // 英語地名の取得（タイムアウト付きでハング防止）
            val enAddresses = withTimeoutOrNull(1500L) {
                val enGeocoder = Geocoder(context, Locale.US)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                        enGeocoder.getFromLocation(lat, lon, 1) { list ->
                            if (cont.isActive) cont.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    enGeocoder.getFromLocation(lat, lon, 1)
                }
            }

            enAddresses?.firstOrNull()?.let { addr ->
                englishName = resolveDetailedName(addr)
            }
        } catch (_: Exception) {
            // Geocoder 失敗時はフォールバック
        }

        // 日本国内で Geocoder が失敗または空の場合、JmaAreaResolver による高精度なオフライン地名フォールバック
        if (displayName.isBlank() || displayName == "現在地") {
            val jmaInfo = JmaAreaResolver.resolve(lat, lon)
            displayName = jmaInfo.stationName.ifBlank { jmaInfo.officeName }
            if (englishName.isBlank() || englishName == "Current Location") {
                englishName = displayName
            }
        }

        if (englishName.isBlank() || englishName == "Current Location") {
            englishName = if (displayName.isNotBlank()) displayName else "Current Location"
        }

        if (displayName.isBlank()) {
            displayName = "現在地"
        }

        City(
            id = "current",
            name = displayName,
            nameEn = englishName,
            country = country,
            latitude = lat,
            longitude = lon,
            isFavorite = false
        )
    }

    /**
     * 都道府県・市区町村・町名からユーザーの現在地を最も具体的かつ直感的に識別できる地名を組み立てる
     */
    private fun resolveDetailedName(addr: android.location.Address): String {
        val admin = addr.adminArea?.trim().orEmpty()       // 例: 東京都, 神奈川県, California
        val locality = addr.locality?.trim().orEmpty()     // 例: 渋谷区, 横浜市, Mountain View
        val subLocality = addr.subLocality?.trim().orEmpty() // 例: 宇田川町, 西区, North Whisman
        val thoroughfare = addr.thoroughfare?.trim().orEmpty() // 例: 表参道, 道玄坂

        return when {
            // 日本: 政令指定都市 + 行政区 (例: 横浜市 西区, 名古屋市 中区)
            locality.endsWith("市") && subLocality.endsWith("区") -> {
                "$locality $subLocality"
            }
            // 日本: 区 + 町名 (例: 渋谷区 道玄坂, 港区 六本木)
            locality.endsWith("区") && subLocality.isNotBlank() && !subLocality.endsWith("市") -> {
                "$locality $subLocality"
            }
            // 日本: 市町村 + 町名 (例: 武蔵野市 吉祥寺)
            locality.isNotBlank() && subLocality.isNotBlank() && !subLocality.endsWith("市") -> {
                "$locality $subLocality"
            }
            // 区・市名のみ (例: 渋谷区, 横浜市, 札幌市)
            locality.isNotBlank() -> locality
            // subLocality のみ (例: 港区, 西区)
            subLocality.isNotBlank() -> subLocality
            // 都道府県 + 通り (例: 東京都 表参道)
            admin.isNotBlank() && thoroughfare.isNotBlank() -> "$admin $thoroughfare"
            // 都道府県のみ (例: 東京都, 大阪府, California)
            admin.isNotBlank() -> admin
            else -> "現在地"
        }
    }
}

