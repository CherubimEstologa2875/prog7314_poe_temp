package com.example.runway.ui.outfits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runway.R
import com.example.runway.databinding.ItemOutfitCardBinding
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import kotlinx.coroutines.CoroutineScope

// One card per saved outfit in the Outfits grid. The picture is the render, or the builder collage.
class OutfitsAdapter(
    private val scope: CoroutineScope,
    private val onClick: (Outfit) -> Unit,
) : ListAdapter<Outfit, OutfitsAdapter.CardHolder>(DIFF) {

    // Photo paths for the collage thumbnails.
    var itemsById: Map<String, Item> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder =
        CardHolder(ItemOutfitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CardHolder, position: Int) = holder.bind(getItem(position))

    inner class CardHolder(private val binding: ItemOutfitCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(outfit: Outfit) {
            val context = binding.root.context
            binding.root.setOnClickListener { onClick(outfit) }
            binding.outfitCardName.text = outfit.name
            binding.outfitCardMeta.text = listOfNotNull(
                outfit.occasion,
                context.resources.getQuantityString(R.plurals.rw_outfits_pieces, outfit.garmentCount, outfit.garmentCount),
            ).joinToString(" · ")
            binding.outfitCardThumb.bind(outfit, itemsById, scope)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Outfit>() {
            override fun areItemsTheSame(old: Outfit, new: Outfit) = old.id == new.id
            override fun areContentsTheSame(old: Outfit, new: Outfit) = old == new
        }
    }
}
