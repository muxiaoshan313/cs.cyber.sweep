package cs.cyber.sweep.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainPresenter(
    private val view: MainContract.View,
    private val storageManager: StorageInfoManager,
    private val permissionManager: PermissionManager
) : MainContract.Presenter {

    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pendingAction: CleanAction? = null

    init {
        setupPermissionCallbacks()
        refreshStorageInfo()
    }

    private fun setupPermissionCallbacks() {
        permissionManager.setPermissionCallback(object : PermissionCallback {
            override fun onPermissionGranted() {
                view.hidePermissionDialog()
                executePendingAction()
            }

            override fun onPermissionDenied() {
                view.hidePermissionDialog()
                permissionManager.showAppSettings()
            }
        })
    }

    override fun onCleanClicked() {
        pendingAction = CleanAction.CLEAN
        checkPermissionAndExecute()
    }

    override fun onPictureCleanClicked() {
        pendingAction = CleanAction.PICTURE_CLEAN
        checkPermissionAndExecute()
    }

    override fun onFileCleanClicked() {
        pendingAction = CleanAction.FILE_CLEAN
        checkPermissionAndExecute()
    }

    override fun onSettingsClicked() {
        view.navigateToSettings()
    }

    override fun onDialogConfirmed() {
        view.hidePermissionDialog()
        permissionManager.requestStoragePermissions()
    }

    override fun onDialogCancelled() {
        view.hidePermissionDialog()
        pendingAction = null
    }

    override fun onResume() {
        refreshStorageInfo()
    }

    override fun onDestroy() {
        presenterScope.cancel()
    }

    override fun getStorageInfoFlow(): Flow<StorageInfo> {
        return storageManager.storageInfoFlow
    }

    private fun checkPermissionAndExecute() {
        if (permissionManager.hasStoragePermissions()) {
            executePendingAction()
        } else {
            view.showPermissionDialog()
        }
    }

    private fun executePendingAction() {
        when (pendingAction) {
            CleanAction.CLEAN -> view.navigateToCleanActivity()
            CleanAction.PICTURE_CLEAN -> view.navigateToPicCleanActivity()
            CleanAction.FILE_CLEAN -> view.navigateToFileCleanActivity()
            null -> return
        }
        pendingAction = null
    }

    private fun refreshStorageInfo() {
        presenterScope.launch {
            storageManager.refreshStorageInfo()
        }
    }
}