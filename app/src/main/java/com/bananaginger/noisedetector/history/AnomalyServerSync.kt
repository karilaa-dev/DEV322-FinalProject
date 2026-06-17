package com.bananaginger.noisedetector.history

/**
 * AnomalyServerSync — interface for uploading history entries to a remote server.
 *
 * Implementation plan:
 *  1. Call uploadEntry() on every new HistoryEntry (fire-and-forget coroutine).
 *  2. Replace NoOpServerSync with a real Retrofit/Ktor implementation once the
 *     server API endpoint is provided.
 *
 * The interface is intentionally minimal: one suspend function per entry.
 * Batching, retry logic and auth headers should be added in the real implementation.
 */
interface AnomalyServerSync {
    /**
     * Upload a single history entry to the remote database.
     * Called on every new detection event.
     *
     * @param entry The entry to upload.
     */
    suspend fun uploadEntry(entry: HistoryEntry)
}

/**
 * NoOpServerSync — placeholder that does nothing.
 * Replace with a real implementation once the API URL and schema are known.
 *
 * Usage in ViewModel:
 *   private val serverSync: AnomalyServerSync = NoOpServerSync
 */
object NoOpServerSync : AnomalyServerSync {
    override suspend fun uploadEntry(entry: HistoryEntry) {
        // TODO: replace with real HTTP call when API is provided.
        // Example Retrofit call:
        //   apiService.postAnomaly(entry.toDto())
    }
}
