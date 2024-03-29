package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderInterestNewViewHolder
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestNewAdapter(
    private val uiHelpers: UiHelpers
) : Adapter<ReaderInterestNewViewHolder>() {
    private val items = mutableListOf<ReaderInterestUiState>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderInterestNewViewHolder {
        return ReaderInterestNewViewHolder(uiHelpers, parent)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ReaderInterestNewViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<ReaderInterestUiState>) {
        val diffResult = DiffUtil.calculateDiff(InterestDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class InterestDiffUtil(
        private val oldList: List<ReaderInterestUiState>,
        private val newList: List<ReaderInterestUiState>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = newList[newItemPosition]
            val oldItem = oldList[oldItemPosition]

            return (oldItem == newItem)
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
