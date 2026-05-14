package cs.cyber.sweep

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cs.cyber.sweep.databinding.ActivityCleanBinding
import kotlinx.coroutines.*
import java.io.File
import java.text.DecimalFormat
import kotlin.random.Random

// 垃圾文件数据类
data class JunkFile(
    val name: String,
    val path: String,
    val size: Long,
    var isSelected: Boolean = true // 默认选中
) {
    // 重写equals和hashCode，基于path进行比较，避免重复
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as JunkFile
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

// 垃圾文件分类数据类
data class JunkCategory(
    val type: String,
    val files: MutableSet<JunkFile> = mutableSetOf(), // 改为Set避免重复
    var isExpanded: Boolean = false,
    var isSelected: Boolean = true, // 默认选中
    var displayOffset: Int = 0 // 用于分页显示
) {
    fun getTotalSize(): Long = files.sumOf { it.size }
    fun getSelectedSize(): Long = files.filter { it.isSelected }.sumOf { it.size }
    fun getFilesList(): List<JunkFile> = files.toList()
}

class CleanActivity : AppCompatActivity() {

    private val binding by lazy { ActivityCleanBinding.inflate(layoutInflater) }
    private val decimalFormat = DecimalFormat("#.#")
    private val handler = Handler(Looper.getMainLooper())
    private var scanJob: Job? = null

    // 垃圾分类
    private val junkCategories = mutableMapOf<String, JunkCategory>(
        "App Cache" to JunkCategory("App Cache"),
        "Apk Files" to JunkCategory("Apk Files"),
        "Log Files" to JunkCategory("Log Files"),
        "AD Junk" to JunkCategory("AD Junk"),
        "Temp Files" to JunkCategory("Temp Files")
    )

    private var totalJunkSize = 0L
    private var foundJunkFiles = false
    private val scannedPaths = mutableSetOf<String>() // 记录已扫描的路径，避免重复扫描

    // 每页显示的文件数量
    private val pageSize = 50

    private val filterStrArr = arrayOf(
        ".*(/|\\\\)logs(/|\\\\|$).*",
        ".*(/|\\\\)temp(/|\\\\|$).*",
        ".*(/|\\\\)temporary(/|\\\\|$).*",
        ".*(/|\\\\)supersonicads(/|\\\\|$).*",
        ".*(/|\\\\)cache(/|\\\\|$).*",
        ".*(/|\\\\)Analytics(/|\\\\|$).*",
        ".*(/|\\\\)thumbnails?(/|\\\\|$).*",
        ".*(/|\\\\)mobvista(/|\\\\|$).*",
        ".*(/|\\\\)UnityAdsVideoCache(/|\\\\|$).*",
        ".*(/|\\\\)albumthumbs?(/|\\\\|$).*",
        ".*(/|\\\\)LOST.DIR(/|\\\\|$).*",
        ".*(/|\\\\)\\.Trash(/|\\\\|$).*",
        ".*(/|\\\\)desktop.ini(/|\\\\|$).*",
        ".*(/|\\\\)leakcanary(/|\\\\|$).*",
        ".*(/|\\\\)\\.DS_Store(/|\\\\|$).*",
        ".*(/|\\\\)\\.spotlight-V100(/|\\\\|$).*",
        ".*(/|\\\\)fseventsd(/|\\\\|$).*",
        ".*(/|\\\\)Bugreport(/|\\\\|$).*",
        ".*(/|\\\\)bugreports(/|\\\\|$).*",
        ".*(/|\\\\)splashad(/|\\\\|$).*",
        ".*(/|\\\\)\\.nomedia(/|\\\\|$).*",
        ".*\\.xapk$",
        ".*\\.property$",
        ".*\\.dat$",
        ".*\\.cached$",
        ".*\\.logcat$",
        ".*\\.download$",
        ".*\\.part$",
        ".*\\.crdownload$",
        ".*\\.thumbnails$",
        ".*\\.thumbdata$",
        ".*\\.thumb$",
        ".*\\.crash$",
        ".*\\.error$",
        ".*\\.stacktrace$",
        ".*\\.bak$",
        ".*\\.backup$",
        ".*\\.old$",
        ".*\\.prev$",
        ".*\\.apks$",
        ".*\\.apkm$",
        ".*\\.idea$",
        ".*\\.iml$",
        ".*\\.classpath$",
        ".*\\.project$",
        ".*\\.webcache$",
        ".*\\.indexeddb$",
        ".*\\.localstorage$",
        ".*\\.tmp$",
        ".*\\.log$",
        ".*\\.temp$",
        ".*\\.logs$",
        ".*\\.cache$",
        ".*\\.apk$",
        ".*\\.exo$",
        ".*thumbs?\\.db$",
        ".*\\.thumb[0-9]$",
        ".*splashad$"
    )
    private val filterPatterns = filterStrArr.map { it.toRegex(RegexOption.IGNORE_CASE) }

    private var jumpJob: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clean)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        startScanning()
    }



    private fun initViews() {
        binding.textBack.setOnClickListener {
            finish()
        }
        binding.inClean.tvBack.setOnClickListener {
            jumpJob?.cancel()
            binding.inClean.load.isVisible = false
        }
        binding.butClean.setOnClickListener {
            cleanSelectedFiles()
        }
        setupCategoryClickListeners()
    }

    private fun setupCategoryClickListeners() {
        // App Cache 分类
        binding.layoutAppCache.setOnClickListener {
            toggleCategoryExpansion("App Cache", binding.ivAppCacheExpand)
        }
        binding.ivAppCacheStatus.setOnClickListener {
            toggleCategorySelection("App Cache", binding.ivAppCacheStatus)
        }

        // Apk Files 分类
        binding.layoutApkFiles.setOnClickListener {
            toggleCategoryExpansion("Apk Files", binding.ivApkExpand)
        }
        binding.ivAppFileStatus.setOnClickListener {
            toggleCategorySelection("Apk Files", binding.ivAppFileStatus)
        }

        // Log Files 分类
        binding.layoutLogFiles.setOnClickListener {
            toggleCategoryExpansion("Log Files", binding.ivLogExpand)
        }
        binding.ivLogFileStatus.setOnClickListener {
            toggleCategorySelection("Log Files", binding.ivLogFileStatus)
        }

        // AD Junk 分类
        binding.layoutAdJunk.setOnClickListener {
            toggleCategoryExpansion("AD Junk", binding.ivAdExpand)
        }
        binding.ivAdJunkStatus.setOnClickListener {
            toggleCategorySelection("AD Junk", binding.ivAdJunkStatus)
        }

        // Temp Files 分类
        binding.layoutTempFiles.setOnClickListener {
            toggleCategoryExpansion("Temp Files", binding.ivTempExpand)
        }
        binding.ivTempFilesStatus.setOnClickListener {
            toggleCategorySelection("Temp Files", binding.ivTempFilesStatus)
        }
    }

    private fun startScanning() {
        binding.linScan.visibility = View.VISIBLE
        binding.butClean.visibility = View.GONE

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                scanForJunkFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    completeScan()
                }
            }
        }
    }

    private suspend fun scanForJunkFiles() {
        val scanPaths = getScanPaths()

        for (rootPath in scanPaths) {
            if (rootPath.exists() && rootPath.canRead()) {
                try {
                    scanDirectory(rootPath, 0) // 限制扫描深度
                } catch (e: Exception) {
                    // 忽略无权限访问的目录
                    continue
                }
            }
        }
    }

    private fun getScanPaths(): List<File> {
        val paths = mutableListOf<File>()

        try {
            // 优先扫描可访问的目录
            Environment.getExternalStorageDirectory()?.let { paths.add(it) }
            paths.add(filesDir) // 应用内部存储目录
            externalCacheDir?.let { paths.add(it) } // 外部缓存目录

            // 添加常见的垃圾文件目录
            val commonJunkPaths = listOf(
                "/storage/emulated/0/Android/data",
                "/storage/emulated/0/Download",
                "/storage/emulated/0/.thumbnails",
                "/storage/emulated/0/Pictures/.thumbnails",
                "/storage/emulated/0/DCIM/.thumbnails",
                "/storage/emulated/0/Temp",
                "/storage/emulated/0/Temporary"
            )

            commonJunkPaths.forEach { pathString ->
                val file = File(pathString)
                if (file.exists() && file.canRead()) {
                    paths.add(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paths
    }

    private suspend fun scanDirectory(directory: File, depth: Int) {
        if (depth > 6) return

        val directoryPath = directory.absolutePath
        // 避免重复扫描同一个目录
        if (scannedPaths.contains(directoryPath)) return
        scannedPaths.add(directoryPath)

        try {
            val files = directory.listFiles() ?: return

            for (file in files.take(200)) {
                if (scanJob?.isCancelled == true) return

                withContext(Dispatchers.Main) {
                    binding.tvFile.text = "Scanning: ${file.absolutePath}"
                }

                if (file.isDirectory) {
                    scanDirectory(file, depth + 1)
                } else {
                    checkAndAddJunkFile(file)
                }

                delay(5)
            }
        } catch (e: Exception) {
            // 忽略权限或访问错误
        }
    }

    private suspend fun checkAndAddJunkFile(file: File) {
        val fileName = file.name
        val filePath = file.absolutePath
        val fileSize = file.length()

        // 跳过太小的文件
        if (fileSize < 512) return // 减小最小文件大小限制到512字节

        // 使用新的过滤规则检查是否为垃圾文件
        val isJunkFile = filterPatterns.any { pattern ->
            pattern.matches(filePath.replace("\\", "/")) ||
                    pattern.matches(fileName) ||
                    pattern.matches("/" + fileName) ||
                    pattern.matches(filePath.replace("\\", "/") + "/")
        }

        if (!isJunkFile) return

        val junkFile = JunkFile(file.name, file.absolutePath, fileSize)

        // 根据文件路径特征分类垃圾文件
        when {
            // App Cache 文件
            filePath.contains("cache", true) || fileName.endsWith(".cache", true) ||
                    filePath.contains("webview", true) || fileName.endsWith(".db-wal", true) ||
                    fileName.endsWith(".db-shm", true) || fileName.endsWith(".cached", true) ||
                    fileName.endsWith(".webcache", true) || fileName.endsWith(".indexeddb", true) ||
                    fileName.endsWith(".localstorage", true) -> {
                junkCategories["App Cache"]?.files?.add(junkFile)
            }

            // APK 文件
            fileName.endsWith(".apk", true) || fileName.endsWith(".apks", true) ||
                    fileName.endsWith(".apkm", true) || fileName.endsWith(".xapk", true) -> {
                junkCategories["Apk Files"]?.files?.add(junkFile)
            }

            // 日志文件
            fileName.endsWith(".log", true) || fileName.endsWith(".logs", true) ||
                    fileName.endsWith(".logcat", true) || fileName.endsWith(".crash", true) ||
                    fileName.endsWith(".error", true) || fileName.endsWith(".stacktrace", true) ||
                    fileName.endsWith(".trace", true) -> {
                junkCategories["Log Files"]?.files?.add(junkFile)
            }

            // 广告垃圾文件
            filePath.contains("ad", true) || filePath.contains("ads", true) ||
                    filePath.contains("supersonicads", true) || filePath.contains("mobvista", true) ||
                    filePath.contains("UnityAdsVideoCache", true) || filePath.contains("splashad", true) ||
                    fileName.endsWith("splashad", true) || filePath.contains("Analytics", true) ||
                    filePath.contains("bugreport", true) || filePath.contains("bugreports", true) -> {
                junkCategories["AD Junk"]?.files?.add(junkFile)
            }

            // 临时文件
            fileName.endsWith(".tmp", true) || fileName.endsWith(".temp", true) ||
                    fileName.endsWith(".part", true) || fileName.endsWith(".crdownload", true) ||
                    fileName.endsWith(".download", true) || fileName.endsWith(".bak", true) ||
                    fileName.endsWith(".backup", true) || fileName.endsWith(".old", true) ||
                    fileName.endsWith(".prev", true) || fileName.endsWith(".exo", true) ||
                    fileName.endsWith(".thumbs.db", true) || fileName.endsWith(".thumbdata", true) ||
                    fileName.endsWith(".thumb", true) || fileName.endsWith(".dat", true) ||
                    fileName.endsWith(".property", true) -> {
                junkCategories["Temp Files"]?.files?.add(junkFile)
            }

            // 默认分类为临时文件
            else -> {
                junkCategories["Temp Files"]?.files?.add(junkFile)
            }
        }

        // 更新总大小和UI（由于使用Set，重复文件不会被重复计算）
        totalJunkSize += fileSize
        if (!foundJunkFiles && totalJunkSize > 0) {
            foundJunkFiles = true
            withContext(Dispatchers.Main) {
                // 更换背景为垃圾背景
                binding.imgBg.setImageResource(R.drawable.bj_junk)
            }
        }

        withContext(Dispatchers.Main) {
            updateScanProgress()
        }
    }

    private fun updateScanProgress() {
        // 重新计算总大小，避免重复计算
        totalJunkSize = junkCategories.values.sumOf { it.getTotalSize() }
        val (size, unit) = formatStorage(totalJunkSize)
        binding.tvProNum.text = size
        binding.tvUnit.text = unit
    }

    private fun completeScan() {
        binding.linScan.visibility = View.GONE
        // 不再直接显示按钮，而是根据是否有选中的文件来决定是否显示
        updateCleanButtonVisibility()

        // 更新各分类的大小显示和状态
        updateCategorySizes()
        updateCategoryStatus()
    }

    private fun updateCategorySizes() {
        // 更新App Cache大小
        val appCacheSize = junkCategories["App Cache"]?.getTotalSize() ?: 0
        val (cacheSize, cacheUnit) = formatStorage(appCacheSize)
        binding.tvAppCacheSize.text = "$cacheSize$cacheUnit"

        // 更新APK文件大小
        val apkSize = junkCategories["Apk Files"]?.getTotalSize() ?: 0
        val (apkSizeText, apkUnit) = formatStorage(apkSize)
        binding.tvApkSize.text = "$apkSizeText$apkUnit"

        // 更新日志文件大小
        val logSize = junkCategories["Log Files"]?.getTotalSize() ?: 0
        val (logSizeText, logUnit) = formatStorage(logSize)
        binding.tvLogSize.text = "$logSizeText$logUnit"

        // 更新广告垃圾大小
        val adSize = junkCategories["AD Junk"]?.getTotalSize() ?: 0
        val (adSizeText, adUnit) = formatStorage(adSize)
        binding.tvAdSize.text = "$adSizeText$adUnit"

        // 更新临时文件大小
        val tempSize = junkCategories["Temp Files"]?.getTotalSize() ?: 0
        val (tempSizeText, tempUnit) = formatStorage(tempSize)
        binding.tvTempSize.text = "$tempSizeText$tempUnit"
    }

    private fun updateCategoryStatus() {
        // 更新所有分类的选择状态图标
        binding.ivAppCacheStatus.setImageResource(R.drawable.ic_selete)
        binding.ivAppFileStatus.setImageResource(R.drawable.ic_selete)
        binding.ivLogFileStatus.setImageResource(R.drawable.ic_selete)
        binding.ivAdJunkStatus.setImageResource(R.drawable.ic_selete)
        binding.ivTempFilesStatus.setImageResource(R.drawable.ic_selete)
    }

    private fun toggleCategoryExpansion(categoryName: String, expandIcon: ImageView) {
        val category = junkCategories[categoryName] ?: return

        category.isExpanded = !category.isExpanded

        // 更新展开图标
        expandIcon.setImageResource(
            if (category.isExpanded) R.drawable.ic_below else R.drawable.ic_right
        )

        // 展开或收起文件列表
        val parentLayout = when (categoryName) {
            "App Cache" -> binding.layoutAppCache
            "Apk Files" -> binding.layoutApkFiles
            "Log Files" -> binding.layoutLogFiles
            "AD Junk" -> binding.layoutAdJunk
            "Temp Files" -> binding.layoutTempFiles
            else -> return
        }

        if (category.isExpanded) {
            category.displayOffset = 0 // 重置显示偏移
            showFileList(parentLayout, category)
        } else {
            hideFileList(parentLayout)
        }
    }

    private fun showFileList(parentLayout: LinearLayout, category: JunkCategory) {
        // 移除之前的文件列表（如果存在）
        hideFileList(parentLayout)

        // 创建文件列表容器
        val fileListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "file_list_container"
        }

        val files = category.getFilesList()
        val startIndex = category.displayOffset
        val endIndex = minOf(startIndex + pageSize, files.size)

        // 添加文件项
        for (i in startIndex until endIndex) {
            val file = files[i]
            val fileItemView = createFileItemView(file, category)
            fileListContainer.addView(fileItemView)
        }

        // 添加加载更多按钮或文件统计信息
        if (endIndex < files.size) {
            val loadMoreView = TextView(this).apply {
                text = "Load more... (${endIndex}/${files.size} files shown)"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                setPadding(68, 16, 16, 16)
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    category.displayOffset += pageSize
                    showFileList(parentLayout, category)
                }
            }
            fileListContainer.addView(loadMoreView)
        } else if (files.size > pageSize) {
            val infoView = TextView(this).apply {
                text = "All ${files.size} files shown"
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(68, 8, 16, 8)
            }
            fileListContainer.addView(infoView)
        }

        parentLayout.addView(fileListContainer)
    }

    private fun hideFileList(parentLayout: LinearLayout) {
        val fileListContainer = parentLayout.findViewWithTag<LinearLayout>("file_list_container")
        fileListContainer?.let {
            parentLayout.removeView(it)
        }
    }

    // ... existing code ...
    private fun createFileItemView(file: JunkFile, category: JunkCategory): View {
        // 创建自定义布局
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(52, 8, 16, 8)
            }
            setPadding(16, 12, 16, 12)
            setBackgroundResource(android.R.drawable.list_selector_background)
        }

        // 文件信息布局
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // 文件名
        val nameText = TextView(this).apply {
            text = file.name
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        // 文件路径和大小
        val (sizeText, unit) = formatStorage(file.size)
        val detailText = TextView(this).apply {
            text = "${file.path} • $sizeText$unit"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }

        infoLayout.addView(nameText)
        infoLayout.addView(detailText)

        // 选择状态图标
        val statusIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setImageResource(if (file.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete)
            setPadding(0, 0, 0, 0)
        }

        container.addView(infoLayout)
        container.addView(statusIcon)

        // 点击事件
        container.setOnClickListener {
            file.isSelected = !file.isSelected
            statusIcon.setImageResource(
                if (file.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
            )
            updateCategorySelectionStatus(category)
        }

        return container
    }

    /**
     * 更新清理按钮的可见性
     * 只有在至少有一个文件被选中时才显示
     */
    private fun updateCleanButtonVisibility() {
        val hasSelectedFiles = junkCategories.values.any { category ->
            category.files.any { it.isSelected }
        }
        binding.butClean.visibility = if (hasSelectedFiles) View.VISIBLE else View.GONE
    }
// ... existing code ...

    // ... existing code ...
    private fun toggleCategorySelection(categoryName: String, statusIcon: ImageView) {
        val category = junkCategories[categoryName] ?: return

        category.isSelected = !category.isSelected

        // 更新分类下所有文件的选择状态
        category.files.forEach { it.isSelected = category.isSelected }

        // 更新UI
        statusIcon.setImageResource(
            if (category.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
        )

        // 如果分类已展开，更新文件列表的选择状态
        if (category.isExpanded) {
            val parentLayout = when (categoryName) {
                "App Cache" -> binding.layoutAppCache
                "Apk Files" -> binding.layoutApkFiles
                "Log Files" -> binding.layoutLogFiles
                "AD Junk" -> binding.layoutAdJunk
                "Temp Files" -> binding.layoutTempFiles
                else -> return
            }
            showFileList(parentLayout, category)
        }

        // 更新清理按钮的可见性
        updateCleanButtonVisibility()
    }
// ... existing code ...


    // ... existing code ...
    private fun updateCategorySelectionStatus(category: JunkCategory) {
        val selectedCount = category.files.count { it.isSelected }
        val totalCount = category.files.size

        category.isSelected = selectedCount == totalCount && totalCount > 0

        // 更新对应的状态图标
        val statusIcon = when (category.type) {
            "App Cache" -> binding.ivAppCacheStatus
            "Apk Files" -> binding.ivAppFileStatus
            "Log Files" -> binding.ivLogFileStatus
            "AD Junk" -> binding.ivAdJunkStatus
            "Temp Files" -> binding.ivTempFilesStatus
            else -> return
        }

        statusIcon.setImageResource(
            if (category.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
        )

        // 更新清理按钮的可见性
        updateCleanButtonVisibility()
    }
// ... existing code ...


    private fun cleanSelectedFiles() {
        var totalCleaned = 0L
        var cleanedCount = 0
        var failedCount = 0
        jumpJob = lifecycleScope.launch(Dispatchers.Main) {
            binding.inClean.load.isVisible = true
          withContext(Dispatchers.IO) {
                junkCategories.values.forEach { category ->
                    val selectedFiles = category.files.filter { it.isSelected }.toList()
                    val filesToRemove = mutableSetOf<JunkFile>()

                    selectedFiles.forEach { junkFile ->
                        try {
                            val file = File(junkFile.path)
                            if (file.exists()) {
                                val deleted = file.delete()
                                if (deleted) {
                                    totalCleaned += junkFile.size
                                    cleanedCount++
                                    filesToRemove.add(junkFile)
                                } else {
                                    failedCount++
                                }
                            } else {
                                // 文件不存在，也从列表中移除
                                filesToRemove.add(junkFile)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            failedCount++
                        }
                    }

                    // 从分类中移除已删除的文件
                    category.files.removeAll(filesToRemove)
                }
            }
            var progress = 0
            while (true){
                progress++
                binding.inClean.pg.progress = progress
                if (progress >= 100) {
                    break
                }
                delay(15)
            }
            val (sizeFormatted, unit) = formatStorage(totalCleaned)
            val cleanedSizeText = "$sizeFormatted $unit"
            val intent = Intent(this@CleanActivity, ResultActivity::class.java)
            intent.putExtra("cleaned_size", cleanedSizeText)
            startActivity(intent)
            binding.inClean.load.isVisible = false
            finish()
        }
    }

    private fun formatStorage(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1024 * 1024 * 1024 -> {
                val gb = bytes.toDouble() / (1024 * 1024 * 1024)
                Pair(decimalFormat.format(gb), "GB")
            }
            bytes >= 1024 * 1024 -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                Pair(decimalFormat.format(mb), "MB")
            }
            else -> {
                val kb = bytes.toDouble() / 1024
                Pair(decimalFormat.format(kb), "KB")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
    }
}