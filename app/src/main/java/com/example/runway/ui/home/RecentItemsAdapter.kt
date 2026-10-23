package com.example.runway.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runway.databinding.ItemRecentBinding
import com.example.runway.domain.model.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The small photo tiles in the Home "Recently added" row.
class RecentItemsAdapter(
    private val scope: CoroutineScope,
    private val onClick: (Item) -> Unit,
) : ListAdapter<Item, RecentItemsAdapter.TileHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileHolder =
        TileHolder(ItemRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TileHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: TileHolder) {
        super.onViewRecycled(holder)
        holder.cancel()
    }

    inner class TileHolder(private val binding: ItemRecentBinding) : RecyclerView.ViewHolder(binding.root) {

        private var loadJob: Job? = null

        fun bind(item: Item) {
            binding.root.setOnClickListener { onClick(item) }
            binding.root.contentDescription = item.name
            binding.recentName.text = item.name

            cancel()
            binding.recentImage.setImageDrawable(null)
            binding.recentPlaceholder.isVisible = true

            val path = item.imagePath ?: return
            loadJob = scope.launch {
                val bitmap = withContext(Dispatchers.IO) { decode(path) } ?: return@launch
                binding.recentImage.setImageBitmap(bitmap)
                binding.recentPlaceholder.isVisible = false
            }
        }

        fun cancel() {
            loadJob?.cancel()
            loadJob = null
        }

        // Tiles are tiny, so decode a scaled-down copy rather than the full photo.
        private fun decode(path: String): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= TILE_PX) sample *= 2
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    private companion object {
        const val TILE_PX = 300

        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(old: Item, new: Item) = old.id == new.id
            override fun areContentsTheSame(old: Item, new: Item) = old == new
        }
    }
}
