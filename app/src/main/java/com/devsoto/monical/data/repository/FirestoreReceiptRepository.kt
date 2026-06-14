package com.devsoto.monical.data.repository

import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReceiptItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Stores receipts in Cloud Firestore under `users/{uid}/receipts`, scoped to the current
 * anonymous user from [AuthManager]. Document keys mirror [Receipt]'s fields.
 */
class FirestoreReceiptRepository(
    private val auth: AuthManager,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : ReceiptRepository {

    override suspend fun save(receipt: Receipt): String {
        val uid = auth.ensureSignedIn()
        val collection = userReceipts(uid)
        val doc = if (receipt.id.isBlank()) collection.document() else collection.document(receipt.id)
        doc.set(receipt.toMap()).await()
        return doc.id
    }

    override fun observeReceipts(): Flow<List<Receipt>> = callbackFlow {
        val uid = auth.ensureSignedIn()
        val registration = userReceipts(uid)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val receipts = snapshot?.documents?.mapNotNull { it.toReceipt() }.orEmpty()
                trySend(receipts)
            }
        awaitClose { registration.remove() }
    }

    private fun userReceipts(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_RECEIPTS)

    private fun Receipt.toMap(): Map<String, Any?> = mapOf(
        "merchant" to merchant,
        "dateMillis" to dateMillis,
        "total" to total,
        "currency" to currency,
        "items" to items.map { it.toMap() },
        "rawText" to rawText,
        "source" to source.name,
        FIELD_CREATED_AT to createdAt,
    )

    private fun ReceiptItem.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "lineTotal" to lineTotal,
    )

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toReceipt(): Receipt? {
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
                )
            },
            rawText = getString("rawText").orEmpty(),
            source = getString("source")?.let { runCatching { ParseSource.valueOf(it) }.getOrNull() }
                ?: ParseSource.MANUAL,
            createdAt = getLong(FIELD_CREATED_AT) ?: 0L,
        )
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_RECEIPTS = "receipts"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
