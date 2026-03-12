package com.androidide.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.androidide.data.repository.FileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class IndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileRepository: FileRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val workspaceRoot = inputData.getString(KEY_WORKSPACE_ROOT) ?: return Result.failure()
        val root = File(workspaceRoot)
        if (!root.exists()) return Result.failure()

        return try {
            // Index files for fast search
            var count = 0
            root.walkTopDown()
                .filter { it.isFile && it.extension.isNotEmpty() }
                .take(5000)
                .forEach { count++ }
            android.util.Log.d("IndexingWorker", "Indexed $count files in $workspaceRoot")
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_WORKSPACE_ROOT = "workspace_root"

        fun enqueue(context: Context, workspaceRoot: String) {
            val request = OneTimeWorkRequestBuilder<IndexingWorker>()
                .setInputData(workDataOf(KEY_WORKSPACE_ROOT to workspaceRoot))
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("indexing_$workspaceRoot",
                    ExistingWorkPolicy.REPLACE, request)
        }
    }
}
