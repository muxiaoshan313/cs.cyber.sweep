package cs.cyber.sweep.main

import kotlinx.coroutines.flow.Flow

interface MainContract {

    interface View {
        fun updateStorageDisplay(storageInfo: StorageInfo)
        fun showPermissionDialog()
        fun hidePermissionDialog()
        fun navigateToCleanActivity()
        fun navigateToPicCleanActivity()
        fun navigateToFileCleanActivity()
        fun navigateToSettings()
    }

    interface Presenter {
        fun onCleanClicked()
        fun onPictureCleanClicked()
        fun onFileCleanClicked()
        fun onSettingsClicked()
        fun onDialogConfirmed()
        fun onDialogCancelled()
        fun onResume()
        fun onDestroy()
        fun getStorageInfoFlow(): Flow<StorageInfo>
    }
}