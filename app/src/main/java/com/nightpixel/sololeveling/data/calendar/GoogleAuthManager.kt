package com.nightpixel.sololeveling.data.calendar

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events"

data class GoogleAccountInfo(val email: String, val displayName: String?)

/**
 * Credential Manager proves *who* is signed in; Google's current guidance keeps that separate
 * from *what* they've granted access to, which is why Calendar scope access goes through the
 * standalone Authorization Client below (not Credential Manager) and returns its own access
 * token. Calling requestCalendarAccess() again after the first consent re-grants silently (no
 * UI) as long as the scope is still authorized, so no token is persisted here - the app just
 * asks again right before each API call.
 */
class GoogleAuthManager(private val webClientId: String) {

    suspend fun signIn(context: Context): GoogleAccountInfo {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(context).getCredential(context, request)

        val credential = response.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential.type}"
        }
        val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleAccountInfo(email = idTokenCredential.id, displayName = idTokenCredential.displayName)
    }

    suspend fun signOut(context: Context) {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    fun requestCalendarAccess(
        context: Context,
        onAuthorized: (accessToken: String) -> Unit,
        onNeedsConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(CALENDAR_SCOPE)))
            .build()

        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { result ->
                when {
                    result.hasResolution() -> {
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent != null) {
                            onNeedsConsent(pendingIntent.intentSender)
                        } else {
                            onError(IllegalStateException("Calendar consent required but no resolution was returned"))
                        }
                    }
                    result.accessToken != null -> onAuthorized(result.accessToken!!)
                    else -> onError(IllegalStateException("No access token returned"))
                }
            }
            .addOnFailureListener(onError)
    }

    fun handleConsentResult(
        context: Context,
        data: Intent?,
        onAuthorized: (accessToken: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token != null) onAuthorized(token) else onError(IllegalStateException("No access token returned"))
        } catch (e: Exception) {
            onError(e)
        }
    }
}
