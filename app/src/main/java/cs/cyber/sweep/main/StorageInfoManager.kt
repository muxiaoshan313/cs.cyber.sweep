package cs.cyber.sweep.main

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class StorageInfoManager(private val context: Context) {

    private val decimalFormat = DecimalFormat("#.#")
    private val _storageInfoFlow = MutableStateFlow(getDefaultStorageInfo())
    val storageInfoFlow: StateFlow<StorageInfo> = _storageInfoFlow.asStateFlow()

    suspend fun refreshStorageInfo() {
        withContext(Dispatchers.IO) {
            try {
                val totalSpace = getTotalStorageSpace()
                val availableSpace = getAvailableStorageSpace()
                val usedSpace = totalSpace - availableSpace

                val storageInfo = createStorageInfo(availableSpace, usedSpace, totalSpace)
                _storageInfoFlow.value = storageInfo

            } catch (e: Exception) {
                e.printStackTrace()
                handleStorageInfoError()
            }
        }
    }

    private fun getTotalStorageSpace(): Long {
        return try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT
            storageStatsManager.getTotalBytes(uuid)
        } catch (e: Exception) {
            e.printStackTrace()
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.blockCountLong * stat.blockSizeLong
        }
    }

    private fun getAvailableStorageSpace(): Long {
        return try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT
            storageStatsManager.getFreeBytes(uuid)
        } catch (e: Exception) {
            e.printStackTrace()
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        }
    }

    private fun createStorageInfo(availableSpace: Long, usedSpace: Long, totalSpace: Long): StorageInfo {
        val freeSpace = formatStorage(availableSpace)
        val usedSpaceFormatted = formatStorage(usedSpace)
        val usagePercentage = if (totalSpace > 0) {
            ((usedSpace.toDouble() / totalSpace.toDouble()) * 100).toInt()
        } else {
            0
        }

        return StorageInfo(
            freeSpace = freeSpace,
            usedSpace = usedSpaceFormatted,
            usagePercentage = usagePercentage
        )
    }

    private fun formatStorage(bytes: Long): FormattedStorage {
        return when {
            bytes >= 1000 * 1000 * 1000 -> {
                val gb = bytes.toDouble() / (1000 * 1000 * 1000)
                FormattedStorage(decimalFormat.format(gb), "GB")
            }
            bytes >= 1000 * 1000 -> {
                val mb = bytes.toDouble() / (1000 * 1000)
                FormattedStorage(decimalFormat.format(mb), "MB")
            }
            else -> {
                val kb = bytes.toDouble() / 1000
                FormattedStorage(decimalFormat.format(kb), "KB")
            }
        }
    }

    private fun handleStorageInfoError() {
        try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val totalSpace = stat.blockCountLong * stat.blockSizeLong
            val availableSpace = stat.availableBlocksLong * stat.blockSizeLong
            val usedSpace = totalSpace - availableSpace

            val storageInfo = createStorageInfo(availableSpace, usedSpace, totalSpace)
            _storageInfoFlow.value = storageInfo
        } catch (e2: Exception) {
            e2.printStackTrace()
            _storageInfoFlow.value = getDefaultStorageInfo()
        }
    }

    private fun getDefaultStorageInfo(): StorageInfo {
        return StorageInfo(
            freeSpace = FormattedStorage("0", "GB"),
            usedSpace = FormattedStorage("0", "GB"),
            usagePercentage = 0
        )
    }
}