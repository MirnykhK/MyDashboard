package ru.adminmk.mydashboard.ui.SettingsUI

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import ru.adminmk.mydashboard.R
import ru.adminmk.mydashboard.databinding.FragmentOrderSettingsBinding
import ru.adminmk.mydashboard.model.database.OrderEntity
import ru.adminmk.mydashboard.viewmodel.CommunicationViewModel
import ru.adminmk.mydashboard.viewmodel.OnIndicatorUpdateMessage
import java.util.*
import kotlin.collections.ArrayList


class OrderSettingsFragment : Fragment() {

    private var binding: FragmentOrderSettingsBinding? = null
    private val communicationViewModel: CommunicationViewModel by activityViewModels()
    private lateinit var adapter: OrderAdapter

    private var indicatorsDisposable: Disposable? = null
    private var validatedDisposable: Disposable? = null

    private var listOfUnvalidatedOrderEntities: MutableList<OrderEntity>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderSettingsBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentOrderSettingsBinding = binding as FragmentOrderSettingsBinding
        val recyclerView = fragmentOrderSettingsBinding.recycler
        recyclerView.layoutManager =
            SmoothScrollLinearLayoutManager(requireContext(), LinearLayout.VERTICAL, false)

        setupAdapter(recyclerView)
        recyclerView.adapter = adapter

