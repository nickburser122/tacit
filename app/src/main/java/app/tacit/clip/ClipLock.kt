package app.tacit.clip

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import app.tacit.Prefs
import app.tacit.R
import app.tacit.core.ItemKind
import app.tacit.core.SearchItem

class ClipLock(private val activity: Activity, private val prefs: Prefs, private val onUnlocked: () -> Unit) {

    private var unlocked = false
    private var prompting = false
    private var cancel: CancellationSignal? = null

    fun isLocked(): Boolean = prefs.clipBiometric && !unlocked && canAuthenticate(activity)

    fun lock() {
        unlocked = false
        cancel?.cancel()
        cancel = null
        prompting = false
    }

    fun lockedRow(): SearchItem =
        SearchItem(LOCKED_KEY, ItemKind.ACTION, activity.getString(R.string.clip_locked), activity.getString(R.string.clip_locked_hint),
            payload = UNLOCK_PAYLOAD)

    fun requestUnlock() {
        if (prompting || !isLocked()) return
        prompting = true
        val signal = CancellationSignal()
        cancel = signal
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.clip_unlock_title))
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(AUTHENTICATORS)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        val prompt = builder.build()
        prompt.authenticate(signal, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                prompting = false
                unlocked = true
                onUnlocked()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                prompting = false
            }
        })
    }

    companion object {
        const val LOCKED_KEY = "clip:locked"
        const val UNLOCK_PAYLOAD = "unlock|clipboard"

        private val AUTHENTICATORS: Int
            get() = if (Build.VERSION.SDK_INT >= 30) {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else 0

        fun canAuthenticate(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT >= 30) {
                val manager = activity.getSystemService(BiometricManager::class.java) ?: return false
                return manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
            }
            val keyguard = activity.getSystemService(KeyguardManager::class.java) ?: return false
            return keyguard.isDeviceSecure
        }
    }
}
