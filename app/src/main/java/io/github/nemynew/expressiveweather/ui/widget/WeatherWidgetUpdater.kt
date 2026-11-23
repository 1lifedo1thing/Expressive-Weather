package io.github.nemynew.expressiveweather.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import io.github.nemynew.expressiveweather.MainActivity
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.ui.components.WeatherIconHelper
import io.github.nemynew.expressiveweather.ui.screens.RadarDetailActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 全ホーム画面ウィジェットの状態同期・RemoteViews 生成・OS 反映を一元管理するアップデータ
 */
object WeatherWidgetUpdater {
    private const val TAG = "WeatherWidgetUpdater"
    private val asyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class ActiveWidgetEntry(
        val appWidgetId: Int,
        val defaultTier: WidgetSizeTier,
        val providerComponent: ComponentName,
        val isShapeWidget: Boolean = false,
        val isRadarWidget: Boolean = false
    )

    private data class ProviderSpec(
        val clazz: Class<*>,
        val tier: WidgetSizeTier,
        val isShape: Boolean = false,
        val isRadar: Boolean = false
    )

    fun getActiveWidgets(context: Context): List<ActiveWidgetEntry> {
        val appCtx = context.applicationContext
        val manager = AppWidgetManager.getInstance(appCtx) ?: return emptyList()
        val providers = listOf(
            ProviderSpec(WeatherWidgetCompactProvider::class.java, WidgetSizeTier.COMPACT),
            ProviderSpec(WeatherWidgetMediumProvider::class.java, WidgetSizeTier.MEDIUM),
            ProviderSpec(WeatherWidgetLargeProvider::class.java, WidgetSizeTier.LARGE),
            ProviderSpec(WeatherWidgetShapeProvider::class.java, WidgetSizeTier.COMPACT, isShape = true),
            ProviderSpec(WeatherWidgetRadarProvider::class.java, WidgetSizeTier.MEDIUM, isRadar = true)
        )
        return buildList {
            for (spec in providers) {
                val component = ComponentName(appCtx, spec.clazz)
                val ids = try {
                    manager.getAppWidgetIds(component)
                } catch (_: Exception) {
                    IntArray(0)
                }
                ids.forEach { id ->
                    add(
                        ActiveWidgetEntry(
                            appWidgetId = id,
                            defaultTier = spec.tier,
                            providerComponent = component,
                            isShapeWidget = spec.isShape,
                            isRadarWidget = spec.isRadar
                        )
                    )
                }
            }
        }
    }

