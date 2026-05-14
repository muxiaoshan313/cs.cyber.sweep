package cs.cyber.sweep

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat

class PhotoAdapter(
    private val photoGroups: MutableList<PhotoGroup>,
    private val onPhotoClick: (PhotoItem, Int, Int) -> Unit,
    private val onGroupSelectAll: (PhotoGroup, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PHOTO = 1
    }

    private val decimalFormat = DecimalFormat("#.#")

    override fun getItemViewType(position: Int): Int {
        var currentPos = 0
        for (group in photoGroups) {
            if (currentPos == position) return TYPE_HEADER
            currentPos++
            if (position < currentPos + group.photos.size) return TYPE_PHOTO
            currentPos += group.photos.size
        }
        return TYPE_PHOTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_header, parent, false)
            PhotoHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
            PhotoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (group, localPos) = getItemAtPosition(position)

        if (holder is PhotoHeaderViewHolder) {
            holder.bind(group, photoGroups.indexOf(group))
        } else if (holder is PhotoViewHolder && localPos >= 0) {
            holder.bind(group.photos[localPos], photoGroups.indexOf(group), localPos)
        }
    }

    override fun getItemCount(): Int {
        return photoGroups.sumOf { it.photos.size + 1 } // +1 for header
    }

    private fun getItemAtPosition(position: Int): Pair<PhotoGroup, Int> {
        var currentPos = 0
        for (group in photoGroups) {
            if (currentPos == position) return Pair(group, -1) // Header
            currentPos++
            if (position < currentPos + group.photos.size) {
                return Pair(group, position - currentPos)
            }
            currentPos += group.photos.size
        }
        return Pair(photoGroups.last(), -1)
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

    inner class PhotoHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_size)
        private val ivSelectAll: ImageView = itemView.findViewById(R.id.iv_select_all)
        private val llSelectAll: View = itemView.findViewById(R.id.ll_select_all)

        fun bind(group: PhotoGroup, groupIndex: Int) {
            tvDate.text = group.date

            val (size, unit) = formatStorage(group.getTotalSize())
            tvSize.text = "$size $unit"

            ivSelectAll.setImageResource(
                if (group.isAllSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
            )

            llSelectAll.setOnClickListener {
                onGroupSelectAll(group, groupIndex)
            }
        }
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.iv_photo)
        private val ivSelect: ImageView = itemView.findViewById(R.id.iv_select)
        private val tvSize: MaterialButton = itemView.findViewById(R.id.tv_size)
        fun bind(photo: PhotoItem, groupIndex: Int, photoIndex: Int) {
            Glide.with(McApp.application)
                .load(photo.path)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8)))
                .placeholder(R.drawable.ic_no_data)
                .error(R.drawable.ic_no_data)
                .into(ivPhoto)

            ivSelect.setImageResource(
                if (photo.isSelected) R.drawable.ic_selete else R.drawable.ic_dis_selete
            )
            tvSize.text = formatStorage(photo.size).first+" "+formatStorage(photo.size).second

            itemView.setOnClickListener {
                onPhotoClick(photo, groupIndex, photoIndex)
            }
        }
    }
}