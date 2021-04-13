package ru.adminmk.mydashboard.ui.SettingsUI

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import ru.adminmk.mydashboard.R
import ru.adminmk.mydashboard.databinding.FragmentContainerSettingsBinding
import ru.adminmk.mydashboard.viewmodel.CurrentFragment
import ru.adminmk.mydashboard.viewmodel.MainViewModel


class SettingsContainerFragment : Fragment() {

    private var binding: FragmentContainerSettingsBinding?=null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: TransactionPagerAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContainerSettingsBinding.inflate(inflater, container, false)
        return binding?.root
    }


    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentSettingsBinding = binding as FragmentContainerSettingsBinding

        adapter = TransactionPagerAdapter(this)
        fragmentSettingsBinding.pager.adapter = adapter

        TabLayoutMediator(fragmentSettingsBinding.tabLayout, fragmentSettingsBinding.pager) { tab, position ->

            when(position){
                TransactionPagerAdapter.INDICATORS_ORDER_SCREEN -> tab.text = resources.getText(R.string.order_of_data)
                TransactionPagerAdapter.APP_SETTINGS_SCREEN -> tab.text = resources.getText(R.string.app_settings)
            }


        }.attach()
    }

    override fun onResume() {
        super.onResume()
        viewModel.titleFragmentOnResume(R.string.app_name_settings)
    }

    override fun onStart() {
        super.onStart()
        viewModel.setCurrentFragment(CurrentFragment.SETTINGS)
    }

    internal class TransactionPagerAdapter(fragment:Fragment) :
        FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment = when (position) {
            INDICATORS_ORDER_SCREEN -> OrderSettingsFragment()
            APP_SETTINGS_SCREEN -> AppSettingsFragment()
            else -> OrderSettingsFragment()
        }

        override fun getItemCount(): Int = TRANSACTION_SCREENS_NUMBER

        companion object {
            internal const val TRANSACTION_SCREENS_NUMBER = 2

            internal const val INDICATORS_ORDER_SCREEN = 0
            internal const val APP_SETTINGS_SCREEN = 1
        }
    }


}