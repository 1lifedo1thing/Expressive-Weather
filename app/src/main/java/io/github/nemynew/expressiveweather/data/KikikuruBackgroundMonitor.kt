package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 警戒レベル・キキクル情報のバックグラウンド定期取得および位置変更トリガー監視マネージャー
 * 通信トリガー:
 * 1. アプリ起動時およびフォアグラウンド復帰時
 * 2. バックグラウンド常駐時は10分間隔で定期取得
 * 3. 現在地が別の市区町村へ移動したことを検知した直後
 */
class KikikuruBackgroundMonitor private constructor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var periodicJob: Job? = null
    @Volatile
    private var lastMunicipalityCode: String? = null

    private val _currentAlert = MutableStateFlow<MunicipalityAlertInfo?>(null)
    val currentAlert: StateFlow<MunicipalityAlertInfo?> = _currentAlert.asStateFlow()

    private val _currentGlobalAlerts = MutableStateFlow<List<GlobalAlertItem>>(emptyList())
    val currentGlobalAlerts: StateFlow<List<GlobalAlertItem>> = _currentGlobalAlerts.asStateFlow()

    private val _savedCitiesMaxAlert = MutableStateFlow<MunicipalityAlertInfo?>(null)
    val savedCitiesMaxAlert: StateFlow<MunicipalityAlertInfo?> = _savedCitiesMaxAlert.asStateFlow()

    companion object {
        private const val TAG = "KikikuruBgMonitor"
        private const val NORMAL_INTERVAL_MILLIS = 10 * 60 * 1000L // 平常時・注意報のみ: 10分間隔
        private const val ALERT_INTERVAL_MILLIS = 3 * 60 * 1000L  // 警報発表中・キキクル警戒時: 3分間隔 (高頻度自動更新)

        @Volatile
        private var INSTANCE: KikikuruBackgroundMonitor? = null

        fun getInstance(context: Context): KikikuruBackgroundMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KikikuruBackgroundMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        JmaMunicipalityResolver.init(context)
        startPeriodicMonitoring()
    }

    /**
     * 定期監視ジョブを開始（平常時10分、警報・特別警報発表中またはキキクル警戒時3分の高頻度フェッチ）
     * 天気・警報ともに「現在地のみ」を対象に監視
     */
    fun startPeriodicMonitoring() {
        if (periodicJob?.isActive == true) return

        periodicJob = scope.launch {
            Log.d(TAG, "Started dynamic periodic alert & warning monitoring loop (Current location only).")
            while (isActive) {
                if (!KikikuruLiveAlertService.isActivePolling) {
                    try {
                        checkCurrentAlert(forceFresh = true)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Periodic alert check error: ${e.message}")
                    }
                }

                // 警報・特別警報が発表中、キキクル警戒中（レベル2以上）、または重大な全球警報がある場合は3分間隔に頻度を引き上げ
                val jmaAlert = _currentAlert.value
                val hasJmaActiveAlert = jmaAlert != null && (
                    jmaAlert.alertLevel.level >= 2 ||
                    jmaAlert.activeWarnings.any { it.isSpecial || it.isWarning }
                )
                val hasGlobalActiveAlert = _currentGlobalAlerts.value.any {
                    it.severity == AlertSeverity.EXTREME || it.severity == AlertSeverity.SEVERE
                }
                val hasActiveAlert = hasJmaActiveAlert || hasGlobalActiveAlert

                val delayTime = if (hasActiveAlert) {
                    Log.d(TAG, "Active alert/warning detected for current location! High-frequency polling active (3-min interval).")
                    ALERT_INTERVAL_MILLIS
                } else {
                    NORMAL_INTERVAL_MILLIS
                }
                delay(delayTime)
            }
        }
    }

    /**
     * アプリ起動時またはフォアグラウンド復帰時の通信トリガー
     */
    fun onAppForegrounded() {
        scope.launch {
            Log.d(TAG, "App foregrounded: Triggering immediate alert & warning evaluation for current location.")
            checkCurrentAlert(forceFresh = true)
        }
    }

    /**
     * 現在地移動検知時の通信トリガー
     * 現在地が別の市区町村へ移動したことを検知した直後に即座に取得
     */
    fun onLocationUpdated(location: Location, cityName: String = "", adminArea: String = "") {
        scope.launch {
            if (!isLocationOrCountryInJapan(adminArea, location.latitude, location.longitude)) {
                val city = City(
                    id = "current",
                    name = cityName.ifBlank { "Current Location" },
                    nameEn = cityName.ifBlank { "Current Location" },
                    country = adminArea,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isFavorite = false
                )
                evaluateGlobalAlertsForCurrentCity(city)
                return@launch
            }
            val muni = JmaMunicipalityResolver.resolveFromLocation(
                location.latitude,
                location.longitude,
                cityName,
                adminArea
            )
            val newCode = muni.code
            if (lastMunicipalityCode != null && lastMunicipalityCode != newCode) {
                Log.d(TAG, "Municipality changed from $lastMunicipalityCode to $newCode (${muni.name}). Immediate alert check!")
                lastMunicipalityCode = newCode
                evaluateAlertForMunicipality(muni, latitude = location.latitude, longitude = location.longitude)
            } else if (lastMunicipalityCode == null) {
                lastMunicipalityCode = newCode
                evaluateAlertForMunicipality(muni, latitude = location.latitude, longitude = location.longitude)
            }
        }
    }

    /**
     * 現在地の警報・注意報・キキクル情報を最新化（現在地のみ）
     */
    suspend fun checkCurrentAlert(forceFresh: Boolean = false) {
        val locationProvider = CurrentLocationProvider(context)
        val hasLocPerm = locationProvider.hasLocationPermission()
        val location = if (hasLocPerm) locationProvider.getCurrentLocation(forceFresh = false) else null
        val db = io.github.nemynew.expressiveweather.data.local.WeatherDatabase.getInstance(context)
        val repo = WeatherRepository(
            weatherDao = db.weatherDao(),
            savedCityDao = db.savedCityDao(),
            context = context
        )
        val liveGpsCity = location?.let { loc ->
            locationProvider.resolveCity(loc).also { repo.setLastResolvedLocationCity(it) }
        }
        val notificationTargetCity = repo.resolveNotificationTargetCityOrNull(
            hasLocationPermission = hasLocPerm,
            liveGpsCity = liveGpsCity
        )
        val fallbackCity = if (notificationTargetCity == null) repo.resolveFallbackCurrentCityOrNull() else null

        val city = when {
            notificationTargetCity != null -> notificationTargetCity
            fallbackCity != null -> fallbackCity
            else -> City(
                id = "current_default",
                name = "千代田区",
                nameEn = "Chiyoda",
                country = "日本",
                latitude = 35.6895,
                longitude = 139.6917,
                isFavorite = false
            )
        }
        val hasEligibleNotificationCity = notificationTargetCity != null

        val inJapan = isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)

        if (inJapan) {
            val muni = if (notificationTargetCity != null || fallbackCity != null) {
                JmaMunicipalityResolver.resolveFromLocation(
                    city.latitude,
                    city.longitude,
                    city.name,
                    ""
                )
            } else {
                JmaMunicipalityResolver.resolve("千代田区", "東京都", "130000")
                    ?: JmaMunicipality("1310100", "131016", "千代田区", "ちよだく", "130000", "東京都", "東京都")
            }

            lastMunicipalityCode = muni.code
            evaluateAlertForMunicipality(
                muni = muni,
                forceFresh = forceFresh,
                notifyUser = hasEligibleNotificationCity,
                latitude = if (hasEligibleNotificationCity) city.latitude else null,
                longitude = if (hasEligibleNotificationCity) city.longitude else null
            )
        } else {
            // 海外現在地: 米国NWS, 世界WMO, GDACSの警報を並行取得
            evaluateGlobalAlertsForCurrentCity(city)
        }
    }

    /**
     * 海外都市（米国/世界各国）の現在地警報を取得・評価
     */
    private suspend fun evaluateGlobalAlertsForCurrentCity(city: City) {
        try {
            val repo = WeatherRepository(context = context)
            val isUs = NwsAlertRepository.isUnitedStates(city)
            val isMeteoAlarm = MeteoAlarmRepository.isSupported(city)

            val alerts = coroutineScope {
                val nwsDef: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (isUs) async {
                    try {
                        NwsAlertRepository.fetchActiveAlerts(city)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "NWS alert fetch failed: ${e.message}")
                        emptyList()
                    }
                } else null

                val meteoDef: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (isMeteoAlarm && repo.getMeteoAlarmAlertsEnabled()) async {
                    try {
                        MeteoAlarmRepository.fetchAlertsForCity(city)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "MeteoAlarm alert fetch failed: ${e.message}")
                        emptyList()
                    }
                } else null

                val wmoDef: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (repo.getWmoAlertsEnabled() && ((!isUs && !isMeteoAlarm) || repo.isWmoAlwaysEnabled())) async {
                    try {
                        WmoAlertRepository.fetchAlertsForCity(city, alwaysEnabled = repo.isWmoAlwaysEnabled())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "WMO alert fetch failed: ${e.message}")
                        emptyList()
                    }
                } else null

                val gdacsDef: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (repo.getGdacsAlertsEnabled()) async {
                    try {
                        GdacsAlertRepository.fetchAlertsForCity(city)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "GDACS alert fetch failed: ${e.message}")
                        emptyList()
                    }
                } else null

                buildList {
                    addAll(nwsDef?.await().orEmpty())
                    addAll(meteoDef?.await().orEmpty())
                    addAll(wmoDef?.await().orEmpty())
                    addAll(gdacsDef?.await().orEmpty())
                }
            }

            val distinctAlerts = alerts.distinctBy { it.id }
            _currentGlobalAlerts.value = distinctAlerts
            if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                Log.d(TAG, "Updated global alerts for current location (${city.name}): ${distinctAlerts.size} items")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                Log.w(TAG, "Failed evaluating global alerts for ${city.name}: ${e.message}")
            } else {
                Log.w(TAG, "Failed evaluating global alerts: ${e.message}")
            }
        }
    }

    /**
     * 保存地点の警報取得（手動呼び出し互換用。定期バックグラウンドループでは呼び出さない）
     */
    fun updateSavedCitiesAlert(savedCities: List<City>) {
        scope.launch {
            try {
                val maxAlert = JmaAlertRepository.evaluateAlertsForSavedCities(context, savedCities)
                _savedCitiesMaxAlert.value = maxAlert
                Log.d(TAG, "Updated saved cities max alert: ${maxAlert?.municipalityName} Lv.${maxAlert?.alertLevel?.level}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to evaluate saved cities alerts: ${e.message}")
            }
        }
    }

    private suspend fun evaluateAlertForMunicipality(
        muni: JmaMunicipality,
        forceFresh: Boolean = false,
        notifyUser: Boolean = true,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        try {
            val alertInfo = JmaAlertRepository.fetchAlertForMunicipality(muni, forceFresh, latitude, longitude)
            _currentAlert.value = alertInfo
            // 通知システムへ通知更新を指示（位置未確定の初期デフォルト時は東京/千代田通知を誤送信しない）
            if (notifyUser) {
                KikikuruNotificationManager.updateLiveAlertNotification(context, alertInfo)
            }
            if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                Log.d(TAG, "Alert check finished for ${muni.name}: Lv.${alertInfo.alertLevel.level} (${alertInfo.alertLevel.title}), Active Warnings: ${alertInfo.activeWarnings.count { it.isWarning || it.isSpecial }}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                Log.e(TAG, "Failed evaluating alert for ${muni.name}: ${e.message}", e)
            } else {
                Log.e(TAG, "Failed evaluating alert: ${e.message}")
            }
        }
    }
}
