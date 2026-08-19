package moe.shizuku.manager.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class DeviceAuthentication(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: () -> Unit,
) {

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError()
            }
        },
    )

    @Suppress("DEPRECATION")
    fun authenticate(title: CharSequence) {
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle(title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            builder.setDeviceCredentialAllowed(true)
        }
        prompt.authenticate(builder.build())
    }

    companion object {
        fun isAvailable(context: Context): Boolean {
            return context.getSystemService(KeyguardManager::class.java).isDeviceSecure
        }
    }
}
