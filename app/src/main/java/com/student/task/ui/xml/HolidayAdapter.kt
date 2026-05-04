package com.student.task.ui.xml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.transition.TransitionManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.student.task.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.student.task.databinding.ItemHolidayCardBinding
import com.student.task.databinding.ItemLoadingMoreBinding
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel

class HolidayAdapter(
    private val onCardClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit
) : ListAdapter<HolidayAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var isLoadingMore = false

    sealed class ListItem {
        data class HolidayItem(val uiModel: HolidayUiModel) : ListItem()
        data object LoadingItem : ListItem()
    }

    fun submitHolidays(holidays: List<HolidayUiModel>, loadingMore: Boolean) {
        isLoadingMore = loadingMore
        val items = mutableListOf<ListItem>()
        items.addAll(holidays.map { ListItem.HolidayItem(it) })
        if (loadingMore) {
            items.add(ListItem.LoadingItem)
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HolidayItem -> VIEW_TYPE_HOLIDAY
            is ListItem.LoadingItem -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HOLIDAY -> {
                val binding = ItemHolidayCardBinding.inflate(inflater, parent, false)
                HolidayViewHolder(binding)
            }
            VIEW_TYPE_LOADING -> {
                val binding = ItemLoadingMoreBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HolidayItem -> (holder as HolidayViewHolder).bind(item.uiModel)
            is ListItem.LoadingItem -> { }
        }
    }

    inner class HolidayViewHolder(
        private val binding: ItemHolidayCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: HolidayUiModel) {
            val holiday = uiModel.holiday

            binding.emojiText.text = holiday.category.emoji
            binding.categoryBadge.text = holiday.category.displayName
            binding.holidayName.text = holiday.name
            binding.holidayDate.text = holiday.date
            binding.holidayDescription.text = holiday.description
            binding.officialBadge.visibility = if (holiday.isOfficial) View.VISIBLE else View.GONE

            binding.cardRoot.setOnClickListener {
                onCardClick(holiday.id)
            }
            binding.favoriteButton.setOnClickListener {
                onFavoriteClick(holiday.id)
            }

            TransitionManager.beginDelayedTransition(binding.cardRoot)

            when (uiModel.cardState) {
                CardState.Default -> {
                    bindDefault()
                }
                CardState.Expanded -> {
                    bindExpanded()
                }
                CardState.Favorite -> {
                    bindFavorite()
                }
            }
        }

        private fun bindDefault() {
            binding.holidayDescription.visibility = View.GONE
            binding.expandIndicator.setImageResource(android.R.drawable.arrow_down_float)
            binding.favoriteButton.setImageResource(android.R.drawable.btn_star_big_off)
            binding.cardRoot.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, android.R.color.white)
            )
        }

        private fun bindExpanded() {
            binding.holidayDescription.visibility = View.VISIBLE
            binding.expandIndicator.setImageResource(android.R.drawable.arrow_up_float)
            binding.favoriteButton.setImageResource(android.R.drawable.btn_star_big_off)
            binding.cardRoot.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, android.R.color.white)
            )
        }

        private fun bindFavorite() {
            binding.holidayDescription.visibility = View.GONE
            binding.expandIndicator.setImageResource(android.R.drawable.arrow_down_float)
            binding.favoriteButton.setImageResource(android.R.drawable.btn_star_big_on)
            binding.cardRoot.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, R.color.purple_200)
            )
        }

    }

    class LoadingViewHolder(binding: ItemLoadingMoreBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.HolidayItem && newItem is ListItem.HolidayItem ->
                    oldItem.uiModel.holiday.id == newItem.uiModel.holiday.id
                oldItem is ListItem.LoadingItem && newItem is ListItem.LoadingItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_HOLIDAY = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