        val callback = SimpleItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

    }

    private fun setupAdapter(recyclerView: RecyclerView) {
        adapter = OrderAdapter(ArrayList())
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()

                val positionOfUnvalidated =
                    communicationViewModel.getUnValidatedPositionStart(adapter.listOfIndicators)

                listOfUnvalidatedOrderEntities = if (positionOfUnvalidated >= 0) {

                    val signString = resources.getString(R.string.new_indicators_sign)
                    Toast.makeText(
                        requireContext(),
                        signString + (adapter.listOfIndicators.size - positionOfUnvalidated),
                        Toast.LENGTH_SHORT
                    ).show()

                    adapter.listOfIndicators.filter { it.isSubmitted == 0 }.toMutableList()
                } else {
                    null
                }

                if (positionOfUnvalidated > 0) {
                    recyclerView.smoothScrollToPosition(positionOfUnvalidated)
                }
            }
        })


    }

    override fun onResume() {
        super.onResume()

        makeViewBinding()
    }

    private fun makeViewBinding() {
        val indicatorsObservable = communicationViewModel.getOrderEntitiesList()

        val actionOnError: (e: Throwable) -> Unit = { e: Throwable ->
            val errorString = resources.getString(R.string.existed_indicators_error)
            Toast.makeText(requireContext(), errorString + e.message, Toast.LENGTH_SHORT).show()
        }


        indicatorsDisposable = indicatorsObservable.subscribe(this::updateList, actionOnError)

        adapter.setOrderOnDragSubscription()
    }


    override fun onPause() {
        super.onPause()

        releaseViewBinding()
        adapter.releaseBindings()
    }

    private fun releaseViewBinding() {
        indicatorsDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }

        validatedDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    private fun updateList(orderEntities: List<OrderEntity>?) {

        adapter.listOfIndicators.clear()
        orderEntities?.let { adapter.listOfIndicators.addAll(it) }

        adapter.notifyDataSetChanged()

        val fragmentOrderSettingsBinding = binding as FragmentOrderSettingsBinding
        if (orderEntities == null || orderEntities.isEmpty()) {
            fragmentOrderSettingsBinding.recycler.visibility = View.GONE
            fragmentOrderSettingsBinding.titleOfOrder.titleLayout.visibility = View.GONE
            fragmentOrderSettingsBinding.textViewEmptyOrderSettings.visibility = View.VISIBLE
        } else {
            fragmentOrderSettingsBinding.recycler.visibility = View.VISIBLE
            fragmentOrderSettingsBinding.titleOfOrder.titleLayout.visibility = View.VISIBLE
            fragmentOrderSettingsBinding.textViewEmptyOrderSettings.visibility = View.GONE
        }
    }


    private inner class IndicatorViewHolder(view: View) : RecyclerView.ViewHolder(view),
        ItemTouchHelperViewHolder {
        val textViewName = view.findViewById<TextView>(R.id.textViewName)
        val checkBoxIsVisible = view.findViewById<CheckBox>(R.id.checkBoxIsVisible)
        val cardView = view.findViewById<CardView>(R.id.card_view)

        var isVisibleSubscription: Disposable? = null
        private val subjectClicksIsVisible = PublishSubject.create<OrderEntity>()

        init {
            checkBoxIsVisible.setOnClickListener {
                subjectClicksIsVisible.onNext(adapter.listOfIndicators[this.absoluteAdapterPosition])
            }
        }


        override fun onItemSelected() {
            val color = ContextCompat.getColor(itemView.context, R.color.blue_200_dragndrop)
            itemView.setBackgroundColor(color)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
            adapter.onMoveEnd()
        }

        fun getClickIsVisibleObservable() = subjectClicksIsVisible.hide()

    }

    private inner class OrderAdapter(val listOfIndicators: ArrayList<OrderEntity>) :
        ItemTouchHelperAdapter, RecyclerView.Adapter<IndicatorViewHolder>() {
        private val compositeSubscription = CompositeDisposable()
        private val subjectListOfIndicators = PublishSubject.create<List<OrderEntity>>()
//        private val subjectOnMove = PublishSubject.create<Unit>()

        private val actionOnNextOnIndicatorUpdateMessage: (it: OnIndicatorUpdateMessage) -> Unit = {
            it.error?.let {
                val signString = resources.getString(R.string.is_visible_indicator_error)
                Toast.makeText(
                    requireContext(),
                    signString + it.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        private val actionOnErrorOnIndicatorUpdateMessage: (e: Throwable) -> Unit = {
            val signString = resources.getString(R.string.is_visible_indicator_error)
            Toast.makeText(
                requireContext(),
                signString + it.message,
                Toast.LENGTH_LONG
            ).show()
        }

        fun getListOfIndicatorsObservable() = subjectListOfIndicators.hide()

        fun onMoveEnd() {
            subjectListOfIndicators.onNext(listOfIndicators)
        }

        fun setOrderOnDragSubscription() {
            val subscription =
                communicationViewModel.getUpdateOrderListObservable(getListOfIndicatorsObservable())
                    .subscribe(
                        actionOnNextOnIndicatorUpdateMessage,
                        actionOnErrorOnIndicatorUpdateMessage
                    )

            compositeSubscription.add(subscription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndicatorViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.indicator_order_layout,
                parent,
                false
            )
            return IndicatorViewHolder(view)
        }


        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)

            releaseBindings()
        }

        override fun onBindViewHolder(holder: IndicatorViewHolder, position: Int) {
            val curOrderEntity = listOfIndicators[position]
            holder.textViewName.text = curOrderEntity.name
            holder.checkBoxIsVisible.isChecked = curOrderEntity.isVisible == 1
            enableHolder(holder)

            setIsVisibleSubscription(holder)


            listOfUnvalidatedOrderEntities?.let {
                if (it.contains(curOrderEntity)) {

                    markAndUpdateNewIndicator(holder, curOrderEntity, position)

                    it.remove(curOrderEntity)
                }
            }
        }

        fun releaseBindings() {
            compositeSubscription.clear()
        }

        private fun setIsVisibleSubscription(holder: IndicatorViewHolder) {
            holder.isVisibleSubscription?.let {
                if (!it.isDisposed) {
                    compositeSubscription.delete(it)
                    it.dispose()
                }
            }

            val isVisibleStateObservable = RxCompoundButton.checkedChanges(holder.checkBoxIsVisible)


            val subscription = communicationViewModel.getUpdateIsVisibleIndicatorObservable(
                holder.getClickIsVisibleObservable(),
                isVisibleStateObservable
            )
                .subscribe(
                    actionOnNextOnIndicatorUpdateMessage,
                    actionOnErrorOnIndicatorUpdateMessage
                )

            holder.isVisibleSubscription = subscription

            compositeSubscription.add(subscription)
        }

        override fun getItemCount(): Int {
            return listOfIndicators.size
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(listOfIndicators, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(listOfIndicators, i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onItemDismiss(position: Int) {
            listOfIndicators.removeAt(position)
            notifyItemRemoved(position)
        }
    }


    private fun blink(holder: IndicatorViewHolder) {

        val backgroundColorAnimator = ObjectAnimator.ofObject(
            holder.cardView,
            "backgroundColor",
            ArgbEvaluator(),
            ContextCompat.getColor(requireContext(), R.color.blue_200_transparent),
            ContextCompat.getColor(requireContext(), R.color.design_default_color_background)
        )
        backgroundColorAnimator.repeatCount = 2
        backgroundColorAnimator.repeatMode = ObjectAnimator.REVERSE
        backgroundColorAnimator.duration = 800
        backgroundColorAnimator.start()


    }


    private fun markAndUpdateNewIndicator(
        holder: IndicatorViewHolder,
        indicator: OrderEntity,
        index: Int
    ) {
        blink(holder)

        val validatedIndicatorsObservable =
            communicationViewModel.updateValidatedIndicator(indicator, index)
        val actionOnNext: (it: Unit) -> Unit = {
            enableHolder(holder)
        }
        val actionOnError: (e: Throwable) -> Unit = {
            enableHolder(holder)
        }
        validatedDisposable = validatedIndicatorsObservable.subscribe(actionOnNext, actionOnError)
        disableHolder(holder)
    }

    private fun disableHolder(holder: IndicatorViewHolder) {
        holder.cardView.isEnabled = false; holder.checkBoxIsVisible.isEnabled = false
    }

    private fun enableHolder(holder: IndicatorViewHolder) {
        holder.cardView.isEnabled = true; holder.checkBoxIsVisible.isEnabled = true
    }

}