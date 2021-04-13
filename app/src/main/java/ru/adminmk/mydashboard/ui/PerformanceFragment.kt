package ru.adminmk.mydashboard.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.Disposable
import ru.adminmk.mydashboard.MainCallbacks
import ru.adminmk.mydashboard.R
import ru.adminmk.mydashboard.databinding.FragmentPerformanceBinding
import ru.adminmk.mydashboard.model.api.DashboardResponse
import ru.adminmk.mydashboard.model.api.DataSet
import ru.adminmk.mydashboard.model.api.DirectionOfChange
import ru.adminmk.mydashboard.viewmodel.CommunicationViewModel
import ru.adminmk.mydashboard.viewmodel.CurrentFragment
import ru.adminmk.mydashboard.viewmodel.MainViewModel


class PerformanceFragment : Fragment() {

    private lateinit var adapter: ElementAdapter
    private var binding: FragmentPerformanceBinding? = null


    private val viewModel: MainViewModel by activityViewModels()
    private val communicationViewModel: CommunicationViewModel by activityViewModels()

    private var orderDisposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPerformanceBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onStart() {
        super.onStart()

        initDataElements()

        viewModel.setCurrentFragment(CurrentFragment.PERFORMANCE)
    }

    private fun initDataElements() {
        arguments?.let {
            val args = PerformanceFragmentArgs.fromBundle(it)
            communicationViewModel.dashboardResponce = args.response

            var dataSetIsInit = false

            communicationViewModel.dashboardResponce?.let {
                dataSetIsInit = true; setInitUpdateOrderSubscription(it)
            }


            if (!dataSetIsInit) {
                viewModel.logedIn.value?.let {
                    if (it.isLogedIn == true) {
                        it.dashboardResponse?.let {
                            dataSetIsInit = true
                            setInitUpdateOrderSubscription(it)
                        }
                    }
                }
            }

            if (!dataSetIsInit) {
                viewModel.onLoginPressed(requireContext(), communicationViewModel)
            }
        }
    }

    private fun setInitUpdateOrderSubscription(dashboardResponse: DashboardResponse) {
        orderDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }

        val actionOnNext: (it: DashboardResponse) -> Unit = {
            updateDataElements(it.listOfIndicators)
        }
        val actionOnError: (e: Throwable) -> Unit = {
            viewModel.onLoginPressed(requireContext(), communicationViewModel)
        }

        orderDisposable = communicationViewModel.getSortOrderObservable(dashboardResponse)
            .subscribe(actionOnNext, actionOnError)
    }

    private class ElementHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView = view.findViewById<TextView>(R.id.textViewName)
        val valueTextView = view.findViewById<TextView>(R.id.textViewValue)
        val button = view.findViewById<ImageButton>(R.id.button)
    }

    private inner class ElementAdapter(val elements: ArrayList<DataSet>) :
        RecyclerView.Adapter<ElementHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementHolder {
            val view = this@PerformanceFragment.layoutInflater.inflate(
                R.layout.indicator_layout,
                parent,
                false
            )
            return ElementHolder(view)
        }

        override fun getItemCount(): Int {
            return elements.size
        }

        override fun onBindViewHolder(holder: ElementHolder, position: Int) {
            val curElement = elements[position]

            holder.nameTextView.text = curElement.dataIndicator?.name
            holder.valueTextView.text = curElement.value

            val pic =
                when (curElement.direction) {
                    DirectionOfChange.UP -> R.drawable.ic_up
                    DirectionOfChange.NONE -> R.drawable.ic_stable
                    DirectionOfChange.DOWN -> R.drawable.ic_down
                    else -> R.drawable.ic_stable
                }

            holder.button.setImageResource(pic)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentPerformanceBinding = binding as FragmentPerformanceBinding
        val recyclerView = fragmentPerformanceBinding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this.activity)

        adapter = ElementAdapter(ArrayList<DataSet>())
        recyclerView.adapter = adapter

        setupAccessibilityObservers(fragmentPerformanceBinding)
    }

    private fun setupAccessibilityObservers(fragmentPerformanceBinding: FragmentPerformanceBinding) {
        viewModel.logedIn.observe(this.viewLifecycleOwner, { loginState ->
            loginState?.let {
                if (it.isLogedIn == true) {
                    it.dashboardResponse.let {
                        it?.let {
                            setInitUpdateOrderSubscription(it)
                        }
                    }
                }
            }
        })


        viewModel.loginError.observe(this.viewLifecycleOwner, { loginError ->
            loginError?.let {
                if (loginError != MainViewModel.BLANK_ERROR) {
                    val mainCallbaks = this.activity as MainCallbacks
                    val stringError = mainCallbaks.composeError(it)
                    mainCallbaks.onErrorLogin(stringError)
                }
            }
        })

        viewModel.loginInProgress.observe(this.viewLifecycleOwner, { isInProgress ->
            if (isInProgress) {
                fragmentPerformanceBinding.progressBar.visibility = View.VISIBLE
            } else {
                fragmentPerformanceBinding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateDataElements(listOfData: List<DataSet>) {
        adapter.elements.clear()
        adapter.elements.addAll(listOfData)
        adapter.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    override fun onResume() {
        super.onResume()

        viewModel.titleFragmentOnResume(R.string.app_name_loged_in)
    }

    override fun onStop() {
        super.onStop()

        orderDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }
}

