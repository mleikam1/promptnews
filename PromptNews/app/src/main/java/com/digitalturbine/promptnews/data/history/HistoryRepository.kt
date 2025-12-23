package com.digitalturbine.promptnews.data.history

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HistoryRepository private constructor(
    context: Context,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private val dao = HistoryDatabase.getInstance(context).historyDao()

    fun recentEntries(): Flow<List<HistoryEntry>> {
        return dao.observeEntriesSince(clockMs() - HISTORY_RETENTION_MS)
    }

    suspend fun addEntry(type: HistoryType, label: String) {
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label,
            timestampMs = clockMs()
        )
        dao.insert(entry)
        pruneOldEntries()
    }

    suspend fun pruneOldEntries() {
        dao.deleteOlderThan(clockMs() - HISTORY_RETENTION_MS)
    }

    companion object {
        private const val HISTORY_RETENTION_MS = 48 * 60 * 60 * 1000L

        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: HistoryRepository(context).also { instance = it }
            }
        }
    }
}
