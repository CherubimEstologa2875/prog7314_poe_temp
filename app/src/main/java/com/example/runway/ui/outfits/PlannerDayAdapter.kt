package com.example.runway.ui.outfits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runway.R
import com.example.runway.databinding.ItemPlannerDayBinding
import com.example.runway.domain.model.Item
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope

// The squares on the planner calendar, seven to a row.
class PlannerDayAdapter(
    private val scope: CoroutineScope,
    private val onClick: (PlannerDay) -> Unit,
) : ListAdapter<PlannerDay, PlannerDayAdapter.DayHolder>(DIFF) {

    // Set by the fragment from the screen width, so every cell is 3:4.
    var cellHeight: Int = 0

    var itemsById: Map<String, Item> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayHolder {
        val binding = ItemPlannerDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (cellHeight > 0) binding.root.layoutParams.height = cellHeight
        return DayHolder(binding)
    }

    override fun onBindViewHolder(holder: DayHolder, position: Int) = holder.bind(getItem(position))

    inner class DayHolder(private val binding: ItemPlannerDayBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: PlannerDay) {
            val card = binding.plannerDayCard
            binding.plannerDayNumber.text = day.date.dayOfMonth.toString()

            val accent = MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary)
            val outline = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline)
            card.strokeColor = if (day.isToday) accent else outline
            binding.plannerDayNumber.setTextColor(
                if (day.isToday) accent else MaterialColors.getColor(card, android.R.attr.textColorSecondary)
            )
            // Days from the months either side fade back, like the mockup.
            card.alpha = if (day.inMonth) 1f else 0.45f

            binding.plannerDayThumb.isVisible = day.outfit != null
            binding.plannerDayThumb.bind(day.outfit, itemsById, scope)

            card.contentDescription = listOfNotNull(day.date.toString(), day.outfit?.name).joinToString(", ")
            card.setOnClickListener { onClick(day) }
        }
    }

    companion object {
        const val COLUMNS = 7

        private val DIFF = object : DiffUtil.ItemCallback<PlannerDay>() {
            override fun areItemsTheSame(old: PlannerDay, new: PlannerDay) = old.date == new.date
            override fun areContentsTheSame(old: PlannerDay, new: PlannerDay) = old == new
        }
    }
}
