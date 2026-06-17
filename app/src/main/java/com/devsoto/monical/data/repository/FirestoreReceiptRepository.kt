package com.devsoto.monical.data.repository

import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.model.MonthlyRollup
import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReceiptCard
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.ReceiptSummary
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.UNCATEGORIZED
import com.devsoto.monical.data.model.applyArchive
import com.devsoto.monical.data.model.applyDelete
import com.devsoto.monical.data.model.applySave
import com.devsoto.monical.data.model.buildSummary
import com.devsoto.monical.data.model.monthKey
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Stores receipts in Cloud Firestore, scoped to the current anonymous user from [AuthManager].
 *
 * Layout (see CLAUDE.md):
 * - `users/{uid}/receipts/{id}` — active (PENDING) receipts. Read 1-at-a-time on detail.
 * - `users/{uid}/archive/{id}`  — cold receipts (RETURNED / NONE). Not observed; read on demand.
 * - `users/{uid}/meta/summary`  — the single denormalized dashboard document Home observes.
 *
 * Every mutation runs in a transaction that also updates `meta/summary` via the pure helpers in
 * `SummaryMath`, so the dashboard never drifts from the receipt documents.
 */
class FirestoreReceiptRepository(
    private val auth: AuthManager,
    private val settings: SettingsRepository,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : ReceiptRepository {

    override fun observeSummary(): Flow<ReceiptSummary> = callbackFlow {
        val uid = auth.ensureSignedIn()
        val backfillStarted = AtomicBoolean(false)
        val registration = summaryDoc(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                trySend(snap.toSummary())
            } else {
                // No summary yet: emit empty and backfill once from any pre-existing receipts.
                trySend(ReceiptSummary())
                if (backfillStarted.compareAndSet(false, true)) {
                    launch { runCatching { backfillSummary(uid) } }
                }
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getReceipt(id: String, archived: Boolean): Receipt? {
        if (id.isBlank()) return null
        val uid = auth.ensureSignedIn()
        val primary = if (archived) userArchive(uid) else userReceipts(uid)
        primary.document(id).get().await().toReceipt()?.let { return it }
        val secondary = if (archived) userReceipts(uid) else userArchive(uid)
        return secondary.document(id).get().await().toReceipt()
    }

    override suspend fun save(receipt: Receipt): String {
        val uid = auth.ensureSignedIn()
        val archived = receipt.returnStatus != ReturnStatus.PENDING
        // Freeze the current global share onto the receipt the moment it's archived; keep any
        // existing stamp so re-saves don't retroactively re-rate it.
        val frozenShare = if (archived) receipt.returnShare ?: settings.load().returnShare else receipt.returnShare
        val targetCol = if (archived) userArchive(uid) else userReceipts(uid)
        val ref = if (receipt.id.isBlank()) targetCol.document() else targetCol.document(receipt.id)
        val otherCol = if (archived) userReceipts(uid) else userArchive(uid)
        val counterpartRef = otherCol.document(ref.id)
        val summaryRef = summaryDoc(uid)

        firestore.runTransaction { txn ->
            // ── reads ──
            val summary = txn.get(summaryRef).toSummaryOrEmpty()
            val counterpartExists = receipt.id.isNotBlank() && txn.get(counterpartRef).exists()
            // ── writes ──
            val stored = receipt.copy(id = ref.id, returnShare = frozenShare)
            val newSummary = if (archived) applyArchive(summary, stored) else applySave(summary, stored)
            txn.set(ref, if (archived) stored.toArchiveMap() else stored.toMap())
            txn.set(summaryRef, newSummary.toMap())
            if (counterpartExists) txn.delete(counterpartRef) // moved between collections
            Unit
        }.await()
        return ref.id
    }

    override suspend fun delete(id: String) {
        if (id.isBlank()) return
        val uid = auth.ensureSignedIn()
        val summaryRef = summaryDoc(uid)
        val activeRef = userReceipts(uid).document(id)
        val archiveRef = userArchive(uid).document(id)
        firestore.runTransaction { txn ->
            val summary = txn.get(summaryRef).toSummaryOrEmpty()
            val activeSnap = txn.get(activeRef)
            val archiveSnap = txn.get(archiveRef)
            val receipt = (activeSnap.toReceipt() ?: archiveSnap.toReceipt())?.copy(id = id)
            if (receipt != null) txn.set(summaryRef, applyDelete(summary, receipt).toMap())
            if (activeSnap.exists()) txn.delete(activeRef)
            if (archiveSnap.exists()) txn.delete(archiveRef)
            Unit
        }.await()
    }

    override suspend fun markReturned(ids: List<String>) {
        val toMove = ids.filter { it.isNotBlank() }
        if (toMove.isEmpty()) return
        val uid = auth.ensureSignedIn()
        // Freeze the current global share onto each receipt as it's returned.
        val share = settings.load().returnShare
        val summaryRef = summaryDoc(uid)
        firestore.runTransaction { txn ->
            // ── reads (all before writes) ──
            var summary = txn.get(summaryRef).toSummaryOrEmpty()
            val moves = toMove.mapNotNull { id ->
                txn.get(userReceipts(uid).document(id)).toReceipt()
                    ?.copy(id = id, returnStatus = ReturnStatus.RETURNED, returnShare = share)
            }
            // ── writes ──
            moves.forEach { r ->
                txn.set(userArchive(uid).document(r.id), r.toArchiveMap())
                txn.delete(userReceipts(uid).document(r.id))
                summary = applyArchive(summary, r)
            }
            txn.set(summaryRef, summary.toMap())
            Unit
        }.await()
    }

    override suspend fun listArchived(month: String): List<ReceiptCard> {
        val uid = auth.ensureSignedIn()
        return userArchive(uid).whereEqualTo(FIELD_MONTH_KEY, month).get().await()
            .documents.mapNotNull { it.toCard() }
    }

    /** One-time migration: derive the summary from any receipts written under the old schema. */
    private suspend fun backfillSummary(uid: String) {
        if (summaryDoc(uid).get().await().exists()) return
        val legacy = userReceipts(uid).get().await().documents.mapNotNull { it.toReceipt() }
        summaryDoc(uid).set(buildSummary(legacy).toMap()).await()
    }

    // ── refs ──────────────────────────────────────────────────
    private fun userReceipts(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_RECEIPTS)

    private fun userArchive(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_ARCHIVE)

    private fun summaryDoc(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_META).document(DOC_SUMMARY)

    // ── Receipt (de)serialization ─────────────────────────────
    private fun Receipt.toMap(): Map<String, Any?> = mapOf(
        "merchant" to merchant,
        "dateMillis" to dateMillis,
        "total" to total,
        "currency" to currency,
        "items" to items.map { it.toMap() },
        "category" to category,
        "returnStatus" to returnStatus.name,
        "rawText" to rawText,
        "source" to source.name,
        "returnShare" to returnShare,
        FIELD_CREATED_AT to createdAt,
    )

    /** Archive documents carry a [FIELD_MONTH_KEY] so [listArchived] can query a single month. */
    private fun Receipt.toArchiveMap(): Map<String, Any?> =
        toMap() + (FIELD_MONTH_KEY to monthKey(dateMillis, createdAt))

    private fun ReceiptItem.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "lineTotal" to lineTotal,
        "isAdjustment" to isAdjustment,
        "returnable" to returnable,
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toReceipt(): Receipt? {
        if (!exists()) return null
        val itemMaps = get("items") as? List<Map<String, Any?>> ?: emptyList()
        return Receipt(
            id = id,
            merchant = getString("merchant"),
            dateMillis = getLong("dateMillis"),
            total = getDouble("total"),
            currency = getString("currency"),
            items = itemMaps.map { map ->
                ReceiptItem(
                    name = map["name"] as? String ?: "",
                    quantity = (map["quantity"] as? Number)?.toDouble(),
                    unitPrice = (map["unitPrice"] as? Number)?.toDouble(),
                    lineTotal = (map["lineTotal"] as? Number)?.toDouble(),
                    isAdjustment = map["isAdjustment"] as? Boolean ?: false,
                    returnable = map["returnable"] as? Boolean ?: true,
                )
            },
            category = getString("category") ?: UNCATEGORIZED,
            returnStatus = getString("returnStatus")
                ?.let { runCatching { ReturnStatus.valueOf(it) }.getOrNull() }
                ?: ReturnStatus.PENDING,
            rawText = getString("rawText").orEmpty(),
            source = getString("source")?.let { runCatching { ParseSource.valueOf(it) }.getOrNull() }
                ?: ParseSource.MANUAL,
            returnShare = getDouble("returnShare"),
            createdAt = getLong(FIELD_CREATED_AT) ?: 0L,
        )
    }

    private fun DocumentSnapshot.toCard(): ReceiptCard? {
        if (!exists()) return null
        return ReceiptCard(
            id = id,
            merchant = getString("merchant"),
            dateMillis = getLong("dateMillis"),
            total = getDouble("total"),
            currency = getString("currency"),
            category = getString("category") ?: UNCATEGORIZED,
            returnStatus = getString("returnStatus")
                ?.let { runCatching { ReturnStatus.valueOf(it) }.getOrNull() }
                ?: ReturnStatus.PENDING,
            returnBase = getDouble("returnBase"),
        )
    }

    // ── Summary (de)serialization ─────────────────────────────
    private fun ReceiptSummary.toMap(): Map<String, Any?> = mapOf(
        "pendingTotal" to pendingTotal,
        "active" to active.map { it.toMap() },
        "archivedMonthly" to archivedMonthly.mapValues { it.value.toMap() },
        "updatedAt" to updatedAt,
    )

    private fun ReceiptCard.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "merchant" to merchant,
        "dateMillis" to dateMillis,
        "total" to total,
        "currency" to currency,
        "category" to category,
        "returnStatus" to returnStatus.name,
        "returnBase" to returnBase,
    )

    private fun MonthlyRollup.toMap(): Map<String, Any?> = mapOf(
        "count" to count,
        "total" to total,
        "refund" to refund,
    )

    private fun DocumentSnapshot.toSummaryOrEmpty(): ReceiptSummary =
        if (exists()) toSummary() else ReceiptSummary()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toSummary(): ReceiptSummary {
        val activeMaps = get("active") as? List<Map<String, Any?>> ?: emptyList()
        val monthlyMaps = get("archivedMonthly") as? Map<String, Map<String, Any?>> ?: emptyMap()
        return ReceiptSummary(
            pendingTotal = getDouble("pendingTotal") ?: 0.0,
            active = activeMaps.map { m ->
                ReceiptCard(
                    id = m["id"] as? String ?: "",
                    merchant = m["merchant"] as? String,
                    dateMillis = (m["dateMillis"] as? Number)?.toLong(),
                    total = (m["total"] as? Number)?.toDouble(),
                    currency = m["currency"] as? String,
                    category = m["category"] as? String ?: UNCATEGORIZED,
                    returnStatus = (m["returnStatus"] as? String)
                        ?.let { runCatching { ReturnStatus.valueOf(it) }.getOrNull() }
                        ?: ReturnStatus.PENDING,
                    returnBase = (m["returnBase"] as? Number)?.toDouble(),
                )
            },
            archivedMonthly = monthlyMaps.mapValues { (_, mm) ->
                MonthlyRollup(
                    count = (mm["count"] as? Number)?.toInt() ?: 0,
                    total = (mm["total"] as? Number)?.toDouble() ?: 0.0,
                    refund = (mm["refund"] as? Number)?.toDouble() ?: 0.0,
                )
            },
            updatedAt = getLong("updatedAt") ?: 0L,
        )
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_RECEIPTS = "receipts"
        const val COLLECTION_ARCHIVE = "archive"
        const val COLLECTION_META = "meta"
        const val DOC_SUMMARY = "summary"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_MONTH_KEY = "monthKey"
    }
}
