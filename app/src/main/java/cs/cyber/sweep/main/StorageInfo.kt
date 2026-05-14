package cs.cyber.sweep.main

data class StorageInfo(
    val freeSpace: FormattedStorage,
    val usedSpace: FormattedStorage,
    val usagePercentage: Int
)

data class FormattedStorage(
    val size: String,
    val unit: String
)

enum class CleanAction {
    CLEAN,
    PICTURE_CLEAN,
    FILE_CLEAN
}

interface PermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied()
}