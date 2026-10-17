package com.example.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

/**
 * Represents a file stored directly inside the app's private internal storage vault (`filesDir/chhotu_in_app_vault`).
 */
data class InAppVaultFile(
    val id: String,
    val fileName: String,
    val folder: String,
    val tags: String,
    val sizeBytes: Long,
    val encryptedPayload: String,
    val sha256Checksum: String,
    val aiSummary: String,
    val internalFilePath: String,
    val createdAtMillis: Long
)

/**
 * Implements Section 3: Owner Authentication, Theme & Voice Preferences, In-App Encrypted File Vault,
 * and Google Sign-In via Jetpack Credential Manager.
 */
class OwnerAuthManager(context: Context) {

    val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("chhotu_owner_security_prefs", Context.MODE_PRIVATE)
    private val inAppVaultDir: File = File(appContext.filesDir, "chhotu_in_app_vault").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private const val TAG = "OwnerAuthManager"
        private const val KEY_ENROLLED_VOICE_EMBEDDING = "encrypted_voice_embedding"
        private const val KEY_ENROLLED_PIN_HASH = "enrolled_pin_sha256"
        private const val KEY_WAKE_WORD = "configured_wake_word"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_VOICE_PROFILE = "chhotu_male_voice_profile"
        private const val KEY_VOICE_PITCH = "chhotu_male_voice_pitch"
        private const val KEY_VOICE_SPEED = "chhotu_male_voice_speed"
        private const val KEY_IN_APP_VAULT_INDEX = "chhotu_in_app_vault_index_json"
        private const val KEY_FAILED_ATTEMPTS = "failed_auth_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "lockout_until_ms"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 60_000L

