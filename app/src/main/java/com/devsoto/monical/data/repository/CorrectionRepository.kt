package com.devsoto.monical.data.repository

import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.refine.CorrectionDictionary
import com.devsoto.monical.data.refine.LearnedCorrections
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/**
 * Persistence for the learned correction dictionary. One document per user
 * (`users/{uid}/meta/corrections`), so [load] is a single read cached in memory by the caller.
 */
interface CorrectionRepository {
    /** Loads the learned dictionary (one document read); empty when none exists yet. */
    suspend fun load(): CorrectionDictionary

    /** Merges newly learned entries into the stored dictionary. No-op when empty. */
    suspend fun learn(corrections: LearnedCorrections)
}

class FirestoreCorrectionRepository(
    private val auth: AuthManager,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : CorrectionRepository {

    override suspend fun load(): CorrectionDictionary {
        val uid = auth.ensureSignedIn()
        val snap = doc(uid).get().await()
        if (!snap.exists()) return CorrectionDictionary.EMPTY
        return CorrectionDictionary(
            merchants = readMap(snap.get(FIELD_MERCHANTS)),
            items = readMap(snap.get(FIELD_ITEMS)),
        )
    }

    override suspend fun learn(corrections: LearnedCorrections) {
        if (corrections.isEmpty()) return
        val uid = auth.ensureSignedIn()
        // SetOptions.merge() deep-merges the nested maps, so existing keys are preserved.
        val data = mapOf(
            FIELD_MERCHANTS to corrections.merchants,
            FIELD_ITEMS to corrections.items,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
        doc(uid).set(data, SetOptions.merge()).await()
    }

    private fun doc(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_META).document(DOC_CORRECTIONS)

    @Suppress("UNCHECKED_CAST")
    private fun readMap(value: Any?): Map<String, String> =
        (value as? Map<String, Any?>)
            ?.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }
            ?.toMap()
            .orEmpty()

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_META = "meta"
        const val DOC_CORRECTIONS = "corrections"
        const val FIELD_MERCHANTS = "merchants"
        const val FIELD_ITEMS = "items"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
