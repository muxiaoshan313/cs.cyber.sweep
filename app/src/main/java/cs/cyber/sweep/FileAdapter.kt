package cs.cyber.sweep

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cs.cyber.sweep.databinding.ItemFileCleanBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCS, DOWNLOAD, ZIP, OTHER
}

data class FileItem(
    val file: File,
    val name: String,
    val size: Long,
    val sizeFormatted: String,
    val unit: String,
    val type: FileType,
    val lastModified: Long,
    var isSelected: Boolean = false
)

class FileAdapter(
    private val onItemClick: (FileItem, Int) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var files: MutableList<FileItem> = mutableListOf()

    inner class FileViewHolder(private val binding: ItemFileCleanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fileItem: FileItem, position: Int) {
            binding.tvFileName.text = fileItem.name
            binding.tvFileSize.text = fileItem.sizeFormatted
            binding.tvFileUnit.text = fileItem.unit

            // 加载文件图标或缩略图
            loadFileIcon(fileItem)

            // 设置选中状态
            binding.ivSelect.setImageResource(
                if (fileItem.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
            )

            binding.root.setOnClickListener {
                onItemClick(fileItem, position)
            }

            Log.v("FileAdapter", "Binding item $position: ${fileItem.name}")
        }

        private fun loadFileIcon(fileItem: FileItem) {
            when (fileItem.type) {
                FileType.IMAGE -> {
                    Glide.with(McApp.application)
                        .load(fileItem.file)
                        .apply(
                            RequestOptions()
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.ic_file_logo)
                                .placeholder(R.drawable.ic_file_logo)
                        )
                        .into(binding.ivFileIcon)
                }
                FileType.VIDEO -> {
                    // 显示视频缩略图
                    loadVideoThumbnail(fileItem.file)
                }
                FileType.AUDIO -> {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
                FileType.DOCS -> {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
                FileType.DOWNLOAD -> {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
                FileType.ZIP -> {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
                else -> {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
            }
        }

        private fun loadVideoThumbnail(videoFile: File) {
            try {
                Glide.with(McApp.application)
                    .asBitmap()
                    .load(videoFile)
                    .apply(
                        RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.ic_file_logo)
                            .placeholder(R.drawable.ic_file_logo)
                    )
                    .into(binding.ivFileIcon)
            } catch (e: Exception) {
                Log.e("FileAdapter", "Error loading video thumbnail: ${e.message}")
                loadVideoThumbnailWithRetriever(videoFile)
            }
        }

        private fun loadVideoThumbnailWithRetriever(videoFile: File) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoFile.absolutePath)
                val bitmap = retriever.getFrameAtTime(1000000) // 获取第一秒的帧
                retriever.release()

                if (bitmap != null) {
                    binding.ivFileIcon.setImageBitmap(bitmap)
                } else {
                    binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
                }
            } catch (e: Exception) {
                Log.e("FileAdapter", "Error creating video thumbnail: ${e.message}")
                binding.ivFileIcon.setImageResource(R.drawable.ic_file_logo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileCleanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        Log.d("FileAdapter", "Creating ViewHolder")
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        if (position < files.size) {
            holder.bind(files[position], position)
        } else {
            Log.e("FileAdapter", "Invalid position: $position, size: ${files.size}")
        }
    }

    override fun getItemCount(): Int {
        Log.d("FileAdapter", "getItemCount: ${files.size}")
        return files.size
    }

    fun updateFiles(newFiles: List<FileItem>) {
        Log.d("FileAdapter", "updateFiles called with ${newFiles.size} files")
        files.clear()
        files.addAll(newFiles)
        Log.d("FileAdapter", "Files updated, new size: ${files.size}")
        notifyDataSetChanged()
        Log.d("FileAdapter", "notifyDataSetChanged called")
    }

    fun selectAll(isSelected: Boolean) {
        files.forEach { it.isSelected = isSelected }
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<FileItem> {
        return files.filter { it.isSelected }
    }

    fun getSelectedCount(): Int {
        return files.count { it.isSelected }
    }
}