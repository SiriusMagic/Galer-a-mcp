package app.luma.gallery

import android.app.KeyguardManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** The vault opens only after the phone's own lock (fingerprint, face, PIN, pattern or password). */
object VaultLock {
    fun ask(activity: FragmentActivity, onOpen: () -> Unit, onNoLock: () -> Unit) {
        if (activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure != true) return onNoLock()
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOpen()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.vault))
            .setSubtitle(activity.getString(R.string.vault_unlock)).setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL).build())
    }
}
