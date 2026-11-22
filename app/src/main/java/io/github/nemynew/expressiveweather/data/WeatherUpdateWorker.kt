package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager を使用した天気の永続的バックグラウンド定期更新ワーカー
 * - アプリが終了していても、OSによって確実に起動され実行されます
 * - ネットワーク接続が確立されている場合にのみ実行
 * - 実行成功・失敗を適切にWorkManagerへ報告
 */
import kotlinx.coroutines.CancellationException

class WeatherUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "WeatherUpdateWorker started background execution (Attempt: $runAttemptCount).")
        return try {
            val updateManager = WeatherBackgroundUpdateManager.getInstance(applicationContext)
            val success = updateManager.performBackgroundWeatherUpdate(isForced = false)
            if (success) {
                Log.d(TAG, "WeatherUpdateWorker completed successfully.")
                Result.success()
            } else {
                Log.w(TAG, "WeatherUpdateWorker could not complete update, scheduling retry.")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "WeatherUpdateWorker error during execution: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val TAG = "WeatherUpdateWorker"
        const val UNIQUE_WORK_NAME = "io.github.nemynew.expressiveweather.work.WEATHER_UPDATE_PERIODIC"
    }
}
