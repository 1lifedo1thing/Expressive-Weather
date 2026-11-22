package io.github.nemynew.expressiveweather.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 全サイズのホーム画面ウィジェット共通 AppWidgetProvider 基底クラス
 */
abstract class BaseWeatherWidgetProvider(
    private val defaultTier: WidgetSizeTier
) : AppWidgetProvider() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val appCtx = context.applicationContext
        WidgetRefreshWorker.ensurePeriodicSchedule(appCtx)
        WidgetRefreshWorker.enqueueRefresh(appCtx)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val appCtx = context.applicationContext
        WidgetRefreshWorker.ensurePeriodicSchedule(appCtx)
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                WeatherWidgetUpdater.updateAllFromCache(appCtx)
                // 初回配置や OS 定期更新時に最新天気も非同期同期
                WidgetRefreshWorker.enqueueRefresh(appCtx)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "onUpdate error: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WeatherWidgetUpdater.updateWidgetAsync(context.applicationContext, appWidgetId, fetchIfMissing = true)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetPreferences.deleteConfigs(context.applicationContext, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val appCtx = context.applicationContext
        if (WeatherWidgetUpdater.getActiveWidgets(appCtx).isEmpty()) {
            WidgetRefreshWorker.cancelPeriodicSchedule(appCtx)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_CONFIGURATION_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == Intent.ACTION_LOCALE_CHANGED
        ) {
            WeatherWidgetUpdater.updateAllFromCacheAsync(context.applicationContext)
        }
    }

    companion object {
        private const val TAG = "WeatherWidgetProvider"
    }
}

class WeatherWidgetCompactProvider : BaseWeatherWidgetProvider(WidgetSizeTier.COMPACT)
class WeatherWidgetMediumProvider : BaseWeatherWidgetProvider(WidgetSizeTier.MEDIUM)
class WeatherWidgetLargeProvider : BaseWeatherWidgetProvider(WidgetSizeTier.LARGE)
class WeatherWidgetShapeProvider : BaseWeatherWidgetProvider(WidgetSizeTier.COMPACT)
class WeatherWidgetRadarProvider : BaseWeatherWidgetProvider(WidgetSizeTier.MEDIUM)
