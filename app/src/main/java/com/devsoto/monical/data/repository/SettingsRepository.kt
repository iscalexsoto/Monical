package com.devsoto.monical.data.repository

import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Persistence for user-scoped [UserSettings]. One document per user (`users/{uid}/meta/settings`),
 * mirroring [CorrectionRepository] / the `meta/summary` doc, so reads stay cheap.
 */
interface SettingsRepository {
    /** Real-time settings; emits [UserSettings.DEFAULT] while no document exists yet. */
    fun observe(): Flow<UserSettings>

    /** One-shot read; used by the receipt repo to stamp the current share when archiving. */
    suspend fun load(): UserSettings

    /** Persists the settings (deep-merged so future fields aren't clobbered). */
    suspend fun update(settings: UserSettings)
}

class FirestoreSettingsRepository(
    private val auth: AuthManager,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : SettingsRepository {

    override fun observe(): Flow<UserSettings> = callbackFlow {
        val uid = auth.ensureSignedIn()
        val registration = doc(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(
                if (snap != null && snap.exists()) {
                    UserSettings(returnShare = snap.getDouble(FIELD_RETURN_SHARE) ?: UserSettings.DEFAULT.returnShare)
                } else {
                    UserSettings.DEFAULT
                },
            )
        }
        awaitClose { registration.remove() }
    }

    override suspend fun load(): UserSettings {
        val uid = auth.ensureSignedIn()
        val snap = doc(uid).get().await()
        if (!snap.exists()) return UserSettings.DEFAULT
        return UserSettings(returnShare = snap.getDouble(FIELD_RETURN_SHARE) ?: UserSettings.DEFAULT.returnShare)
    }

    override suspend fun update(settings: UserSettings) {
        val uid = auth.ensureSignedIn()
        val data = mapOf(
            FIELD_RETURN_SHARE to settings.returnShare,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
        doc(uid).set(data, SetOptions.merge()).await()
    }

    private fun doc(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_META).document(DOC_SETTINGS)

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_META = "meta"
        const val DOC_SETTINGS = "settings"
        const val FIELD_RETURN_SHARE = "returnShare"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
