package com.example.runway.ui.wardrobe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runway.R
import com.example.runway.databinding.ItemWardrobeBinding
import com.example.runway.domain.model.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// One row per garment in the wardrobe (IIE, 2026; Android Open Source Project, 2020a).
// Thumbnails are decoded off the main thread and cached, because a list that
// decodes a photo during binding stutters as soon as there are more than a few.

class WardrobeAdapter(
    private val scope: CoroutineScope,
    private val onClick: (Item) -> Unit,
    private val matchScore: (Item) -> Int? = { null },
    private val onUseInOutfit: ((Item) -> Unit)? = null,
) : ListAdapter<Item, WardrobeAdapter.ItemViewHolder>(DIFF) {

    // Small enough to be safe on a low-memory device, large enough for a screenful.
    private val thumbnails = object : LruCache<String, Bitmap>(CACHE_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemWardrobeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) =
        holder.bind(getItem(position))

    // A recycled row must forget the load it started, or a fast scroll drops the
    // wrong photo into the wrong row.
    override fun onViewRecycled(holder: ItemViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelThumbnail()
    }

    inner class ItemViewHolder(
        private val binding: ItemWardrobeBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadJob: Job? = null

        fun bind(item: Item) {
            binding.root.setOnClickListener { onClick(item) }
            binding.wardrobeItemName.text = item.name

            binding.wardrobeItemMeta.text = item.purchasePrice?.let {
                binding.root.context.getString(R.string.rw_wardrobe_price, it)
            } ?: listOfNotNull(
                item.category.lowercase().replaceFirstChar { it.uppercase() },
                item.size?.takeIf { it.isNotBlank() },
                item.brand?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")

            binding.wardrobeItemStat.text = when {
                item.purchasePrice != null -> ""
                item.costPerWear != null ->
                    binding.root.context.getString(R.string.rw_wardrobe_cost_per_wear, item.costPerWear)
                item.wearCount > 0 ->
                    binding.root.context.getString(R.string.rw_wardrobe_worn, item.wearCount)
                else -> binding.root.context.getString(R.string.rw_wardrobe_never_worn)
            }

            // Marks a row the server has not accepted yet, so "it saved" and "it
            // synced" are visibly different things.
            binding.wardrobeItemPending.isVisible = item.pendingSync

            val score = matchScore(item)
            binding.wardrobeItemMatchScore.isVisible = score != null
            binding.wardrobeItemMatchScore.text = score?.let {
                binding.root.context.getString(R.string.rw_colour_matcher_score, it)
            }
            binding.wardrobeItemUseInOutfit.isVisible = onUseInOutfit != null
            binding.wardrobeItemUseInOutfit.setOnClickListener {
                onUseInOutfit?.invoke(item)
            }

            loadThumbnail(item)
        }

        private fun loadThumbnail(item: Item) {
            cancelThumbnail()

            val path = item.imagePath
            if (path.isNullOrBlank()) {
                binding.wardrobeItemImage.setImageResource(R.drawable.ic_rw_plus)
                return
            }

            thumbnails.get(path)?.let {
                binding.wardrobeItemImage.setImageBitmap(it)
                return
            }

            binding.wardrobeItemImage.setImageDrawable(null)
            loadJob = scope.launch {
                val bitmap = withContext(Dispatchers.IO) { decode(path) } ?: return@launch
                thumbnails.put(path, bitmap)
                // The row may have been rebound to another item while this ran.
                if (bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    getItem(bindingAdapterPosition).imagePath == path
                ) {
                    binding.wardrobeItemImage.setImageBitmap(bitmap)
                }
            }
        }

        fun cancelThumbnail() {
            loadJob?.cancel()
            loadJob = null
        }

        // Decoded at roughly the size the row shows, not at full camera resolution.
        private fun decode(path: String): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > THUMBNAIL_PX) sample *= 2

            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    private companion object {
        const val THUMBNAIL_PX = 240
        val CACHE_SIZE = ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt()

        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(old: Item, new: Item) = old.id == new.id

            override fun areContentsTheSame(old: Item, new: Item) = old == new
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020a. Create dynamic lists with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 15 September 2026].
*/
