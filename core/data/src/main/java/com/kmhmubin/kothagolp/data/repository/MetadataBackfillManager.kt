package com.kmhmubin.kothagolp.data.repository

import android.content.Context
import com.kmhmubin.kothagolp.data.local.dao.LibraryDao
import com.kmhmubin.kothagolp.data.local.dao.OfflineDao
import com.kmhmubin.kothagolp.data.repository.LibraryRepository
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackfillProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentNovelTitle: String = "",
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val isRunning: Boolean = false,
    val isCancelled: Boolean = false
)

class MetadataBackfillManager(
    private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val libraryDao: LibraryDao,
    private val offlineDao: OfflineDao
) {
    private val _backfillProgress = MutableStateFlow(BackfillProgress())
    val backfillProgress = _backfillProgress.asStateFlow()

    private var backfillJob: Job? = null

    suspend fun startBackfill() {
        if (backfillJob?.isActive == true) return

        backfillJob = CoroutineScope(Dispatchers.Default).launch {
            _backfillProgress.value = BackfillProgress(isRunning = true)
            try {
                performBackfill()
            } catch (e: CancellationException) {
                _backfillProgress.value = _backfillProgress.value.copy(
                    isCancelled = true,
                    isRunning = false
                )
                throw e
            } catch (e: Exception) {
                _backfillProgress.value = _backfillProgress.value.copy(isRunning = false)
            }
        }
    }

    fun cancelBackfill() {
        backfillJob?.cancel()
    }

    private suspend fun performBackfill() = withContext(Dispatchers.IO) {
        val allLibraryEntities = libraryDao.getAll()
        val total = allLibraryEntities.size

        if (total == 0) {
            _backfillProgress.value = BackfillProgress(isRunning = false)
            return@withContext
        }

        var success = 0
        var skipped = 0
        var failed = 0

        // Process in batches of 10 to balance memory vs concurrency
        allLibraryEntities.chunked(10).forEachIndexed { batchIdx, batch ->
            val tasks = batch.mapIndexed { idx, entity ->
                async(Dispatchers.IO) {
                    val current = batchIdx * 10 + idx + 1
                    _backfillProgress.value = _backfillProgress.value.copy(
                        current = current,
                        total = total,
                        currentNovelTitle = entity.name
                    )

                    try {
                        // Skip if already cached
                        val existing = offlineDao.getNovelDetails(entity.url)
                        if (existing != null) {
                            skipped++
                            return@async
                        }

                        // Fetch and cache metadata
                        runCatching {
                            libraryRepository.fetchAndCacheNovelDetails(entity.url)
                        }.onSuccess {
                            success++
                        }.onFailure {
                            // Source dead/unavailable — tolerate gracefully
                            failed++
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                }
            }

            // Wait for batch; collect counts before next batch (tight memory window)
            runBlocking {
                tasks.awaitAll()
            }

            _backfillProgress.value = _backfillProgress.value.copy(
                successCount = success,
                skippedCount = skipped,
                failedCount = failed
            )
        }

        _backfillProgress.value = _backfillProgress.value.copy(isRunning = false)
    }
}
