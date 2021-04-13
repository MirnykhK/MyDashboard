package ru.adminmk.mydashboard.ui.SettingsUI

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller


class SmoothScrollLinearLayoutManager(context: Context, orientation:Int, reverseLayout:Boolean): LinearLayoutManager(context, orientation, reverseLayout) {
    companion object { private const val MILLISECONDS_PER_INCH = 250f}

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView, state: RecyclerView.State?,
        position: Int
    ) {
        val smoothScroller: SmoothScroller = TopSnappedSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }


    private open inner class TopSnappedSmoothScroller(context: Context?) :
        LinearSmoothScroller(context) {

        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            return PointF(0F, 1F)
        }

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }


        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
        }
    }
}