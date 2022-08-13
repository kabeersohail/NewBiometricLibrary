package com.example.authenticator

import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.authenticator.utils.Constants.NOTE_5_PRO

class Authenticator(private val fragment: Fragment) {

    private val biometricManager: BiometricManager = BiometricManager.from(fragment.requireContext())
    private val keyguardManager: KeyguardManager = fragment.requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    init {
        justCheckIfCanAuthenticateAndShowToastAndLog(BIOMETRIC_WEAK)
        justCheckIfCanAuthenticateAndShowToastAndLog(BIOMETRIC_STRONG)
        justCheckIfCanAuthenticateAndShowToastAndLog(DEVICE_CREDENTIAL)
    }

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock screen")
        .setAllowedAuthenticators(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    private val onBiometricEnrollmentResult = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        showToastAndLog("onBiometricEnrollmentResult -> ${it.resultCode}")
        if(biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS){
            showToastAndLog("BIOMETRIC_WEAK == SUCCESS")
            Handler(Looper.getMainLooper()).post {
                authenticate()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val onsetNewPassword = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK || it.resultCode == Activity.RESULT_FIRST_USER){
            val fingerprintManager: FingerprintManager = fragment.requireActivity().getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
            if(!fingerprintManager.hasEnrolledFingerprints()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    fingerprintEnroll()
                }
            }
            showToastAndLog("onSetNewPassword success")
        } else if(it.resultCode == Activity.RESULT_CANCELED){
            showToastAndLog("onsetNewPassword cancelled")
        }
    }

    private val onFingerprintEnrollResult = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            showToastAndLog("onFingerprintEnrollResult")
        } else {
            showToastAndLog("onFingerprintEnrollResult cancelled")
        }
    }

    private val biometricAuthCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            showToastAndLog("Error $errorCode $errString")
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            showToastAndLog("Failed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            showToastAndLog("Success ${result.authenticationType}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun nameYetToDecide(){
        when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showToastAndLog("BIOMETRIC_SUCCESS")
                authenticate()
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                showToastAndLog("BIOMETRIC_STATUS_UNKNOWN")

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                    authenticate()
                } else {
                    showToastAndLog("Status unknown")
                }
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                showToastAndLog("BIOMETRIC_ERROR_UNSUPPORTED")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToastAndLog("BIOMETRIC_ERROR_HW_UNAVAILABLE")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToastAndLog("BIOMETRIC_ERROR_NONE_ENROLLED")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    possibilitiesForAPI30()
                } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                    possibilitiesForAPI28()
                } else {
                    possibilitiesForAPI24()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToastAndLog("BIOMETRIC_ERROR_NO_HARDWARE")
                if(!keyguardManager.isDeviceSecure) {
                    launchSetNewPasswordIntent()
                } else {
                    authenticate()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                showToastAndLog("BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
            }
        }
    }

    private fun authenticate() {
        showToastAndLog("authenticate()")
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val biometricPrompt = BiometricPrompt(fragment.requireActivity(), executor, biometricAuthCallback)
        biometricPrompt.authenticate(promptInfo)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun possibilitiesForAPI30() {
        showToastAndLog("possibilitiesForAPI30()")
        val biometricEnrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
        showToastAndLog("Launching ACTION_BIOMETRIC_ENROLL")
        onBiometricEnrollmentResult.launch(biometricEnrollIntent)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun possibilitiesForAPI28() {
        showToastAndLog("possibilitiesForAPI28()")
        showToastAndLog("ACTION_BIOMETRIC_ENROLL not supported on API ${Build.VERSION.SDK_INT} , supports from API 30 ")

        if(Build.MODEL == NOTE_5_PRO){
            launchSetNewPasswordIntent()
        } else {
            fingerprintEnroll()
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun possibilitiesForAPI24() {
        showToastAndLog("possibilitiesForAPI24()")
        showToastAndLog("ACTION_FINGERPRINT_ENROLL not supported on API ${Build.VERSION.SDK_INT} , supports from API 28")

        val fingerprintManager: FingerprintManager = fragment.requireActivity().getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        if(fingerprintManager.isHardwareDetected){

            showToastAndLog("Hardware detected")

            if(fingerprintManager.hasEnrolledFingerprints()){
                showToastAndLog("Yep")
            } else {
                showToastAndLog("Nope")
                launchSetNewPasswordIntent()
            }
        }
    }

    // Added in 28
    @RequiresApi(Build.VERSION_CODES.P)
    private fun fingerprintEnroll() {
        val fingerprintEnrollIntent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
        showToastAndLog("Launching ACTION_FINGERPRINT_ENROLL")
        onFingerprintEnrollResult.launch(fingerprintEnrollIntent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun launchSetNewPasswordIntent() {
        showToastAndLog("Launching set new password intent")
        val setNewPasswordIntent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
        onsetNewPassword.launch(setNewPasswordIntent)
    }

    private fun showToastAndLog(message: String) {
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.d("SOHAIL", message)
    }

    private fun justCheckIfCanAuthenticateAndShowToastAndLog(authenticatorType: Int) {
        when (biometricManager.canAuthenticate(authenticatorType)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showToastAndLog("BIOMETRIC_SUCCESS")
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                showToastAndLog("BIOMETRIC_STATUS_UNKNOWN")
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                showToastAndLog("BIOMETRIC_ERROR_UNSUPPORTED")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToastAndLog("BIOMETRIC_ERROR_HW_UNAVAILABLE")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToastAndLog("BIOMETRIC_ERROR_NONE_ENROLLED")
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToastAndLog("BIOMETRIC_ERROR_NO_HARDWARE")
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                showToastAndLog("BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
            }
        }
    }


}