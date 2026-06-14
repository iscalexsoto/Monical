package com.devsoto.monical.data.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Anonymous Authentication. Each install gets a stable anonymous UID used
 * to scope the user's receipts in Firestore (`users/{uid}/receipts`).
 *
 * Can be migrated to Google Sign-In later by linking the anonymous account.
 */
class AuthManager(private val auth: FirebaseAuth = Firebase.auth) {

    val uid: String?
        get() = auth.currentUser?.uid

    /**
     * Ensures there is a signed-in (anonymous) user and returns its UID, signing in if needed.
     * Idempotent: returns the existing UID when already authenticated.
     *
     * @throws Exception if anonymous sign-in fails (e.g. it's disabled in the Firebase console).
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid
            ?: throw IllegalStateException("Anonymous sign-in returned no user")
    }
}
