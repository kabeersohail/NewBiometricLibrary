package com.example.newbiometriclibrary.fragments

import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD
import android.content.ComponentName
import android.content.Context
import android.content.Context.FINGERPRINT_SERVICE
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.ACTION_FINGERPRINT_ENROLL
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.newbiometriclibrary.databinding.FragmentLaunchBinding


class LaunchFragment : Fragment() {

    private lateinit var binding: FragmentLaunchBinding
    private lateinit var biometricManager: BiometricManager
    private lateinit var keyguardManager: KeyguardManager


    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock screen")
        .setAllowedAuthenticators(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    private val onBiometricEnrollmentResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        showToastAndLog("onBiometricEnrollmentResult -> ${it.resultCode}")
        if(biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS){
            showToastAndLog("BIOMETRIC_WEAK == SUCCESS")
            Handler(Looper.getMainLooper()).post {
                authenticate()
            }
        }
    }

    private val onFingerprintEnrollResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            showToastAndLog("onFingerprintEnrollResult")
        } else {
            showToastAndLog("onFingerprintEnrollResult cancelled")
        }
    }

    private val onsetNewPassword = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK || it.resultCode == Activity.RESULT_FIRST_USER){
            showToastAndLog("onSetNewPassword success")
        } else if(it.resultCode == Activity.RESULT_CANCELED){
            showToastAndLog("onsetNewPassword cancelled")
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricManager = BiometricManager.from(requireContext())
        keyguardManager = requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        justCheckIfCanAuthenticateAndShowToastAndLog(BIOMETRIC_WEAK)
        justCheckIfCanAuthenticateAndShowToastAndLog(BIOMETRIC_STRONG)
        justCheckIfCanAuthenticateAndShowToastAndLog(DEVICE_CREDENTIAL)

        binding.authenticate.setOnClickListener {

            when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    showToastAndLog("BIOMETRIC_SUCCESS")
                    authenticate()
                }
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    showToastAndLog("BIOMETRIC_STATUS_UNKNOWN")

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                        authenticate()
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
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    showToastAndLog("BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
                }
            }
        }
    }

    private fun possibilitiesForAPI30() {
        showToastAndLog("possibilitiesForAPI30()")
        val biometricEnrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
        showToastAndLog("Launching ACTION_BIOMETRIC_ENROLL")
        onBiometricEnrollmentResult.launch(biometricEnrollIntent)
    }

    private fun possibilitiesForAPI28() {
        showToastAndLog("possibilitiesForAPI28()")
        showToastAndLog("ACTION_BIOMETRIC_ENROLL not supported on API ${Build.VERSION.SDK_INT} , supports from API 30 ")

        val fingerprintEnrollIntent = Intent(ACTION_FINGERPRINT_ENROLL)
        showToastAndLog("Launching ACTION_FINGERPRINT_ENROLL")
        onFingerprintEnrollResult.launch(fingerprintEnrollIntent)
    }

    private fun possibilitiesForAPI24() {
        showToastAndLog("possibilitiesForAPI24()")
        showToastAndLog("ACTION_FINGERPRINT_ENROLL not supported on API ${Build.VERSION.SDK_INT} , supports from API 28")

        val fingerprintManager: FingerprintManager = requireActivity().getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
        if(fingerprintManager.isHardwareDetected){

            showToastAndLog("Hardware detected")

            if(fingerprintManager.hasEnrolledFingerprints()){
                showToastAndLog("Yep")
            } else {
                showToastAndLog("Nope")
                val setNewPasswordIntent = Intent(ACTION_SET_NEW_PASSWORD)
                onsetNewPassword.launch(setNewPasswordIntent)
            }
        }
    }

    private fun authenticate() {
        showToastAndLog("authenticate()")
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(requireActivity(), executor, biometricAuthCallback)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showToastAndLog(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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