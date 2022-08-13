package com.example.newbiometriclibrary.fragments

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.authenticator.Authenticator
import com.example.newbiometriclibrary.BuildConfig
import com.example.newbiometriclibrary.databinding.FragmentLaunchBinding
import com.example.newbiometriclibrary.states.DeviceState
import com.example.newbiometriclibrary.states.UninstallState

enum class UninstallTriggerReason {
    DEVICE_OWNER_CLEARED,
    SECURITY_EXCEPTION_WHILE_CLEARING_DEVICE_OWNER,
    USER_CLICKED_ON_CANCEL
}

class LaunchFragment : Fragment() {

    private lateinit var binding: FragmentLaunchBinding
    private lateinit var authenticator: Authenticator

    private val deviceState = DeviceState()

    /**
     * This broadcast receiver triggers uninstall pop when Device owner is cleared.
     *
     * Note: read about [ACTION_DEVICE_OWNER_CHANGED] broadcast action for more information
     * on when this broadcast receiver will be triggered.
     */
    private var onDeviceOwnerCleared = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DEVICE_OWNER_CHANGED && deviceState.uninstallState == UninstallState.CommandIssued) {
                println("SOHAIL broadcast action triggered by context registered broadcast")
                triggerUninstallPopup(UninstallTriggerReason.DEVICE_OWNER_CLEARED)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        authenticator = Authenticator(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.authenticate.setOnClickListener {
            // Setting scenario
            deviceState.uninstallState = UninstallState.CommandIssued
            removeDeviceOwnerIfExistsOrElseDisplayUninstallPopup()
        }
    }

    /**
     * This method does the following:
     *
     * case 1: When device owner exists
     * -> Removes device owner and notifies [onDeviceOwnerCleared] broadcast receiver, which will display uninstall popup dialog
     *
     * case 2: When device owner doesn't exist
     * -> Displays uninstall pop up dialog directly
     *
     */
    private fun removeDeviceOwnerIfExistsOrElseDisplayUninstallPopup() = try {
        val mDevicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mDevicePolicyManager.clearDeviceOwnerApp(BuildConfig.APPLICATION_ID)
        println("SOHAIL Tried to remove device owner")
    } catch (e: SecurityException) {
        println("SOHAIL Exception while clearing device owner, triggering uninstall -> ${e.message}")
        triggerUninstallPopup(UninstallTriggerReason.SECURITY_EXCEPTION_WHILE_CLEARING_DEVICE_OWNER)
    }

    private fun triggerUninstallPopup(reason: UninstallTriggerReason) {
        println("SOHAIL Displaying uninstall popup -> $reason")
        val packageUri = Uri.parse("package:com.example.newbiometriclibrary")
        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        Log.d(tag, "UninstallSequence click ok")
        startActivityForResult(uninstallIntent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                println("SOHAIL ok clicked")
                requireActivity().finish()
            } else {
//                println( "SOHAIL cancelled")
//                triggerUninstallPopup(UninstallTriggerReason.USER_CLICKED_ON_CANCEL)
            }
        }
    }
}