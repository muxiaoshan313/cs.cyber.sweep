package cs.cyber.sweep.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import cs.cyber.sweep.StoragePermissions

class PermissionManager(private val activity: AppCompatActivity) {

    private var permissionCallback: PermissionCallback? = null

    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                permissionCallback?.onPermissionGranted()
            } else {
                permissionCallback?.onPermissionDenied()
            }
        }

    fun setPermissionCallback(callback: PermissionCallback) {
        this.permissionCallback = callback
    }

    fun hasStoragePermissions(): Boolean = StoragePermissions.hasAll(activity)

    fun requestStoragePermissions() {
        if (StoragePermissions.hasAll(activity)) {
            permissionCallback?.onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(StoragePermissions.permissionsToRequest())
        }
    }

    fun showAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}