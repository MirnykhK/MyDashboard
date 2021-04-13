package ru.adminmk.mydashboard.ui.SettingsUI

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SimpleItemTouchHelperCallback(private val mAdapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        mAdapter.onItemMove(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        mAdapter.onItemDismiss(viewHolder.bindingAdapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is ItemTouchHelperViewHolder) {
                viewHolder.onItemSelected()
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is ItemTouchHelperViewHolder) {
            viewHolder.onItemClear()
        }
    }
}