package app.luma.gallery.index

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.luma.gallery.data.MediaStoreSource
import java.util.concurrent.TimeUnit

/** Analyzes the photos not yet in the index; Android stops it as soon as its conditions end. */
class IndexWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // The periodic run waits for «charging» from Android and checks «screen off» itself (some phones are never
        // «charging and idle» at once). «Analizar ahora» runs with the screen on.
        val power = applicationContext.getSystemService(android.os.PowerManager::class.java)
        if (inputData.getBoolean(KEY_QUIET, true) && power?.isInteractive == true) return Result.retry()
        val media = runCatching { MediaStoreSource(applicationContext).load() }.getOrElse { return Result.retry() }
        val analyzer = MlKitAnalyzer(applicationContext)
        val store = SqlFacts(applicationContext)
        return try {
            Indexer(store, analyzer).run(media, inputData.getInt(KEY_LIMIT, BATCH)) { done, total ->
                setProgressAsync(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
            }
            Result.success()
        } finally { analyzer.close(); store.close() }
    }

    companion object {
        const val KEY_DONE = "done"; const val KEY_TOTAL = "total"; private const val KEY_LIMIT = "limit"
        private const val KEY_QUIET = "quiet"
        private const val BATCH = 400
        const val PERIODIC = "luma-index"; const val NOW = "luma-index-now"

        /** Every 6 h, only while charging with the screen off, like the phone's own gallery. */
        fun schedule(context: Context) {
            val quiet = Constraints.Builder().setRequiresCharging(true).setRequiresBatteryNotLow(true).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<IndexWorker>(6, TimeUnit.HOURS).setConstraints(quiet).build())
        }

        /** «Analizar ahora» (Ajustes): right away, a batch at a time, as long as the battery is not low. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<IndexWorker>()
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .setInputData(workDataOf(KEY_LIMIT to 1_000, KEY_QUIET to false)).build())
        }

        fun stop(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NOW)
    }
}
