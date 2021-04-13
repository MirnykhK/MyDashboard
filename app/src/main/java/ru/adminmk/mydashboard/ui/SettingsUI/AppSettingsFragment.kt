package ru.adminmk.mydashboard.ui.SettingsUI

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import ru.adminmk.mydashboard.databinding.FragmentAppSettingsBinding
import ru.adminmk.mydashboard.viewmodel.LOGIN_SETTINGS
import ru.adminmk.mydashboard.viewmodel.MainViewModel
import ru.adminmk.mydashboard.viewmodel.NotificationMode


class AppSettingsFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private var binding: FragmentAppSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAppSettingsBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appSettingsBinding = binding as FragmentAppSettingsBinding

        initTextValues(appSettingsBinding)
    }

    private fun initTextValues(appSettingsBinding: FragmentAppSettingsBinding) {
        appSettingsBinding.checkBoxAutoLogin.isChecked = mainViewModel.autologin
        appSettingsBinding.radioButtonNotificationSilent.isChecked =
            mainViewModel.notificationMode == NotificationMode.SILENT
        appSettingsBinding.radioButtonNotificationSound.isChecked =
            mainViewModel.notificationMode == NotificationMode.SOUND
        appSettingsBinding.radioButtonNotificationVibrate.isChecked =
            mainViewModel.notificationMode == NotificationMode.VIBRO

        val sharedPreferences = context?.getSharedPreferences(LOGIN_SETTINGS, Context.MODE_PRIVATE)

        appSettingsBinding.checkBoxAutoLogin.setOnCheckedChangeListener { _, isChecked ->
            mainViewModel.saveLoginData(
                MainViewModel.SETTINGS_AUTOLOGIN,
                isChecked,
                sharedPreferences!!
            )
        }
        appSettingsBinding.radioButtonNotificationSilent.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            mainViewModel.saveLoginData(
                MainViewModel.SETTINGS_NOTIFICATION_MODE,
                NotificationMode.SILENT.ordinal,
                sharedPreferences!!
            )
        }
        appSettingsBinding.radioButtonNotificationSound.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            mainViewModel.saveLoginData(
                MainViewModel.SETTINGS_NOTIFICATION_MODE,
                NotificationMode.SOUND.ordinal,
                sharedPreferences!!
            )
        }
        appSettingsBinding.radioButtonNotificationVibrate.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
            mainViewModel.saveLoginData(
                MainViewModel.SETTINGS_NOTIFICATION_MODE,
                NotificationMode.VIBRO.ordinal,
                sharedPreferences!!
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}