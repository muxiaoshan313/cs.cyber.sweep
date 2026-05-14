package cs.cyber.sweep

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cs.cyber.sweep.databinding.ActivityFileScanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.util.*

class FileScanActivity : AppCompatActivity() {

    private val binding by lazy { ActivityFileScanBinding.inflate(layoutInflater) }
    private val decimalFormat = DecimalFormat("#.#")

    private lateinit var fileAdapter: FileAdapter
    private var allFiles = mutableListOf<FileItem>()
    private var filteredFiles = mutableListOf<FileItem>()

    // 筛选条件
    private var currentFileType: FileType? = null
    private var currentSizeFilter: Long = 0 // 最小大小（字节）
    private var currentTimeFilter: Long = 0 // 时间戳
    private var jumpJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) {
            loadFiles()
        } else {
            Toast.makeText(
                this,
                "Storage permission required. Please grant permission and try again.",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.file)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        showScanDialog()
        initViews()
        loadFiles()
    }

    fun showScanDialog() {
        lifecycleScope.launch {
            var progress = 0
            binding.inClean.tvTip.text = "Scanning..."
            binding.inClean.imgBg2.setImageResource(R.drawable.ic_file_clean)
            binding.inClean.load.isVisible = true
            while (true) {
                progress++
                binding.inClean.pg.progress = progress
                if (progress >= 100) {
                    break
                }
                delay(15)
            }
            binding.inClean.load.isVisible = false
        }
    }

    private fun initViews() {
        // 初始化filteredFiles列表
        filteredFiles.clear()

        // 设置RecyclerView
        fileAdapter = FileAdapter { fileItem, position ->
            fileItem.isSelected = !fileItem.isSelected
            fileAdapter.notifyItemChanged(position)
            updateSelectedCount()
            updateSelectAllIcon()
        }

        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@FileScanActivity)
            adapter = fileAdapter
        }

        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.inClean.tvBack.setOnClickListener {
            jumpJob?.cancel()
            binding.inClean.load.isVisible = false
        }
        binding.llTypes.setOnClickListener {
            toggleDropdown(DropdownType.TYPE)
        }

        binding.llSize.setOnClickListener {
            toggleDropdown(DropdownType.SIZE)
        }

        binding.llTime.setOnClickListener {
            toggleDropdown(DropdownType.TIME)
        }

        binding.ivSelectAll.setOnClickListener {
            val allSelected =
                fileAdapter.getSelectedCount() == filteredFiles.size && filteredFiles.isNotEmpty()
            fileAdapter.selectAll(!allSelected)
            updateSelectedCount()
            updateSelectAllIcon()
        }

        binding.tvDelete.setOnClickListener {
            deleteSelectedFiles()
        }

        binding.viewDropdownBg.setOnClickListener {
            hideAllDropdowns()
        }

        setupDropdownClickListeners()
    }

    private fun setupDropdownClickListeners() {
        // 文件类型下拉菜单
        binding.tvAllType.setOnClickListener { selectFileType(null, "All types") }
        binding.tvImage.setOnClickListener { selectFileType(FileType.IMAGE, "Image") }
        binding.tvVideo.setOnClickListener { selectFileType(FileType.VIDEO, "Video") }
        binding.tvAudio.setOnClickListener { selectFileType(FileType.AUDIO, "Audio") }
        binding.tvDocs.setOnClickListener { selectFileType(FileType.DOCS, "Docs") }
        binding.tvDownload.setOnClickListener { selectFileType(FileType.DOWNLOAD, "Download") }
        binding.tvZip.setOnClickListener { selectFileType(FileType.ZIP, "Zip") }

        // 文件大小下拉菜单
        binding.tvAllSize.setOnClickListener { selectSizeFilter(0, "All Size") }
        binding.tv10mb.setOnClickListener { selectSizeFilter(10 * 1024 * 1024L, ">10MB") }
        binding.tv20mb.setOnClickListener { selectSizeFilter(20 * 1024 * 1024L, ">20MB") }
        binding.tv50mb.setOnClickListener { selectSizeFilter(50 * 1024 * 1024L, ">50MB") }
        binding.tv100mb.setOnClickListener { selectSizeFilter(100 * 1024 * 1024L, ">100MB") }
        binding.tv200mb.setOnClickListener { selectSizeFilter(200 * 1024 * 1024L, ">200MB") }
        binding.tv500mb.setOnClickListener { selectSizeFilter(500 * 1024 * 1024L, ">500MB") }

        // 时间筛选下拉菜单
        val currentTime = System.currentTimeMillis()
        binding.tvAllTime.setOnClickListener { selectTimeFilter(0, "All Time") }
        binding.tv1day.setOnClickListener {
            selectTimeFilter(
                currentTime - 24 * 60 * 60 * 1000L,
                "Within 1 day"
            )
        }
        binding.tv1week.setOnClickListener {
            selectTimeFilter(
                currentTime - 7 * 24 * 60 * 60 * 1000L,
                "Within 1 week"
            )
        }
        binding.tv1month.setOnClickListener {
            selectTimeFilter(
                currentTime - 30 * 24 * 60 * 60 * 1000L,
                "Within 1 month"
            )
        }
        binding.tv3month.setOnClickListener {
            selectTimeFilter(
                currentTime - 90 * 24 * 60 * 60 * 1000L,
                "Within 3 month"
            )
        }
        binding.tv6month.setOnClickListener {
            selectTimeFilter(
                currentTime - 180 * 24 * 60 * 60 * 1000L,
                "Within 6 month"
            )
        }
    }

    private fun loadFiles() {
        // 首先检查权限
        if (!hasStoragePermissions()) {
            Log.e("FileScan", "No storage permissions, requesting permissions...")
            Toast.makeText(
                this,
                "Storage permission required. Please grant permission and try again.",
                Toast.LENGTH_LONG
            ).show()
            requestStoragePermissions()
            return
        }

        lifecycleScope.launch {
            try {
                binding.tvSelectedCount.text = "Scanning files..."
                showEmptyState(false)

                val files = withContext(Dispatchers.IO) {
                    scanAllFiles()
                }

                Log.d("FileScan", "Found ${files.size} files")

                withContext(Dispatchers.Main) {
                    allFiles.clear()
                    allFiles.addAll(files)

                    if (files.isNotEmpty()) {
                        Log.d("FileScan", "Sample files:")
                        files.take(5).forEach { file ->
                            Log.d(
                                "FileScan",
                                "File: ${file.name}, Size: ${file.sizeFormatted}${file.unit}, Type: ${file.type}"
                            )
                        }
                    }
                    applyFilters()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FileScan", "Error loading files", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FileScanActivity,
                        "Failed to load files: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState(true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasStoragePermissions() && allFiles.isEmpty()) {
            loadFiles()
        }
    }

    private fun requestStoragePermissions() {
        requestPermissionLauncher.launch(StoragePermissions.permissionsToRequest())
    }

    private fun scanAllFiles(): List<FileItem> {
        val fileList = mutableListOf<FileItem>()
        val scannedFiles = mutableSetOf<String>()

        if (!hasStoragePermissions()) {
            Log.e("FileScan", "Storage permissions not granted")
            return fileList
        }

        val directories = mutableListOf<File>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            addLimitedDirectories(directories)
        } else {
            addStorageDirectories(directories)
        }

        Log.d("FileScan", "Scanning directories: ${directories.map { it.absolutePath }}")

        for (directory in directories) {
            try {
                if (directory.exists() && directory.canRead()) {
                    Log.d("FileScan", "Successfully accessing directory: ${directory.absolutePath}")
                    scanDirectory(directory, fileList, mutableSetOf(), scannedFiles)
                } else {
                    Log.w("FileScan", "Cannot access directory: ${directory.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w("FileScan", "Exception accessing directory: ${directory.absolutePath}", e)
            }
        }

        Log.d("FileScan", "Total unique files found: ${fileList.size}")
        return fileList.sortedByDescending { it.size }
    }

    private fun addStorageDirectories(directories: MutableList<File>) {
        Environment.getExternalStorageDirectory()?.let { dir ->
            if (dir.exists()) directories.add(dir)
        }

        val publicDirs = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_DOCUMENTS,
            Environment.DIRECTORY_DCIM
        )

        for (dirType in publicDirs) {
            Environment.getExternalStoragePublicDirectory(dirType)?.let { dir ->
                if (dir.exists() && !directories.contains(dir)) {
                    directories.add(dir)
                }
            }
        }
    }

    private fun addLimitedDirectories(directories: MutableList<File>) {
        getExternalFilesDir(null)?.let { directories.add(it) }
        externalCacheDir?.let { directories.add(it) }

        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                ?.let { dir ->
                    if (dir.exists() && dir.canRead()) directories.add(dir)
                }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?.let { dir ->
                    if (dir.exists() && dir.canRead()) directories.add(dir)
                }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.let { dir ->
                if (dir.exists() && dir.canRead()) directories.add(dir)
            }
        } catch (e: Exception) {
            Log.w("FileScan", "Cannot access public directories", e)
        }
    }

    private fun hasStoragePermissions(): Boolean = StoragePermissions.hasAll(this)

    private fun scanDirectory(
        directory: File,
        fileList: MutableList<FileItem>,
        visitedDirs: MutableSet<String>,
        scannedFiles: MutableSet<String>
    ) {
        try {
            val canonicalPath = directory.canonicalPath
            if (visitedDirs.contains(canonicalPath)) {
                return
            }
            visitedDirs.add(canonicalPath)

            val files = directory.listFiles()
            if (files == null) {
                Log.w("FileScan", "Cannot list files in: ${directory.absolutePath}")
                return
            }

            for (file in files) {
                try {
                    if (file.isFile && file.length() > 0 && file.canRead()) {
                        val canonicalFilePath = file.canonicalPath
                        if (!scannedFiles.contains(canonicalFilePath)) {
                            scannedFiles.add(canonicalFilePath)

                            val fileType = getFileType(file)
                            val (sizeFormatted, unit) = formatFileSize(file.length())

                            fileList.add(
                                FileItem(
                                    file = file,
                                    name = file.name,
                                    size = file.length(),
                                    sizeFormatted = sizeFormatted,
                                    unit = unit,
                                    type = fileType,
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    } else if (file.isDirectory && file.canRead() && !file.name.startsWith(".")) {
                        scanDirectory(file, fileList, visitedDirs, scannedFiles)
                    }
                } catch (e: SecurityException) {
                    Log.w("FileScan", "Permission denied for file: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.w("FileScan", "Error processing file: ${file.absolutePath}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FileScan", "Error scanning directory: ${directory.absolutePath}", e)
        }
    }

    private fun getFileType(file: File): FileType {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg" -> FileType.IMAGE
            "mp4", "avi", "mkv", "mov", "3gp", "wmv", "flv", "webm", "m4v" -> FileType.VIDEO
            "mp3", "wav", "aac", "flac", "ogg", "wma", "m4a" -> FileType.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "rtf" -> FileType.DOCS
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileType.ZIP
            "apk", "exe", "deb", "dmg" -> FileType.OTHER
            else -> {
                val path = file.absolutePath.lowercase()
                when {
                    path.contains("download") -> FileType.DOWNLOAD
                    path.contains("dcim") || path.contains("pictures") || path.contains("camera") -> FileType.IMAGE
                    path.contains("movies") || path.contains("video") -> FileType.VIDEO
                    path.contains("music") || path.contains("audio") -> FileType.AUDIO
                    path.contains("documents") -> FileType.DOCS
                    else -> FileType.OTHER
                }
            }
        }
    }

    private fun formatFileSize(bytes: Long): Pair<String, String> {
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

    private fun applyFilters() {
        filteredFiles.clear()

        for (file in allFiles) {
            var include = true

            // 文件类型筛选
            if (currentFileType != null && file.type != currentFileType) {
                include = false
            }

            // 文件大小筛选
            if (currentSizeFilter > 0 && file.size < currentSizeFilter) {
                include = false
            }

            // 时间筛选
            if (currentTimeFilter > 0 && file.lastModified < currentTimeFilter) {
                include = false
            }

            if (include) {
                filteredFiles.add(file)
            }
        }

        Log.d("FileScan", "Filtered files: ${filteredFiles.size}")

        runOnUiThread {
            fileAdapter.updateFiles(filteredFiles)
            updateSelectedCount()
            updateSelectAllIcon()

            // 显示或隐藏空状态
            showEmptyState(filteredFiles.isEmpty())
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            binding.rvFiles.visibility = View.GONE
            binding.llBottomControl.visibility = View.GONE
            binding.tvDelete.visibility = View.GONE
            binding.ivNoData.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.VISIBLE
        } else {
            binding.rvFiles.visibility = View.VISIBLE
            binding.llBottomControl.visibility = View.VISIBLE
            binding.tvDelete.visibility = View.VISIBLE
            binding.ivNoData.visibility = View.GONE
            binding.tvNoData.visibility = View.GONE
        }
    }

    private fun selectFileType(type: FileType?, displayText: String) {
        currentFileType = type
        binding.tvTypes.text = displayText
        hideAllDropdowns()
        applyFilters()
    }

    private fun selectSizeFilter(sizeBytes: Long, displayText: String) {
        currentSizeFilter = sizeBytes
        binding.tvSize.text = displayText
        hideAllDropdowns()
        applyFilters()
    }

    private fun selectTimeFilter(timeStamp: Long, displayText: String) {
        currentTimeFilter = timeStamp
        binding.tvTime.text = displayText
        hideAllDropdowns()
        applyFilters()
    }

    private enum class DropdownType {
        TYPE, SIZE, TIME
    }

    private fun toggleDropdown(type: DropdownType) {
        hideAllDropdowns()
        binding.viewDropdownBg.visibility = View.VISIBLE

        when (type) {
            DropdownType.TYPE -> binding.llDropdownTypes.visibility = View.VISIBLE
            DropdownType.SIZE -> binding.llDropdownSize.visibility = View.VISIBLE
            DropdownType.TIME -> binding.llDropdownTime.visibility = View.VISIBLE
        }
    }

    private fun hideAllDropdowns() {
        binding.viewDropdownBg.visibility = View.GONE
        binding.llDropdownTypes.visibility = View.GONE
        binding.llDropdownSize.visibility = View.GONE
        binding.llDropdownTime.visibility = View.GONE
    }

    private fun updateSelectedCount() {
        val selectedCount = fileAdapter.getSelectedCount()
        val totalCount = filteredFiles.size
        binding.tvSelectedCount.text = "$selectedCount/$totalCount selected"
    }

    private fun updateSelectAllIcon() {
        val allSelected =
            fileAdapter.getSelectedCount() == filteredFiles.size && filteredFiles.isNotEmpty()
        binding.ivSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
        )
    }

    private fun deleteSelectedFiles() {
        val selectedFiles = fileAdapter.getSelectedFiles()

        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            return
        }


        jumpJob = lifecycleScope.launch(Dispatchers.Main) {
            var progress = 0
            binding.inClean.tvTip.text = "Cleaning..."
            binding.inClean.load.isVisible = true
            try {
                var totalDeletedSize = 0L
                val deletedCount = withContext(Dispatchers.IO) {
                    var count = 0
                    for (fileItem in selectedFiles) {
                        try {
                            if (fileItem.file.delete()) {
                                totalDeletedSize += fileItem.size
                                count++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    count
                }
                if (deletedCount > 0) {
                    val (sizeFormatted, unit) = formatFileSize(totalDeletedSize)
                    val cleanedSizeText = "$sizeFormatted $unit"

                    while (true) {
                        progress++
                        binding.inClean.pg.progress = progress
                        if (progress >= 100) {
                            break
                        }
                        delay(15)
                    }
                    val intent =
                        Intent(this@FileScanActivity, ResultActivity::class.java).apply {
                            putExtra("cleaned_size", cleanedSizeText)
                        }
                    startActivity(intent)
                    binding.inClean.load.isVisible = false
                    finish()
                } else {
                    Toast.makeText(
                        this@FileScanActivity,
                        "Failed to delete files",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@FileScanActivity,
                    "Error deleting files",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    }
}