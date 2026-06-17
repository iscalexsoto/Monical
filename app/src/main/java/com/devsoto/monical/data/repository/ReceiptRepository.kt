package com.devsoto.monical.data.repository

import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReceiptCard
import com.devsoto.monical.data.model.ReceiptSummary
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for receipts. Implemented by [FirestoreReceiptRepository] today; the
 * interface keeps the UI decoupled so the backend can change (the DB was "to be determined").
 *
 * Reads are deliberately frugal: Home observes a single denormalized [ReceiptSummary] document,
 * and a receipt's full detail is fetched one document at a time only when opened.
 */
interface ReceiptRepository {

    /** Emits the user's dashboard summary (one document), updating in real time. */
    fun observeSummary(): Flow<ReceiptSummary>

    /** Fetches one full receipt by id (one read), from the active or [archived] collection. */
    suspend fun getReceipt(id: String, archived: Boolean = false): Receipt?

    /** Saves a receipt and updates the summary atomically; returns its generated id. */
    suspend fun save(receipt: Receipt): String

    /** Deletes a receipt by id (active or archived) and updates the summary. */
    suspend fun delete(id: String)

    /**
     * Marks the given receipts [com.devsoto.monical.data.model.ReturnStatus.RETURNED], moving them
     * to the cold archive collection and folding their numbers into the monthly rollups.
     */
    suspend fun markReturned(ids: List<String>)

    /** Lightweight history listing for one archived "yyyy-MM" month, read on demand. */
    suspend fun listArchived(month: String): List<ReceiptCard>
}
