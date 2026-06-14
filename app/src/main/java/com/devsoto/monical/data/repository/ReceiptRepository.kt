package com.devsoto.monical.data.repository

import com.devsoto.monical.data.model.Receipt
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for receipts. Implemented by [FirestoreReceiptRepository] today; the
 * interface keeps the UI decoupled so the backend can change (the DB was "to be determined").
 */
interface ReceiptRepository {

    /** Saves a receipt for the current user and returns its generated id. */
    suspend fun save(receipt: Receipt): String

    /** Emits the current user's receipts, newest first, updating in real time. */
    fun observeReceipts(): Flow<List<Receipt>>
}