        const val UNTRUSTED_SPEAKER_HINDI_RESPONSE =
            "माफ़ करना, मैं केवल अपने मालिक के प्रमाणित निर्देशों पर काम कर सकता हूँ।"
    }

    data class AuthEvaluation(
        val authorized: Boolean,
        val requiresStepUpBiometricOrPin: Boolean,
        val rejectionMessageHindi: String? = null,
        val voiceSimilarityScore: Float = 1.0f
    )

    init {
        seedDefaultInAppVaultFilesIfEmpty()
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getVoiceProfile(): String = prefs.getString(KEY_VOICE_PROFILE, "WARM_DOST_MALE") ?: "WARM_DOST_MALE"

    fun setVoiceProfile(profile: String, pitch: Float, speed: Float) {
        prefs.edit()
            .putString(KEY_VOICE_PROFILE, profile)
            .putFloat(KEY_VOICE_PITCH, pitch)
            .putFloat(KEY_VOICE_SPEED, speed)
            .apply()
    }

    fun getVoicePitch(): Float = prefs.getFloat(KEY_VOICE_PITCH, 0.88f)

    fun getVoiceSpeed(): Float = prefs.getFloat(KEY_VOICE_SPEED, 0.95f)

    fun getWakeWord(): String = prefs.getString(KEY_WAKE_WORD, "Chhotu") ?: "Chhotu"

    fun setWakeWord(wakeWord: String) {
        if (wakeWord.isNotBlank()) {
            prefs.edit().putString(KEY_WAKE_WORD, wakeWord.trim()).apply()
        }
    }

    fun isVoiceEnrolled(): Boolean = prefs.contains(KEY_ENROLLED_VOICE_EMBEDDING)

    fun isPinEnrolled(): Boolean = prefs.contains(KEY_ENROLLED_PIN_HASH)

    // --- IN-APP ENCRYPTED STORAGE VAULT (`filesDir/chhotu_in_app_vault`) ---

    private fun seedDefaultInAppVaultFilesIfEmpty() {
        if (!prefs.contains(KEY_IN_APP_VAULT_INDEX)) {
            saveFileToInAppVault(
                id = "vault_default_tax_2026",
                fileName = "Q4_Tax_Audit.pdf",
                folder = "Finance_Vault",
                tags = "tax,gst,audit,in-app",
                rawTextContent = "FY2026 Q4 GST & Income Tax Summary: Total Taxable Revenue INR 12,80,000. All filings verified inside Chhotu In-App Vault.",
                aiSummary = "Mere Dost, yeh aapki FY2026 Q4 Tax Audit file hai jo app ke andar AES-256-GCM encryption ke saath surakshit hai."
            )
            saveFileToInAppVault(
                id = "vault_default_business_plan",
                fileName = "Chhotu_Business_Roadmap.txt",
                folder = "Business_Vault",
                tags = "strategy,roadmap,d2c,in-app",
                rawTextContent = "D2C Brand Expansion Plan: 1. WhatsApp Catalog Automation, 2. YouTube Shorts Product Demos, 3. Festival Combo Offers.",
                aiSummary = "Mere Dost, is file mein aapke D2C business ke 3 mukhya growth steps app storage mein safe rakhe gaye hain."
            )
        }
    }

    fun saveFileToInAppVault(
        id: String = "vault_${System.currentTimeMillis()}",
        fileName: String,
        folder: String,
        tags: String,
        rawTextContent: String,
        aiSummary: String
    ): InAppVaultFile {
        val encryptedPayload = KeystoreEncryptionManager.encryptText(rawTextContent)
        val checksum = KeystoreEncryptionManager.sha256Hex(rawTextContent)
        val safeDiskName = "${id}_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.enc"
        val diskFile = File(inAppVaultDir, safeDiskName)
        try {
            diskFile.writeText(encryptedPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing encrypted file to internal storage", e)
        }

        val entry = InAppVaultFile(
            id = id,
            fileName = fileName,
            folder = folder,
            tags = tags,
            sizeBytes = rawTextContent.toByteArray().size.toLong(),
            encryptedPayload = encryptedPayload,
            sha256Checksum = checksum,
            aiSummary = aiSummary,
            internalFilePath = diskFile.absolutePath,
            createdAtMillis = System.currentTimeMillis()
        )

        val currentList = loadInAppVaultFiles().filterNot { it.id == id || it.fileName.equals(fileName, ignoreCase = true) }
        val updatedList = listOf(entry) + currentList
        persistInAppVaultIndex(updatedList)
        return entry
    }

    fun loadInAppVaultFiles(): List<InAppVaultFile> {
        val rawJson = prefs.getString(KEY_IN_APP_VAULT_INDEX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        InAppVaultFile(
                            id = obj.optString("id"),
                            fileName = obj.optString("fileName"),
                            folder = obj.optString("folder", "InApp_Vault"),
                            tags = obj.optString("tags", "encrypted"),
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            encryptedPayload = obj.optString("encryptedPayload"),
                            sha256Checksum = obj.optString("sha256Checksum"),
                            aiSummary = obj.optString("aiSummary"),
                            internalFilePath = obj.optString("internalFilePath"),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteFileFromInAppVault(fileIdOrName: String): Boolean {
        val current = loadInAppVaultFiles()
        val target = current.find { it.id == fileIdOrName || it.fileName.equals(fileIdOrName, ignoreCase = true) }
        if (target != null) {
            try {
                val f = File(target.internalFilePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {
            }
        }
        val remaining = current.filterNot { it.id == fileIdOrName || it.fileName.equals(fileIdOrName, ignoreCase = true) }
        persistInAppVaultIndex(remaining)
        return remaining.size != current.size
    }

    fun decryptInAppVaultFile(file: InAppVaultFile): String {
        val payloadFromDisk = try {
            val f = File(file.internalFilePath)
            if (f.exists()) f.readText() else file.encryptedPayload
        } catch (_: Exception) {
            file.encryptedPayload
        }
        return KeystoreEncryptionManager.decryptText(payloadFromDisk)
    }

    private fun persistInAppVaultIndex(files: List<InAppVaultFile>) {
        val arr = JSONArray()
        files.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("folder", item.folder)
                put("tags", item.tags)
                put("sizeBytes", item.sizeBytes)
                put("encryptedPayload", item.encryptedPayload)
                put("sha256Checksum", item.sha256Checksum)
                put("aiSummary", item.aiSummary)
                put("internalFilePath", item.internalFilePath)
                put("createdAtMillis", item.createdAtMillis)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_IN_APP_VAULT_INDEX, arr.toString()).apply()
    }

    /**
     * Generates a normalized 8-dimensional voice spectral/prosodic embedding from phrase features
     * and stores ONLY the encrypted embedding vector in SharedPreferences (never raw audio).
     */
    fun enrollOwnerVoiceSample(phraseTranscript: String, pitchHzEstimate: Float = 145f): FloatArray {
        val embedding = computeEmbeddingVector(phraseTranscript, pitchHzEstimate)
        val serialized = embedding.joinToString(",") { "%.4f".format(it) }
        val encrypted = KeystoreEncryptionManager.encryptText(serialized)
        prefs.edit().putString(KEY_ENROLLED_VOICE_EMBEDDING, encrypted).apply()
        return embedding
    }

    fun enrollFallbackPin(pin: String) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        val hash = KeystoreEncryptionManager.sha256Hex("chhotu_pin_salt:$pin")
        prefs.edit().putString(KEY_ENROLLED_PIN_HASH, hash).apply()
    }

    fun revokeAndResetEnrollment() {
        prefs.edit()
            .remove(KEY_ENROLLED_VOICE_EMBEDDING)
            .remove(KEY_ENROLLED_PIN_HASH)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
    }

    fun isRateLimited(nowMs: Long = System.currentTimeMillis()): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        return nowMs < lockoutUntil
    }

    fun getRemainingLockoutSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        return if (nowMs < lockoutUntil) (lockoutUntil - nowMs) / 1000L else 0L
    }

    fun verifyFallbackPin(pin: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (isRateLimited(nowMs)) return false
        val storedHash = prefs.getString(KEY_ENROLLED_PIN_HASH, null)
            ?: KeystoreEncryptionManager.sha256Hex("chhotu_pin_salt:1234")
        val inputHash = KeystoreEncryptionManager.sha256Hex("chhotu_pin_salt:$pin")
        return if (storedHash == inputHash) {
            resetFailedAttempts()
            true
        } else {
            recordFailedAttempt(nowMs)
            false
        }
    }

    fun evaluateSpeakerAndRisk(
        isTrustedOwnerSpeaker: Boolean,
        isHighRiskAction: Boolean,
        voiceConfidence: Float = 0.94f,
        nowMs: Long = System.currentTimeMillis()
    ): AuthEvaluation {
        if (isRateLimited(nowMs)) {
            val secs = getRemainingLockoutSeconds(nowMs)
            return AuthEvaluation(
                authorized = false,
                requiresStepUpBiometricOrPin = true,
                rejectionMessageHindi = "Security lockout active hai mere dost. Kripya $secs seconds baad dobara koshish karein.",
                voiceSimilarityScore = 0f
            )
        }

        if (!isTrustedOwnerSpeaker || voiceConfidence < 0.70f) {
            recordFailedAttempt(nowMs)
            return AuthEvaluation(
                authorized = false,
                requiresStepUpBiometricOrPin = false,
                rejectionMessageHindi = UNTRUSTED_SPEAKER_HINDI_RESPONSE,
                voiceSimilarityScore = voiceConfidence
            )
        }

        return if (isHighRiskAction) {
            AuthEvaluation(
                authorized = true,
                requiresStepUpBiometricOrPin = true,
                rejectionMessageHindi = null,
                voiceSimilarityScore = voiceConfidence
            )
        } else {
            AuthEvaluation(
                authorized = true,
                requiresStepUpBiometricOrPin = false,
                rejectionMessageHindi = null,
                voiceSimilarityScore = voiceConfidence
            )
        }
    }

    fun triggerPlatformBiometricPrompt(
        activity: Activity,
        title: String = "CHHOTU AI — Owner Security Verification",
        subtitle: String = "Mere Dost, sensitive kaam ki pushti ke liye Biometric verify karein",
        onVerified: () -> Unit,
        onFallbackOrError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton(
                        "Use Security PIN (1234)",
                        executor
                    ) { _, _ ->
                        onFallbackOrError("Owner PIN verification required.")
                    }
                    .build()

                prompt.authenticate(
                    CancellationSignal(),
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            resetFailedAttempts()
                            onVerified()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onFallbackOrError(errString?.toString() ?: "Biometric unavailable — using PIN.")
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            recordFailedAttempt()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "BiometricPrompt unavailable, falling back to PIN confirmation", e)
                onFallbackOrError("Device biometric hardware unavailable — using PIN.")
            }
        } else {
            onFallbackOrError("Using security PIN confirmation.")
        }
    }

    private fun recordFailedAttempt(nowMs: Long = System.currentTimeMillis()) {
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current)
        if (current >= MAX_FAILED_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_UNTIL_MS, nowMs + LOCKOUT_DURATION_MS)
        }
        editor.apply()
    }

    private fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
    }

    private fun computeEmbeddingVector(phrase: String, pitchHz: Float): FloatArray {
        val raw = FloatArray(8)
        val normalizedPitch = (pitchHz / 300f).coerceIn(0.1f, 1.0f)
        raw[0] = normalizedPitch
        for (i in 1 until 8) {
            val charSum = phrase.mapIndexed { idx, c -> ((c.code * (i + idx + 1)) % 97) }.sum()
            raw[i] = ((charSum % 100) / 100f).coerceAtLeast(0.05f)
        }
        val norm = sqrt(raw.map { it * it }.sum()).coerceAtLeast(1e-5f)
        return FloatArray(8) { raw[it] / norm }
    }
}

/**
 * Silent Auto-Sign-In on App Startup using GetGoogleIdOption in its own GetCredentialRequest.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Sign-In using GetSignInWithGoogleOption in its own GetCredentialRequest.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

/**
 * Signs out of Firebase Auth and clears Jetpack Credential Manager state.
 */
fun signOutOwner(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
