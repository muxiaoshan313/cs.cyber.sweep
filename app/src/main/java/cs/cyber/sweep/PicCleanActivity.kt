package cs.cyber.sweep

import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter.formatFileSize
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import cs.cyber.sweep.databinding.ActivityPicCleanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class PicCleanActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPicCleanBinding.inflate(layoutInflater) }
    private lateinit var photoAdapter: PhotoAdapter
    private val photoGroups = mutableListOf<PhotoGroup>()
    private val decimalFormat = DecimalFormat("#.#")
    private var isAllSelected = false
    private var jumpJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.image)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()
        showScanDialog()
        initViews()
        loadPhotos()
    }

    fun showScanDialog() {
        lifecycleScope.launch {
            var progress = 0
            binding.inClean.tvTip.text = "Scanning..."
            binding.inClean.imgBg2.setImageResource(R.drawable.ic_picture_clean)
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
        binding.textBack.setOnClickListener {
            finish()
        }
        binding.inClean.tvBack.setOnClickListener {
            jumpJob?.cancel()
            binding.inClean.load.isVisible = false
        }
        val spanCount = 3
        val gridLayoutManager = GridLayoutManager(this, spanCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (photoAdapter.getItemViewType(position) == 0) spanCount else 1
            }
        }
        binding.rvPhotos.layoutManager = gridLayoutManager
        photoAdapter = PhotoAdapter(
            photoGroups,
            onPhotoClick = { photo, groupIndex, photoIndex ->
                togglePhotoSelection(photo, groupIndex, photoIndex)
            },
            onGroupSelectAll = { group, groupIndex ->
                toggleGroupSelection(group, groupIndex)
            }
        )

        binding.rvPhotos.adapter = photoAdapter

        binding.llSelectAll.setOnClickListener {
            toggleAllSelection()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun loadPhotos() {
        val photos = getPhotosFromDevice()
        groupPhotosByDate(photos)
        updateUI()
    }

    private fun getPhotosFromDevice(): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Images.Media.SIZE} > ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val path = it.getString(dataColumn)
                val size = it.getLong(sizeColumn)
                val dateAdded = it.getLong(dateColumn)
                val displayName = it.getString(nameColumn)

                // 检查文件是否存在
                if (File(path).exists()) {
                    photos.add(PhotoItem(id, path, size, dateAdded, displayName))
                }
            }
        }

        return photos
    }

    private fun groupPhotosByDate(photos: List<PhotoItem>) {
        photoGroups.clear()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterdayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val today = todayFormat.format(Date())
        val yesterday =
            yesterdayFormat.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

        val groupedPhotos = photos.groupBy { photo ->
            val date = Date(photo.dateAdded * 1000)
            dateFormat.format(date)
        }

        for ((dateString, photoList) in groupedPhotos) {
            val displayDate = when (dateString) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> {
                    val date = dateFormat.parse(dateString)
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }
            }

            photoGroups.add(PhotoGroup(displayDate, photoList.toMutableList()))
        }
    }

    private fun togglePhotoSelection(photo: PhotoItem, groupIndex: Int, photoIndex: Int) {
        photo.isSelected = !photo.isSelected

        // 更新组的全选状态
        val group = photoGroups[groupIndex]
        group.isAllSelected = group.photos.all { it.isSelected }

        // 更新全局全选状态
        updateGlobalSelectAllState()

        photoAdapter.notifyDataSetChanged()
        updateUI()
    }

    private fun toggleGroupSelection(group: PhotoGroup, groupIndex: Int) {
        group.isAllSelected = !group.isAllSelected

        // 更新组内所有照片的选中状态
        group.photos.forEach { photo ->
            photo.isSelected = group.isAllSelected
        }

        // 更新全局全选状态
        updateGlobalSelectAllState()

        photoAdapter.notifyDataSetChanged()
        updateUI()
    }

    private fun toggleAllSelection() {
        isAllSelected = !isAllSelected

        // 更新所有照片和组的选中状态
        photoGroups.forEach { group ->
            group.isAllSelected = isAllSelected
            group.photos.forEach { photo ->
                photo.isSelected = isAllSelected
            }
        }

        photoAdapter.notifyDataSetChanged()
        updateUI()
    }

    private fun updateGlobalSelectAllState() {
        isAllSelected = photoGroups.all { group ->
            group.photos.all { it.isSelected }
        }
    }

    private fun updateUI() {
        val selectedPhotos = photoGroups.flatMap { group ->
            group.photos.filter { it.isSelected }
        }

        val totalSelectedSize = selectedPhotos.sumOf { it.size }
        val (size, unit) = formatStorage(totalSelectedSize)

        binding.tvProNum.text = size
        binding.tvUnit.text = unit

        // 更新全选按钮状态
        binding.ivSelectAll.setImageResource(
            if (isAllSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
        )

        // 更新删除按钮状态
        binding.btnDelete.isEnabled = selectedPhotos.isNotEmpty()

        // 更新文件类型显示
        binding.tvFile.text = when {
            selectedPhotos.isEmpty() -> "No images selected"
            selectedPhotos.size == 1 -> "1 image selected"
            else -> "${selectedPhotos.size} images selected"
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

            bytes >= 1024 -> {
                val kb = bytes.toDouble() / 1024
                Pair(decimalFormat.format(kb), "KB")
            }

            else -> {
                Pair(bytes.toString(), "B")
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        val selectedPhotos = photoGroups.flatMap { group ->
            group.photos.filter { it.isSelected }
        }

        if (selectedPhotos.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Delete Photos")
            .setMessage("Are you sure you want to delete ${selectedPhotos.size} selected photos? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedPhotos(selectedPhotos)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedPhotos(photosToDelete: List<PhotoItem>) {
        var deletedCount = 0
        var deletedSize = 0L
        jumpJob = lifecycleScope.launch(Dispatchers.Main) {
            var progress = 0
            binding.inClean.tvTip.text = "Cleaning..."
            binding.inClean.load.isVisible = true
            withContext(Dispatchers.IO) {
                photosToDelete.forEach { photo ->
                    try {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            photo.id
                        )

                        val deleted = contentResolver.delete(uri, null, null)
                        if (deleted > 0) {
                            deletedCount++
                            deletedSize += photo.size

                            // 从数据中移除
                            photoGroups.forEach { group ->
                                group.photos.removeAll { it.id == photo.id }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                photoGroups.removeAll { it.photos.isEmpty() }
            }
            while (true) {
                progress++
                binding.inClean.pg.progress = progress
                if (progress >= 100) {
                    break
                }
                delay(15)
            }
            val (sizeFormatted, unit) = formatFileSize(deletedSize)
            val cleanedSizeText = "$sizeFormatted $unit"

            val intent = Intent(this@PicCleanActivity, ResultActivity::class.java).apply {
                putExtra("cleaned_size", cleanedSizeText)
            }
            startActivity(intent)
            binding.inClean.load.isVisible = false
            finish()
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
}