    fun updateAllFromCacheAsync(context: Context) {
        val appCtx = context.applicationContext
        asyncScope.launch {
            try {
                updateAllFromCache(appCtx)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "updateAllFromCacheAsync failed: ${e.message}")
            }
        }
    }

    internal fun isTallRadarSize(widthDp: Float, heightDp: Float): Boolean {
        return heightDp >= 210f && (widthDp <= 0f || heightDp >= widthDp * 0.72f)
    }

    internal fun resolveRadarSizeNeeds(
        reportedSizes: List<SizeF>,
        minWidthDp: Float,
        maxHeightDp: Float
    ): Pair<Boolean, Boolean> {
        return if (reportedSizes.isNotEmpty()) {
            val needMedium = reportedSizes.any { !isTallRadarSize(it.width, it.height) }
            val needTall = reportedSizes.any { isTallRadarSize(it.width, it.height) }
            needMedium to needTall
        } else {
            val isTall = isTallRadarSize(minWidthDp, maxHeightDp)
            true to isTall
        }
    }

    private fun getReportedWidgetSizes(options: android.os.Bundle): List<SizeF> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        @Suppress("DEPRECATION")
        val reported: ArrayList<SizeF>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
        } else {
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        }
        return reported ?: emptyList()
    }

    private fun resolveRadarSizeNeedsForWidget(
        manager: AppWidgetManager?,
        appWidgetId: Int
    ): Pair<Boolean, Boolean> {
        if (manager == null) return true to false
        val opts = manager.getAppWidgetOptions(appWidgetId)
        val reported = getReportedWidgetSizes(opts)
        val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
        val maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
        return resolveRadarSizeNeeds(reported, minW, maxH)
    }

    fun updateWidgetAsync(
        context: Context,
        appWidgetId: Int,
        fetchIfMissing: Boolean = false
    ) {
        val appCtx = context.applicationContext
        asyncScope.launch {
            try {
                val entry = getActiveWidgets(appCtx).firstOrNull { it.appWidgetId == appWidgetId }
                val defaultTier = entry?.defaultTier ?: WidgetSizeTier.MEDIUM
                val isShape = entry?.isShapeWidget == true
                val isRadar = entry?.isRadarWidget == true
                updateWidgetsFromCache(
                    appCtx,
                    listOf(
                        entry ?: ActiveWidgetEntry(
                            appWidgetId = appWidgetId,
                            defaultTier = defaultTier,
                            providerComponent = ComponentName(appCtx, WeatherWidgetMediumProvider::class.java),
                            isShapeWidget = isShape,
                            isRadarWidget = isRadar
                        )
                    ),
                    refreshingIds = emptySet(),
                    allowEnqueueRefresh = !fetchIfMissing
                )
                if (fetchIfMissing) {
                    val repo = createRepository(appCtx)
                    val config = WidgetPreferences.getConfig(appCtx, appWidgetId, WidgetStyle.MATERIAL_YOU)
                    val currentCity = repo.resolveFallbackCurrentCity()
                    val targetCity = WidgetUiModelBuilder.resolveTargetCity(config, currentCity, repo.defaultCity)
                    val isSystemDark = (appCtx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    val missingWeather = repo.getCachedWeatherOrNull(targetCity) == null
                    val missingRadar = if (isRadar) {
                        val (needMedium, needTall) = resolveRadarSizeNeedsForWidget(
                            AppWidgetManager.getInstance(appCtx),
                            appWidgetId
                        )
                        (needMedium && !WidgetRadarMapRenderer.hasCachedComposite(appCtx, targetCity, isSystemDark, 300, 240)) ||
                            (needTall && !WidgetRadarMapRenderer.hasCachedComposite(appCtx, targetCity, isSystemDark, 420, 220))
                    } else false
                    if (missingWeather || missingRadar) {
                        WidgetRefreshWorker.enqueueRefresh(appCtx, appWidgetId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "updateWidgetAsync failed for $appWidgetId: ${e.message}")
            }
        }
    }

    suspend fun updateProviderWidgetsFromCache(
        context: Context,
        providerComponent: ComponentName
    ) = withContext(Dispatchers.IO) {
        val entries = getActiveWidgets(context).filter { it.providerComponent == providerComponent }
        if (entries.isEmpty()) return@withContext
        updateWidgetsFromCache(
            context = context,
            targets = entries,
            refreshingIds = emptySet(),
            allowEnqueueRefresh = false
        )
    }

    suspend fun updateSingleWidgetFromCache(
        context: Context,
        appWidgetId: Int,
        fetchIfMissing: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val entry = getActiveWidgets(appCtx).firstOrNull { it.appWidgetId == appWidgetId }
        val defaultTier = entry?.defaultTier ?: WidgetSizeTier.MEDIUM
        val isShape = entry?.isShapeWidget == true
        val isRadar = entry?.isRadarWidget == true
        updateWidgetsFromCache(
            appCtx,
            listOf(
                entry ?: ActiveWidgetEntry(
                    appWidgetId = appWidgetId,
                    defaultTier = defaultTier,
                    providerComponent = ComponentName(appCtx, WeatherWidgetMediumProvider::class.java),
                    isShapeWidget = isShape,
                    isRadarWidget = isRadar
                )
            ),
            refreshingIds = emptySet(),
            allowEnqueueRefresh = !fetchIfMissing
        )
        if (fetchIfMissing) {
            val repo = createRepository(appCtx)
            val config = WidgetPreferences.getConfig(appCtx, appWidgetId, WidgetStyle.MATERIAL_YOU)
            val currentCity = repo.resolveFallbackCurrentCity()
            val targetCity = WidgetUiModelBuilder.resolveTargetCity(config, currentCity, repo.defaultCity)
            val isSystemDark = (appCtx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val missingWeather = repo.getCachedWeatherOrNull(targetCity) == null
            val missingRadar = if (isRadar) {
                val (needMedium, needTall) = resolveRadarSizeNeedsForWidget(
                    AppWidgetManager.getInstance(appCtx),
                    appWidgetId
                )
                (needMedium && !WidgetRadarMapRenderer.hasCachedComposite(appCtx, targetCity, isSystemDark, 300, 240)) ||
                    (needTall && !WidgetRadarMapRenderer.hasCachedComposite(appCtx, targetCity, isSystemDark, 420, 220))
            } else false
            if (missingWeather || missingRadar) {
                WidgetRefreshWorker.enqueueRefresh(appCtx, appWidgetId)
            }
        }
    }

    suspend fun updateAllFromCache(
        context: Context,
        refreshingIds: Set<Int> = emptySet(),
        allowEnqueueRefresh: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val entries = getActiveWidgets(context)
        if (entries.isEmpty()) return@withContext
        updateWidgetsFromCache(
            context = context,
            targets = entries,
            refreshingIds = refreshingIds,
            allowEnqueueRefresh = allowEnqueueRefresh
        )
    }

    suspend fun refreshActiveRadarWidgets(
        context: Context,
        currentCityOverride: City? = null
    ) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val radarWidgets = getActiveWidgets(appCtx).filter { it.isRadarWidget }
        if (radarWidgets.isEmpty()) return@withContext
        val repo = createRepository(appCtx)
        val effectiveCurrent = currentCityOverride ?: repo.resolveFallbackCurrentCity()
        val radarCityNeeds = collectRadarCityNeeds(appCtx, repo, radarWidgets, effectiveCurrent)
        if (radarCityNeeds.isEmpty()) return@withContext
        renderRadarCachesForNeeds(appCtx, repo, radarCityNeeds)
    }

    private data class RadarRenderNeed(
        val city: City,
        var needMedium: Boolean = false,
        var needTall: Boolean = false
    )

    private fun collectRadarCityNeeds(
        appCtx: Context,
        repo: WeatherRepository,
        entries: List<ActiveWidgetEntry>,
        effectiveCurrentCity: City
    ): LinkedHashMap<String, RadarRenderNeed> {
        val radarCityNeeds = LinkedHashMap<String, RadarRenderNeed>()
        val manager = AppWidgetManager.getInstance(appCtx)
        entries.filter { it.isRadarWidget }.forEach { entry ->
            val cfg = WidgetPreferences.getConfig(appCtx, entry.appWidgetId, WidgetStyle.MATERIAL_YOU)
            val city = WidgetUiModelBuilder.resolveTargetCity(cfg, effectiveCurrentCity, repo.defaultCity)
            val key = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            val (needMedium, needTall) = resolveRadarSizeNeedsForWidget(manager, entry.appWidgetId)
            val need = radarCityNeeds.getOrPut(key) { RadarRenderNeed(city = city) }
            if (needMedium) need.needMedium = true
            if (needTall) need.needTall = true
        }
        return radarCityNeeds
    }

    private suspend fun renderRadarCachesForNeeds(
        appCtx: Context,
        repo: WeatherRepository,
        radarCityNeeds: Map<String, RadarRenderNeed>
    ) = coroutineScope {
        val isSystemDark = (appCtx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val myPalette = WidgetCanvasRenderer.resolveMaterialYouPalette(appCtx, isSystemDark)
        val language = repo.getLanguage()
        radarCityNeeds.values.map { need ->
            async {
                try {
                    if (need.needMedium) {
                        WidgetRadarMapRenderer.fetchAndRenderAndCache(
                            context = appCtx,
                            city = need.city,
                            myPalette = myPalette,
                            isSystemDark = isSystemDark,
                            language = language,
                            forceRefresh = true,
                            widthPx = 300,
                            heightPx = 240
                        )
                    }
                    if (need.needTall) {
                        WidgetRadarMapRenderer.fetchAndRenderAndCache(
                            context = appCtx,
                            city = need.city,
                            myPalette = myPalette,
                            isSystemDark = isSystemDark,
                            language = language,
                            forceRefresh = !need.needMedium,
                            widthPx = 420,
                            heightPx = 220
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                        Log.w(TAG, "Widget radar map refresh failed for ${need.city.name}: ${e.message}")
                    } else {
                        Log.w(TAG, "Widget radar map refresh failed: ${e.message}")
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun refreshWidgetsFromNetwork(
        context: Context,
        specificWidgetId: Int? = null
    ) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val active = getActiveWidgets(appCtx)
        val targets = if (specificWidgetId != null) {
            active.filter { it.appWidgetId == specificWidgetId }
        } else {
            active
        }
        if (targets.isEmpty()) return@withContext

        val repo = createRepository(appCtx)
        val locationProvider = CurrentLocationProvider(appCtx)

        var resolvedCurrentCity = repo.resolveFallbackCurrentCityOrNull()
        val needsCurrentLocation = targets.any { entry ->
            val cfg = WidgetPreferences.getConfig(appCtx, entry.appWidgetId, WidgetStyle.MATERIAL_YOU)
            cfg.useCurrentLocation || cfg.pinnedCity == null
        }
        if (needsCurrentLocation && locationProvider.hasLocationPermission()) {
            try {
                val loc = withTimeoutOrNull(3_000L) {
                    locationProvider.getCurrentLocation(forceFresh = true)
                }
                if (loc != null) {
                    resolvedCurrentCity = locationProvider.resolveCity(loc)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
        val effectiveCurrentCity = resolvedCurrentCity ?: repo.resolveFallbackCurrentCity()

        // 各ウィジェットのターゲット都市を重複排除
        val uniqueCities = LinkedHashMap<String, City>()
        targets.forEach { entry ->
            val cfg = WidgetPreferences.getConfig(appCtx, entry.appWidgetId, WidgetStyle.MATERIAL_YOU)
            val city = WidgetUiModelBuilder.resolveTargetCity(cfg, effectiveCurrentCity, repo.defaultCity)
            val key = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            uniqueCities.putIfAbsent(key, city)
        }
        val radarCityNeeds = collectRadarCityNeeds(appCtx, repo, targets, effectiveCurrentCity)

        // 天気データ更新とレーダー合成を並列実行（8.5s Broadcast 枠内に収める）
        coroutineScope {
            val weatherJob = uniqueCities.values.map { city ->
                async {
                    try {
                        repo.refresh(city)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
                            Log.w(TAG, "Widget network refresh failed for ${city.name}: ${e.message}")
                        } else {
                            Log.w(TAG, "Widget network refresh failed: ${e.message}")
                        }
                    }
                }
            }
            val radarJob = async {
                if (radarCityNeeds.isNotEmpty()) {
                    renderRadarCachesForNeeds(appCtx, repo, radarCityNeeds)
                }
            }
            weatherJob.awaitAll()
            radarJob.await()
        }

        // 更新完了後、全アクティブウィジェットを最新キャッシュから再描画
        updateAllFromCache(appCtx, allowEnqueueRefresh = false)
    }

    private suspend fun updateWidgetsFromCache(
        context: Context,
        targets: List<ActiveWidgetEntry>,
        refreshingIds: Set<Int>,
        allowEnqueueRefresh: Boolean = true
    ) {
        if (targets.isEmpty()) return
        val appCtx = context.applicationContext
        val manager = AppWidgetManager.getInstance(appCtx) ?: return
        val repo = createRepository(appCtx)
        val language = repo.getLanguage()
        val tempUnit = repo.getTemperatureUnit()
        val windUnit = repo.getWindSpeedUnit()
        val fallbackCurrent = repo.resolveFallbackCurrentCity()
        val isSystemDark = (appCtx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val cacheByLocationKey = mutableMapOf<String, WeatherData?>()

        for (entry in targets) {
            val appWidgetId = entry.appWidgetId
            val config = WidgetPreferences.getConfig(appCtx, appWidgetId, WidgetStyle.MATERIAL_YOU)
            val targetCity = WidgetUiModelBuilder.resolveTargetCity(config, fallbackCurrent, repo.defaultCity)
            val locKey = LocationKeyNormalizer.normalize(targetCity.latitude, targetCity.longitude)
            val weather = cacheByLocationKey.getOrPut(locKey) {
                repo.getCachedWeatherOrNull(targetCity)
            }
            val uiModel = WidgetUiModelBuilder.build(
                config = config,
                weather = weather,
                language = language,
                temperatureUnit = tempUnit,
                windSpeedUnit = windUnit,
                fallbackCity = fallbackCurrent,
                isRefreshing = appWidgetId in refreshingIds
            )
            val providerComponent = entry.providerComponent
            var missingRadarCacheForWidget = false

            val remoteViews = if (entry.isShapeWidget) {
                buildShapeRemoteViews(appCtx, appWidgetId, config, uiModel)
            } else if (entry.isRadarWidget) {
                val effectiveRadarCity = uiModel.targetCity ?: targetCity
                fun checkMissingCacheForTier(tier: WidgetSizeTier) {
                    val w = if (tier == WidgetSizeTier.LARGE) 420 else 300
                    val h = if (tier == WidgetSizeTier.LARGE) 220 else 240
                    if (!WidgetRadarMapRenderer.hasCachedComposite(appCtx, effectiveRadarCity, isSystemDark, w, h)) {
                        missingRadarCacheForWidget = true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val options = manager.getAppWidgetOptions(appWidgetId)
                    val reportedSizes = getReportedWidgetSizes(options)
                    val builtByTier = HashMap<WidgetSizeTier, RemoteViews>()
                    fun getOrBuildRadarTier(tier: WidgetSizeTier): RemoteViews {
                        checkMissingCacheForTier(tier)
                        return builtByTier.getOrPut(tier) {
                            buildRadarRemoteViewsForTier(
                                appCtx, appWidgetId, tier, config, uiModel, targetCity, isSystemDark, language
                            )
                        }
                    }
                    val sizeMap: Map<SizeF, RemoteViews> = if (reportedSizes.isNotEmpty()) {
                        reportedSizes.associateWith { sz ->
                            val isTall = isTallRadarSize(sz.width, sz.height)
                            getOrBuildRadarTier(if (isTall) WidgetSizeTier.LARGE else WidgetSizeTier.MEDIUM)
                        }
                    } else {
                        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
                        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
                        val pairs = buildList {
                            add(SizeF(230f, 120f) to WidgetSizeTier.MEDIUM)
                            add(SizeF(230f, 230f) to WidgetSizeTier.LARGE)
                            if (minW > 0f && maxH > 0f) {
                                val isTall = isTallRadarSize(minW, maxH)
                                add(SizeF(minW, maxH) to if (isTall) WidgetSizeTier.LARGE else WidgetSizeTier.MEDIUM)
                            }
                        }
                        pairs.associate { (sz, t) -> sz to getOrBuildRadarTier(t) }
                    }
                    RemoteViews(sizeMap)
                } else {
                    val options = manager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
                    val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
                    val resolvedTier = if (isTallRadarSize(minWidth, maxHeight)) WidgetSizeTier.LARGE else WidgetSizeTier.MEDIUM
                    checkMissingCacheForTier(resolvedTier)
                    buildRadarRemoteViewsForTier(
                        appCtx, appWidgetId, resolvedTier, config, uiModel, targetCity, isSystemDark, language
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val options = manager.getAppWidgetOptions(appWidgetId)
                val reportedSizes = getReportedWidgetSizes(options)

                val builtByTier = HashMap<WidgetSizeTier, RemoteViews>()
                fun getOrBuildTier(tier: WidgetSizeTier): RemoteViews {
                    return builtByTier.getOrPut(tier) {
                        buildRemoteViewsForTier(
                            appCtx, appWidgetId, tier, config, uiModel, isSystemDark, providerComponent
                        )
                    }
                }

                val sizeMap: Map<SizeF, RemoteViews> = if (reportedSizes.isNotEmpty()) {
                    reportedSizes.associateWith { sz ->
                        val resolvedTier = WidgetSizeTier.fromSizeDp(sz.width, sz.height, entry.defaultTier)
                        getOrBuildTier(resolvedTier)
                    }
                } else {
                    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
                    val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
                    // Binder IPC 1MB 上限超過（TransactionTooLargeException）を防ぐため、同時生成する tier 数を最小限に制限
                    val fallbackTiers: List<Pair<SizeF, WidgetSizeTier>> = buildList {
                        when (entry.defaultTier) {
                            WidgetSizeTier.COMPACT, WidgetSizeTier.COMPACT_WIDE -> {
                                add(SizeF(110f, 52f) to WidgetSizeTier.COMPACT_WIDE)
                                add(SizeF(110f, 110f) to WidgetSizeTier.COMPACT)
                            }
                            WidgetSizeTier.WIDE_HOURLY -> {
                                add(SizeF(250f, 56f) to WidgetSizeTier.WIDE_HOURLY)
                                add(SizeF(230f, 140f) to WidgetSizeTier.MEDIUM)
                            }
                            WidgetSizeTier.MEDIUM -> {
                                add(SizeF(250f, 56f) to WidgetSizeTier.WIDE_HOURLY)
                                add(SizeF(230f, 140f) to WidgetSizeTier.MEDIUM)
                            }
                            WidgetSizeTier.LARGE -> {
                                add(SizeF(230f, 230f) to WidgetSizeTier.LARGE)
                            }
                        }
                        if (minW > 0f && maxH > 0f) {
                            val currentTier = WidgetSizeTier.fromSizeDp(minW, maxH, entry.defaultTier)
                            if (none { it.second == currentTier }) {
                                add(SizeF(minW, maxH) to currentTier)
                            }
                        }
                    }
                    fallbackTiers.associate { (sz, t) -> sz to getOrBuildTier(t) }
                }
                RemoteViews(sizeMap)
            } else {
                val options = manager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
                val resolvedTier = WidgetSizeTier.fromSizeDp(minWidth, minHeight, entry.defaultTier)
                buildRemoteViewsForTier(
                    appCtx, appWidgetId, resolvedTier, config, uiModel, isSystemDark, providerComponent
                )
            }

            try {
                manager.updateAppWidget(appWidgetId, remoteViews)
            } catch (e: Exception) {
                val isBinderTooLarge = e is android.os.TransactionTooLargeException ||
                    e.cause is android.os.TransactionTooLargeException ||
                    e.message?.contains("TransactionTooLarge", ignoreCase = true) == true
                if (isBinderTooLarge && !entry.isShapeWidget) {
                    Log.w(TAG, "TransactionTooLargeException on appWidgetId=$appWidgetId; falling back to single-tier RemoteViews")
                    try {
                        val fallbackViews = if (entry.isRadarWidget) {
                            buildRadarRemoteViewsForTier(
                                appCtx, appWidgetId, WidgetSizeTier.MEDIUM, config, uiModel, targetCity, isSystemDark, language
                            )
                        } else {
                            val opts = manager.getAppWidgetOptions(appWidgetId)
                            val w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
                            val h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
                            val singleTier = WidgetSizeTier.fromSizeDp(w, h, entry.defaultTier)
                            buildRemoteViewsForTier(
                                appCtx, appWidgetId, singleTier, config, uiModel, isSystemDark, providerComponent
                            )
                        }
                        manager.updateAppWidget(appWidgetId, fallbackViews)
                    } catch (fallbackEx: Exception) {
                        Log.e(TAG, "Single-tier fallback update also failed for appWidgetId=$appWidgetId: ${fallbackEx.message}", fallbackEx)
                    }
                } else {
                    Log.e(TAG, "Failed updating appWidgetId=$appWidgetId: ${e.message}", e)
                }
            }

            if (allowEnqueueRefresh && missingRadarCacheForWidget) {
                WidgetRefreshWorker.enqueueRefresh(appCtx, appWidgetId)
            }
        }
    }

    internal fun buildShapeRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: WidgetConfig,
        model: WidgetUiModel
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_weather_shape)
        val bitmap = WidgetMaterialShapeRenderer.renderShapeWidgetBitmap(
            context = context,
            config = config,
            model = model,
            sizePx = 384
        )
        views.setImageViewBitmap(R.id.widget_shape_bitmap, bitmap)
        views.setContentDescription(R.id.widget_root, buildWidgetContentDescription(model))
        views.setOnClickPendingIntent(R.id.widget_root, createOpenAppPendingIntent(context, appWidgetId, model))
        return views
    }

    private fun buildWidgetContentDescription(model: WidgetUiModel): String {
        return buildString {
            append(model.cityName)
            if (model.currentTempText.isNotBlank()) {
                append(", ")
                append(model.currentTempText)
            }
            if (model.conditionLabel.isNotBlank()) {
                append(", ")
                append(model.conditionLabel)
            }
            if (model.highLowText.isNotBlank()) {
                append(", ")
                append(model.highLowText)
            }
        }
    }

    internal fun buildRemoteViewsForTier(
        context: Context,
        appWidgetId: Int,
        tier: WidgetSizeTier,
        config: WidgetConfig,
        model: WidgetUiModel,
        isSystemDark: Boolean,
        providerComponent: ComponentName?
    ): RemoteViews {
        val layoutRes = when (tier) {
            WidgetSizeTier.COMPACT_WIDE -> R.layout.widget_weather_compact_wide
            WidgetSizeTier.WIDE_HOURLY -> R.layout.widget_weather_wide_hourly
            WidgetSizeTier.COMPACT -> R.layout.widget_weather_compact
            WidgetSizeTier.MEDIUM -> R.layout.widget_weather_medium
            WidgetSizeTier.LARGE -> R.layout.widget_weather_large
        }
        val views = RemoteViews(context.packageName, layoutRes)

        with(resolveSurfaceColors(context, config, isSystemDark)) {
            // 背景スタイル適用（Material You / Material You Frosted Glass、枠線なし）
            val bgBitmap = WidgetCanvasRenderer.renderBackgroundBitmap(
                context = context,
                sizeTier = tier,
                style = config.style,
                condition = model.condition,
                isDay = model.isDay,
                isSystemDark = isSystemDark,
                myPalette = myPalette,
                glassOpacity = glassOpacity
            )
            views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

        // 基本情報バインド（全四角ウィジェットで Google Sans Flex 描画 + WeatherIconHelper 統一）
        val cityMaxW = when (tier) {
            WidgetSizeTier.COMPACT_WIDE -> 128f
            WidgetSizeTier.WIDE_HOURLY -> 98f
            WidgetSizeTier.COMPACT -> 104f
            WidgetSizeTier.MEDIUM -> 116f
            WidgetSizeTier.LARGE -> 168f
        }
        val citySizeSp = when (tier) {
            WidgetSizeTier.LARGE -> 17f
            WidgetSizeTier.MEDIUM -> 16f
            WidgetSizeTier.WIDE_HOURLY -> 14f
            else -> 15f
        }
        views.setImageViewBitmap(
            R.id.widget_city_name,
            WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                context = context,
                text = model.cityName,
                textSizeSp = citySizeSp,
                colorArgb = primaryTextColor,
                fontWeight = 700,
                maxWidthDp = cityMaxW,
                densityDpi = DisplayMetrics.DENSITY_XHIGH,
                softShadow = softShadow
            )
        )
        val heroTempSizeSp = when (tier) {
            WidgetSizeTier.COMPACT_WIDE -> 34f
            WidgetSizeTier.WIDE_HOURLY -> 30f
            WidgetSizeTier.COMPACT, WidgetSizeTier.MEDIUM -> 40f
            WidgetSizeTier.LARGE -> 44f
        }
        val heroTempWeight = if (tier == WidgetSizeTier.COMPACT_WIDE || tier == WidgetSizeTier.WIDE_HOURLY) 420 else 380
        views.setImageViewBitmap(
            R.id.widget_current_temp,
            WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                context = context,
                text = model.currentTempText,
                textSizeSp = heroTempSizeSp,
                colorArgb = primaryTextColor,
                fontWeight = heroTempWeight,
                letterSpacing = -0.04f,
                densityDpi = DisplayMetrics.DENSITY_420,
                softShadow = softShadow
            )
        )
        views.setViewVisibility(
            R.id.widget_location_icon,
            if (model.isCurrentLocation) View.VISIBLE else View.GONE
        )
        views.setInt(R.id.widget_location_icon, "setColorFilter", primaryTextColor)

        val heroIconRes = WeatherIconHelper.getConditionDrawable(model.condition, model.isDay)
        val heroIconTint = WidgetCanvasRenderer.getConditionIconTint(model.condition, model.isDay, isLightSurface)
        views.setImageViewResource(R.id.widget_weather_icon, heroIconRes)
        views.setInt(R.id.widget_weather_icon, "setColorFilter", heroIconTint)

        // タップ操作 PendingIntent（本体 → MainActivity 都市起動）
        views.setOnClickPendingIntent(R.id.widget_root, createOpenAppPendingIntent(context, appWidgetId, model))

        when (tier) {
            WidgetSizeTier.COMPACT_WIDE -> {
                views.setImageViewBitmap(
                    R.id.widget_subtitle,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.compactSubtitle,
                        textSizeSp = 12f,
                        colorArgb = secondaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 136f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
            }

            WidgetSizeTier.WIDE_HOURLY -> {
                views.setImageViewBitmap(
                    R.id.widget_subtitle,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.compactSubtitle,
                        textSizeSp = 11.5f,
                        colorArgb = secondaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 104f,
                        densityDpi = DisplayMetrics.DENSITY_HIGH,
                        softShadow = softShadow
                    )
                )
                bindHourlyTray(
                    context = context,
                    views = views,
                    items = model.hourlyItems,
                    maxCount = 4,
                    includePop = false,
                    trayFillColor = trayFillColor,
                    activeBoxFillColor = activeHourlyFillColor,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    activePrimaryTextColor = activeHourlyPrimaryText,
                    activeMutedTextColor = activeHourlyMutedText,
                    popTextColor = popTextColor,
                    isLightSurface = isLightSurface,
                    activeIsLightSurface = activeHourlyIsLight,
                    softShadow = softShadow
                )
            }

            WidgetSizeTier.COMPACT -> {
                views.setImageViewBitmap(
                    R.id.widget_condition_label,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.conditionLabel,
                        textSizeSp = 13f,
                        colorArgb = primaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 96f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
                val compactHiLo = if (model.hasData) model.highLowText.substringBefore("・").trim() else model.highLowText
                views.setImageViewBitmap(
                    R.id.widget_high_low,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = compactHiLo,
                        textSizeSp = 13f,
                        colorArgb = secondaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 64f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
            }

            WidgetSizeTier.MEDIUM -> {
                val mediumSub = if (model.hasData) "${model.conditionLabel}  ${model.highLowText}" else model.conditionLabel
                views.setImageViewBitmap(
                    R.id.widget_condition_label,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = mediumSub,
                        textSizeSp = 13f,
                        colorArgb = secondaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 175f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
                views.setImageViewBitmap(
                    R.id.widget_updated_time,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.updatedTimeText,
                        textSizeSp = 11f,
                        colorArgb = mutedTextColor,
                        fontWeight = 400,
                        maxWidthDp = 46f,
                        densityDpi = DisplayMetrics.DENSITY_HIGH,
                        softShadow = softShadow
                    )
                )
                bindHourlyTray(
                    context = context,
                    views = views,
                    items = model.hourlyItems,
                    maxCount = 5,
                    includePop = true,
                    trayFillColor = trayFillColor,
                    activeBoxFillColor = activeHourlyFillColor,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    activePrimaryTextColor = activeHourlyPrimaryText,
                    activeMutedTextColor = activeHourlyMutedText,
                    popTextColor = popTextColor,
                    isLightSurface = isLightSurface,
                    activeIsLightSurface = activeHourlyIsLight,
                    softShadow = softShadow
                )
            }

            WidgetSizeTier.LARGE -> {
                views.setImageViewBitmap(
                    R.id.widget_condition_label,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.conditionLabel,
                        textSizeSp = 14f,
                        colorArgb = primaryTextColor,
                        fontWeight = 600,
                        maxWidthDp = 140f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
                views.setImageViewBitmap(
                    R.id.widget_high_low,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.highLowText,
                        textSizeSp = 13f,
                        colorArgb = secondaryTextColor,
                        fontWeight = 500,
                        maxWidthDp = 148f,
                        densityDpi = DisplayMetrics.DENSITY_XHIGH,
                        softShadow = softShadow
                    )
                )
                views.setImageViewBitmap(
                    R.id.widget_updated_time,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = model.updatedTimeText,
                        textSizeSp = 11f,
                        colorArgb = mutedTextColor,
                        fontWeight = 400,
                        maxWidthDp = 50f,
                        densityDpi = DisplayMetrics.DENSITY_HIGH,
                        softShadow = softShadow
                    )
                )

                views.setInt(R.id.widget_btn_settings, "setColorFilter", iconControlTint)
                views.setOnClickPendingIntent(R.id.widget_btn_settings, createSettingsPendingIntent(context, appWidgetId))

                bindHourlyTray(
                    context = context,
                    views = views,
                    items = model.hourlyItems,
                    maxCount = 6,
                    includePop = true,
                    trayFillColor = trayFillColor,
                    activeBoxFillColor = activeHourlyFillColor,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    activePrimaryTextColor = activeHourlyPrimaryText,
                    activeMutedTextColor = activeHourlyMutedText,
                    popTextColor = popTextColor,
                    isLightSurface = isLightSurface,
                    activeIsLightSurface = activeHourlyIsLight,
                    softShadow = softShadow
                )
                bindDailyTray(
                    context = context,
                    views = views,
                    items = model.dailyItems,
                    weekMin = model.weekMinTemp,
                    weekMax = model.weekMaxTemp,
                    style = config.style,
                    myPalette = myPalette,
                    isSystemDark = isSystemDark,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    isLightSurface = isLightSurface,
                    softShadow = softShadow
                )
            }
        }
        }

        views.setContentDescription(R.id.widget_root, buildWidgetContentDescription(model))
        return views
    }

    internal fun buildRadarRemoteViewsForTier(
        context: Context,
        appWidgetId: Int,
        tier: WidgetSizeTier,
        config: WidgetConfig,
        model: WidgetUiModel,
        targetCity: City,
        isSystemDark: Boolean,
        language: AppLanguage
    ): RemoteViews {
        val isTall = tier == WidgetSizeTier.LARGE
        val layoutRes = if (isTall) R.layout.widget_weather_radar_tall else R.layout.widget_weather_radar
        val views = RemoteViews(context.packageName, layoutRes)

        with(resolveSurfaceColors(context, config, isSystemDark)) {
            val bgBitmap = WidgetCanvasRenderer.renderBackgroundBitmap(
                context = context,
                sizeTier = tier,
                style = config.style,
                condition = model.condition,
                isDay = model.isDay,
                isSystemDark = isSystemDark,
                myPalette = myPalette,
                glassOpacity = glassOpacity
            )
            views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

            val effectiveCity = model.targetCity ?: targetCity
            val radarBitmap = WidgetRadarMapRenderer.loadCachedOrFallbackBitmap(
                context = context,
                city = effectiveCity,
                myPalette = myPalette,
                isSystemDark = isSystemDark,
                language = language,
                widthPx = if (isTall) 420 else 300,
                heightPx = if (isTall) 220 else 240
            )
            views.setImageViewBitmap(R.id.widget_radar_map, radarBitmap)

            val badgeText = WidgetRadarMapRenderer.getCachedBadgeText(context, effectiveCity, language)
            views.setImageViewBitmap(
                R.id.widget_radar_badge,
                WidgetRadarMapRenderer.renderRadarBadgePillBitmap(context, badgeText, myPalette)
            )

            // レーダー領域タップで雨雲レーダー詳細画面へ、右/本体タップで天気詳細へ
            views.setContentDescription(R.id.widget_root, buildWidgetContentDescription(model))
            views.setOnClickPendingIntent(
                R.id.widget_root,
                createOpenAppPendingIntent(context, appWidgetId, model)
            )
            views.setOnClickPendingIntent(
                R.id.widget_radar_container,
                createOpenRadarPendingIntent(context, appWidgetId, effectiveCity)
            )

            views.setViewVisibility(
                R.id.widget_location_icon,
                if (model.isCurrentLocation) View.VISIBLE else View.GONE
            )
            views.setInt(R.id.widget_location_icon, "setColorFilter", primaryTextColor)
            views.setImageViewBitmap(
                R.id.widget_city_name,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = model.cityName,
                    textSizeSp = if (isTall) 15f else 13f,
                    colorArgb = primaryTextColor,
                    fontWeight = 700,
                    maxWidthDp = if (isTall) 180f else 108f,
                    densityDpi = DisplayMetrics.DENSITY_XHIGH,
                    softShadow = softShadow
                )
            )

            val heroIconRes = WeatherIconHelper.getConditionDrawable(model.condition, model.isDay)
            val heroIconTint = WidgetCanvasRenderer.getConditionIconTint(model.condition, model.isDay, isLightSurface)
            views.setImageViewResource(R.id.widget_weather_icon, heroIconRes)
            views.setInt(R.id.widget_weather_icon, "setColorFilter", heroIconTint)

            views.setImageViewBitmap(
                R.id.widget_current_temp,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = model.currentTempText,
                    textSizeSp = if (isTall) 36f else 30f,
                    colorArgb = primaryTextColor,
                    fontWeight = 380,
                    letterSpacing = -0.04f,
                    densityDpi = DisplayMetrics.DENSITY_420,
                    softShadow = softShadow
                )
            )

            val subLabel = if (model.hasData) "${model.conditionLabel}  ${model.highLowText}" else model.conditionLabel
            views.setImageViewBitmap(
                R.id.widget_condition_label,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = subLabel,
                    textSizeSp = if (isTall) 12f else 10.5f,
                    colorArgb = secondaryTextColor,
                    fontWeight = 500,
                    maxWidthDp = if (isTall) 190f else 132f,
                    densityDpi = DisplayMetrics.DENSITY_XHIGH,
                    softShadow = softShadow
                )
            )

            bindHourlyTray(
                context = context,
                views = views,
                items = model.hourlyItems,
                maxCount = if (isTall) 5 else 3,
                includePop = false,
                trayFillColor = trayFillColor,
                activeBoxFillColor = activeHourlyFillColor,
                primaryTextColor = primaryTextColor,
                mutedTextColor = mutedTextColor,
                activePrimaryTextColor = activeHourlyPrimaryText,
                activeMutedTextColor = activeHourlyMutedText,
                popTextColor = popTextColor,
                isLightSurface = isLightSurface,
                activeIsLightSurface = activeHourlyIsLight,
                softShadow = softShadow
            )
        }

        return views
    }

    private data class WidgetSurfaceColors(
        val myPalette: WidgetMaterialYouPalette,
        val glassOpacity: Float,
        val isLightSurface: Boolean,
        val softShadow: Boolean,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val mutedTextColor: Int,
        val popTextColor: Int,
        val iconControlTint: Int,
        val trayFillColor: Int,
        val activeHourlyFillColor: Int,
        val activeHourlyPrimaryText: Int,
        val activeHourlyMutedText: Int,
        val activeHourlyIsLight: Boolean
    )

    private fun resolveSurfaceColors(
        context: Context,
        config: WidgetConfig,
        isSystemDark: Boolean
    ): WidgetSurfaceColors {
        val myPalette = WidgetCanvasRenderer.resolveMaterialYouPalette(context, isSystemDark)
        val glassOpacity = config.glassOpacity.coerceIn(0.15f, 0.95f)
        val isMaterialYou = config.style == WidgetStyle.MATERIAL_YOU
        val isFrosted = config.style == WidgetStyle.FROSTED_GLASS
        val isLightSurface = when (config.style) {
            WidgetStyle.MATERIAL_YOU -> myPalette.isLightSurface
            WidgetStyle.FROSTED_GLASS -> myPalette.isLightSurface && glassOpacity >= 0.45f
            WidgetStyle.WEATHER_EXPRESSIVE -> false
        }
        val softShadow = isFrosted && !isLightSurface && glassOpacity < 0.65f
        val primaryTextColor = if (isMaterialYou || (isFrosted && isLightSurface)) {
            myPalette.primaryTextColor
        } else {
            Color.WHITE
        }
        val secondaryTextColor = if (isMaterialYou || (isFrosted && isLightSurface)) {
            myPalette.secondaryTextColor
        } else {
            Color.argb(230, 255, 255, 255)
        }
        val mutedTextColor = if (isMaterialYou || (isFrosted && isLightSurface)) {
            WidgetCanvasRenderer.withAlpha(myPalette.secondaryTextColor, 200)
        } else {
            Color.argb(195, 255, 255, 255)
        }
        val popTextColor = when {
            isMaterialYou || (isFrosted && isLightSurface) -> myPalette.accentColor
            isFrosted -> WidgetCanvasRenderer.blendColors(myPalette.accentColor, Color.WHITE, 0.35f)
            else -> Color.rgb(144, 202, 249)
        }
        val iconControlTint = if (isLightSurface) primaryTextColor else Color.WHITE
        val trayFillColor = when (config.style) {
            WidgetStyle.MATERIAL_YOU -> myPalette.trayColor
            WidgetStyle.FROSTED_GLASS -> {
                val base = if (isSystemDark) myPalette.trayColor else Color.WHITE
                val trayAlpha = if (isLightSurface) {
                    (glassOpacity * 170f).toInt().coerceIn(56, 175)
                } else {
                    (glassOpacity * 115f + 34f).toInt().coerceIn(42, 148)
                }
                WidgetCanvasRenderer.withAlpha(base, trayAlpha)
            }
            WidgetStyle.WEATHER_EXPRESSIVE -> Color.argb(38, 255, 255, 255)
        }
        val activeHourlyFillColor = when (config.style) {
            WidgetStyle.MATERIAL_YOU -> myPalette.primaryContainerColor
            WidgetStyle.FROSTED_GLASS -> {
                val base = if (isSystemDark) myPalette.primaryContainerColor else Color.WHITE
                val alpha = if (isLightSurface) {
                    (glassOpacity * 210f).toInt().coerceIn(85, 220)
                } else {
                    (glassOpacity * 145f + 54f).toInt().coerceIn(64, 190)
                }
                WidgetCanvasRenderer.withAlpha(base, alpha)
            }
            WidgetStyle.WEATHER_EXPRESSIVE -> Color.argb(72, 255, 255, 255)
        }
        val activeHourlyPrimaryText = if (isMaterialYou || (isFrosted && isLightSurface)) {
            myPalette.onPrimaryContainerColor
        } else {
            Color.WHITE
        }
        return WidgetSurfaceColors(
            myPalette = myPalette,
            glassOpacity = glassOpacity,
            isLightSurface = isLightSurface,
            softShadow = softShadow,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            mutedTextColor = mutedTextColor,
            popTextColor = popTextColor,
            iconControlTint = iconControlTint,
            trayFillColor = trayFillColor,
            activeHourlyFillColor = activeHourlyFillColor,
            activeHourlyPrimaryText = activeHourlyPrimaryText,
            activeHourlyMutedText = WidgetCanvasRenderer.withAlpha(activeHourlyPrimaryText, 210),
            activeHourlyIsLight = isLightSurface
        )
    }

    private fun applyShapeSurfaceTint(
        views: RemoteViews,
        imageViewId: Int,
        drawableRes: Int,
        fillColorArgb: Int
    ) {
        val opaqueRgb = Color.rgb(
            Color.red(fillColorArgb),
            Color.green(fillColorArgb),
            Color.blue(fillColorArgb)
        )
        views.setImageViewResource(imageViewId, drawableRes)
        views.setInt(imageViewId, "setColorFilter", opaqueRgb)
        views.setInt(imageViewId, "setImageAlpha", Color.alpha(fillColorArgb))
    }

    private fun bindHourlyTray(
        context: Context,
        views: RemoteViews,
        items: List<WidgetHourlyItem>,
        maxCount: Int,
        includePop: Boolean,
        trayFillColor: Int,
        activeBoxFillColor: Int,
        primaryTextColor: Int,
        mutedTextColor: Int,
        activePrimaryTextColor: Int,
        activeMutedTextColor: Int,
        popTextColor: Int,
        isLightSurface: Boolean,
        activeIsLightSurface: Boolean,
        softShadow: Boolean
    ) {
        if (items.isEmpty()) {
            views.setViewVisibility(R.id.widget_hourly_tray, View.GONE)
            return
        }
        views.setViewVisibility(R.id.widget_hourly_tray, View.VISIBLE)
        val cols = listOf(
            HourlySlotIds(R.id.hourly_col_0, R.id.hourly_box_bg_0, R.id.hourly_time_0, R.id.hourly_icon_0, R.id.hourly_pop_0, R.id.hourly_temp_0),
            HourlySlotIds(R.id.hourly_col_1, R.id.hourly_box_bg_1, R.id.hourly_time_1, R.id.hourly_icon_1, R.id.hourly_pop_1, R.id.hourly_temp_1),
            HourlySlotIds(R.id.hourly_col_2, R.id.hourly_box_bg_2, R.id.hourly_time_2, R.id.hourly_icon_2, R.id.hourly_pop_2, R.id.hourly_temp_2),
            HourlySlotIds(R.id.hourly_col_3, R.id.hourly_box_bg_3, R.id.hourly_time_3, R.id.hourly_icon_3, R.id.hourly_pop_3, R.id.hourly_temp_3),
            HourlySlotIds(R.id.hourly_col_4, R.id.hourly_box_bg_4, R.id.hourly_time_4, R.id.hourly_icon_4, R.id.hourly_pop_4, R.id.hourly_temp_4),
            HourlySlotIds(R.id.hourly_col_5, R.id.hourly_box_bg_5, R.id.hourly_time_5, R.id.hourly_icon_5, R.id.hourly_pop_5, R.id.hourly_temp_5)
        )
        val count = maxCount.coerceAtMost(cols.size)
        for (i in 0 until count) {
            val ids = cols[i]
            val item = items.getOrNull(i)
            if (item == null) {
                views.setViewVisibility(ids.col, View.INVISIBLE)
                continue
            }
            val isNow = i == 0
            val boxFill = if (isNow) activeBoxFillColor else trayFillColor
            val colPrimary = if (isNow) activePrimaryTextColor else primaryTextColor
            val colMuted = if (isNow) activeMutedTextColor else mutedTextColor
            val colLight = if (isNow) activeIsLightSurface else isLightSurface
            views.setViewVisibility(ids.col, View.VISIBLE)
            applyShapeSurfaceTint(
                views = views,
                imageViewId = ids.boxBg,
                drawableRes = R.drawable.widget_hourly_box,
                fillColorArgb = boxFill
            )
            views.setImageViewBitmap(
                ids.time,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = item.timeLabel,
                    textSizeSp = if (includePop) 11f else 10f,
                    colorArgb = colMuted,
                    fontWeight = if (isNow) 700 else 500,
                    densityDpi = DisplayMetrics.DENSITY_HIGH,
                    softShadow = softShadow
                )
            )
            val iconRes = WeatherIconHelper.getConditionDrawable(item.condition, item.isDay)
            val tint = WidgetCanvasRenderer.getConditionIconTint(item.condition, item.isDay, colLight)
            views.setImageViewResource(ids.icon, iconRes)
            views.setInt(ids.icon, "setColorFilter", tint)
            if (includePop) {
                // 常に高さを保持し、全カラムの気温ベースラインを水平に揃える
                views.setViewVisibility(ids.pop, View.VISIBLE)
                views.setImageViewBitmap(
                    ids.pop,
                    WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                        context = context,
                        text = item.precipProbText.ifEmpty { " " },
                        textSizeSp = 10f,
                        colorArgb = popTextColor,
                        fontWeight = 600,
                        densityDpi = DisplayMetrics.DENSITY_HIGH,
                        softShadow = false
                    )
                )
            }
            views.setImageViewBitmap(
                ids.temp,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = item.tempText,
                    textSizeSp = if (includePop) 14f else 13f,
                    colorArgb = colPrimary,
                    fontWeight = 700,
                    densityDpi = DisplayMetrics.DENSITY_XHIGH,
                    softShadow = softShadow
                )
            )
        }
    }

    private fun bindDailyTray(
        context: Context,
        views: RemoteViews,
        items: List<WidgetDailyItem>,
        weekMin: Int,
        weekMax: Int,
        style: WidgetStyle,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean,
        primaryTextColor: Int,
        mutedTextColor: Int,
        isLightSurface: Boolean,
        softShadow: Boolean
    ) {
        if (items.isEmpty()) {
            views.setViewVisibility(R.id.widget_daily_tray, View.GONE)
            return
        }
        views.setViewVisibility(R.id.widget_daily_tray, View.VISIBLE)
        val useMaterialYouColors = style == WidgetStyle.MATERIAL_YOU || style == WidgetStyle.FROSTED_GLASS
        val dividerColor = when {
            useMaterialYouColors && isLightSurface -> WidgetCanvasRenderer.withAlpha(myPalette.accentColor, 48)
            useMaterialYouColors -> WidgetCanvasRenderer.withAlpha(myPalette.accentColor, 68)
            else -> Color.argb(72, 255, 255, 255)
        }
        applyShapeSurfaceTint(
            views = views,
            imageViewId = R.id.widget_daily_divider,
            drawableRes = R.drawable.widget_pill_white,
            fillColorArgb = dividerColor
        )

        val rows = listOf(
            DailyRowIds(R.id.daily_row_0, R.id.daily_day_0, R.id.daily_icon_0, R.id.daily_min_0, R.id.daily_bar_0, R.id.daily_max_0),
            DailyRowIds(R.id.daily_row_1, R.id.daily_day_1, R.id.daily_icon_1, R.id.daily_min_1, R.id.daily_bar_1, R.id.daily_max_1),
            DailyRowIds(R.id.daily_row_2, R.id.daily_day_2, R.id.daily_icon_2, R.id.daily_min_2, R.id.daily_bar_2, R.id.daily_max_2),
            DailyRowIds(R.id.daily_row_3, R.id.daily_day_3, R.id.daily_icon_3, R.id.daily_min_3, R.id.daily_bar_3, R.id.daily_max_3),
            DailyRowIds(R.id.daily_row_4, R.id.daily_day_4, R.id.daily_icon_4, R.id.daily_min_4, R.id.daily_bar_4, R.id.daily_max_4),
            DailyRowIds(R.id.daily_row_5, R.id.daily_day_5, R.id.daily_icon_5, R.id.daily_min_5, R.id.daily_bar_5, R.id.daily_max_5),
            DailyRowIds(R.id.daily_row_6, R.id.daily_day_6, R.id.daily_icon_6, R.id.daily_min_6, R.id.daily_bar_6, R.id.daily_max_6)
        )
        val (trackColor, pillColor) = when (style) {
            WidgetStyle.MATERIAL_YOU, WidgetStyle.FROSTED_GLASS -> {
                val track = WidgetCanvasRenderer.withAlpha(
                    myPalette.accentColor,
                    if (isLightSurface) 62 else 78
                )
                val indicator = if (isSystemDark || isLightSurface) {
                    myPalette.accentColor
                } else {
                    WidgetCanvasRenderer.blendColors(myPalette.accentColor, Color.WHITE, 0.42f)
                }
                track to indicator
            }
            WidgetStyle.WEATHER_EXPRESSIVE -> {
                Color.argb(64, 255, 255, 255) to Color.rgb(228, 236, 244)
            }
        }
        for (i in rows.indices) {
            val ids = rows[i]
            val item = items.getOrNull(i)
            if (item == null) {
                views.setViewVisibility(ids.row, View.GONE)
                continue
            }
            views.setViewVisibility(ids.row, View.VISIBLE)
            views.setImageViewBitmap(
                ids.day,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = item.dayLabel,
                    textSizeSp = 13f,
                    colorArgb = mutedTextColor,
                    fontWeight = 500,
                    maxWidthDp = 36f,
                    densityDpi = DisplayMetrics.DENSITY_HIGH,
                    softShadow = softShadow
                )
            )
            val iconRes = WeatherIconHelper.getConditionDrawable(item.condition, isDay = true)
            val tint = WidgetCanvasRenderer.getConditionIconTint(item.condition, isDay = true, isLightSurface)
            views.setImageViewResource(ids.icon, iconRes)
            views.setInt(ids.icon, "setColorFilter", tint)
            views.setImageViewBitmap(
                ids.min,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = item.minTempVal.toString(),
                    textSizeSp = 13f,
                    colorArgb = mutedTextColor,
                    fontWeight = 500,
                    maxWidthDp = 26f,
                    densityDpi = DisplayMetrics.DENSITY_HIGH,
                    softShadow = softShadow
                )
            )
            views.setImageViewBitmap(
                ids.bar,
                WidgetCanvasRenderer.renderTempRangeBarBitmap(
                    dayMin = item.minTempVal,
                    dayMax = item.maxTempVal,
                    weekMin = weekMin,
                    weekMax = weekMax,
                    trackColorArgb = trackColor,
                    pillColorArgb = pillColor
                )
            )
            views.setImageViewBitmap(
                ids.max,
                WidgetCanvasRenderer.renderGoogleSansTextBitmap(
                    context = context,
                    text = item.maxTempVal.toString(),
                    textSizeSp = 13f,
                    colorArgb = primaryTextColor,
                    fontWeight = 700,
                    maxWidthDp = 26f,
                    densityDpi = DisplayMetrics.DENSITY_HIGH,
                    softShadow = softShadow
                )
            )
        }
    }

    private fun createOpenAppPendingIntent(
        context: Context,
        appWidgetId: Int,
        model: WidgetUiModel
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_WIDGET_SELECT_CURRENT, model.isCurrentLocation)
            if (!model.isCurrentLocation) {
                WeatherRepository.encodeCityJson(model.targetCity)?.let { cityJson ->
                    putExtra(WeatherRepository.EXTRA_CITY_JSON, cityJson)
                }
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQUEST_OPEN_BASE + appWidgetId, intent, flags)
    }

    private fun createOpenRadarPendingIntent(
        context: Context,
        appWidgetId: Int,
        city: City
    ): PendingIntent {
        val intent = Intent(context, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("city_name", city.name)
            putExtra("extra_lat", city.latitude)
            putExtra("extra_lon", city.longitude)
            WeatherRepository.encodeCityJson(city)?.let { cityJson ->
                putExtra(WeatherRepository.EXTRA_CITY_JSON, cityJson)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQUEST_RADAR_BASE + appWidgetId, intent, flags)
    }

    private fun createSettingsPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQUEST_CONFIG_BASE + appWidgetId, intent, flags)
    }

    private fun createRepository(context: Context): WeatherRepository {
        val db = WeatherDatabase.getInstance(context)
        return WeatherRepository(
            weatherDao = db.weatherDao(),
            savedCityDao = db.savedCityDao(),
            context = context
        )
    }

    private data class HourlySlotIds(val col: Int, val boxBg: Int, val time: Int, val icon: Int, val pop: Int, val temp: Int)
    private data class DailyRowIds(val row: Int, val day: Int, val icon: Int, val min: Int, val bar: Int, val max: Int)

    const val EXTRA_WIDGET_ID = "extra_widget_id"
    const val EXTRA_WIDGET_SELECT_CURRENT = "extra_widget_select_current"
    private const val REQUEST_OPEN_BASE = 20_000
    private const val REQUEST_CONFIG_BASE = 40_000
    private const val REQUEST_RADAR_BASE = 60_000
}
