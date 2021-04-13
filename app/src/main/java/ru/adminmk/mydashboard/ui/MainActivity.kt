package ru.adminmk.mydashboard

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.SoundPool
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.disposables.CompositeDisposable
import ru.adminmk.mydashboard.databinding.MainActivityBinding
import ru.adminmk.mydashboard.model.api.DashboardResponse
import ru.adminmk.mydashboard.ui.MainFragmentDirections
import ru.adminmk.mydashboard.ui.PerformanceFragmentDirections
import ru.adminmk.mydashboard.viewmodel.*


class MainActivity : AppCompatActivity(), MainCallbacks {
    private lateinit var binding: MainActivityBinding

    private lateinit var navController: NavController

    private lateinit var mainViewModel: MainViewModel
    private lateinit var communicationViewModel: CommunicationViewModel

    private val titleDisposable = CompositeDisposable()

    private lateinit var vibe: Vibrator
    private lateinit var spool: SoundPool
    private var soundID: Int = 0

    private var receiver: BroadcastReceiver? = null

    companion object {
        const val MY_PACK = "ru.adminmk.mydashboard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModels()


        binding = MainActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        setupAccessibilityObservers()



        initNavGraph(savedInstanceState)
        setupBackButton()

        initNotificationPool()
        configureReceiver()

        if (savedInstanceState == null) {
            initCommunication(
                id = resources.getString(R.string.default_notification_channel_id),
                name = resources.getString(R.string.default_notification_channel_name),
                description = resources.getString(R.string.default_notification_channel_description)
            )
        }
    }

    private fun initNotificationPool() {
        vibe = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        spool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()


        val ringtoneUri =
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION)


        contentResolver.openAssetFileDescriptor(ringtoneUri, "r").use {
            soundID = spool.load(it, 1)
        }
    }

    private fun configureReceiver() {
        val filter = IntentFilter()
        filter.addAction(MY_PACK)
        receiver = FCMReceiver()
        registerReceiver(receiver, filter)
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
        spool.unload(soundID)
        spool.release()
    }

    private fun setupBackButton() {
        binding.buttonBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setupAccessibilityObservers() {
        mainViewModel.loginInProgress.observe(this, {
            this.invalidateOptionsMenu()
        })

        mainViewModel.currFragment.observe(this, {
            this.invalidateOptionsMenu()
        })
    }

    private fun initViewModels() {
        val sharedPreferences =
            application.getSharedPreferences(LOGIN_SETTINGS, Context.MODE_PRIVATE)
        val factory = MainViewModelFactory(sharedPreferences, this)
        mainViewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)


        communicationViewModel = ViewModelProvider(this).get(CommunicationViewModel::class.java)
    }

    private fun initNavGraph(savedInstanceState: Bundle?) {
        navController = Navigation.findNavController(this, R.id.container)

        if (savedInstanceState == null) {
//            val curFragment = viewModel.getCurrentDestination()

            val startFragment = mainViewModel.getStartFragment()
            setCurrentFragment(startFragment)
        }
    }

    private fun initCommunication(
        id: String, name: String,
        description: String
    ) {

        val notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        communicationViewModel.createNotificationChannel(id, name, description, notificationManager)

        communicationViewModel.subscribeToTopic(
//            successMessage= getString(R.string.message_subscribed),
            failMessage = getString(R.string.message_subscribe_failed),
            showOnComplete = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            })

    }


    override fun setToolbarTitle(rString: Int) {
        binding.toolbarStatus.text = getString(rString)
    }

    override fun onLogin(dashboardResponse: DashboardResponse?, showTransition: Boolean) {
        setToolbarTitle(R.string.app_name_loged_in)


        val action =
            if (showTransition) {
                MainFragmentDirections.actionMainFragmentToPerformanceFragment(dashboardResponse)
            } else {
                MainFragmentDirections.actionMainFragmentToPerformanceFragmentNoAnimation(
                    dashboardResponse
                )
            }


        navController.navigate(
            action
        )

    }

    private fun setCurrentFragment(destFragmentR: Int) {
        val graphInflater = navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.navigation_graph)

        navGraph.startDestination = destFragmentR
        navController.graph = navGraph
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val menuId = when (mainViewModel.currFragment.value) {
            CurrentFragment.LOGIN -> R.menu.toolbar_empty
            CurrentFragment.SETTINGS -> R.menu.toolbar_empty
            else -> {
                if (mainViewModel.loginInProgress.value == true) {
                    R.menu.toolbar_in_connection
                } else {
                    R.menu.toolbar
                }
            }
        }

        val buttonBackIsVisible = when (mainViewModel.currFragment.value) {
            CurrentFragment.SETTINGS -> View.VISIBLE
            else -> View.GONE
        }
        binding.buttonBack.visibility = buttonBackIsVisible


        menuInflater.inflate(menuId, menu)


        if (menu is MenuBuilder) {
            try {
                menu.setOptionalIconsVisible(true)
            } catch (e: Exception) {
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                goToSettings(); true
            }
            R.id.menu_stop -> {
                mainViewModel.onLoginPressed(this, communicationViewModel); true
            }
            R.id.menu_update -> {
                mainViewModel.onLoginPressed(this, communicationViewModel); true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun goToSettings() {
        val action =
            PerformanceFragmentDirections.actionPerformanceFragmentToSettingsContainerFragment()

        navController.navigate(
            action
        )
    }


    override fun onResume() {
        super.onResume()

        makeViewBinding()
    }

    override fun onPause() {
        super.onPause()

        releaseViewBinding()

        mainViewModel.unsubscribeToResponce()
    }

    private fun makeViewBinding() {
        val titleObservable = mainViewModel.getTitleObservable()
        titleDisposable.add(titleObservable.subscribe(this::setToolbarTitle))
    }

    private fun releaseViewBinding() {
        titleDisposable.clear()
    }

    private fun getSomeString(stringR: Int) = resources.getString(stringR)

    override fun composeError(loginError: LoginError): String {
        val errorMessageBuilder = StringBuilder()
        loginError.listOfErrors?.let {
            for (errorId in it) {
                if (errorMessageBuilder.toString()
                        .isNotBlank()
                ) errorMessageBuilder.append(", ")


                errorMessageBuilder.append(getSomeString(errorId))
            }
        }

        loginError.throwableError?.let {
            if (errorMessageBuilder.toString().isNotBlank()
            ) errorMessageBuilder.append(", ")

            errorMessageBuilder.append(it)
        }

        if (errorMessageBuilder.toString().isNotBlank()) {
            errorMessageBuilder.insert(0, "Login error: ")
        }

        return errorMessageBuilder.toString()
    }


    override fun onErrorLogin(error: String) {
        val action = PerformanceFragmentDirections.actionPerformanceFragmentToMainFragment(error)

        navController.navigate(
            action
        )

    }

    private inner class FCMReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!(mainViewModel.loginInProgress.value
                    ?: false) && mainViewModel.logedIn.value?.isLogedIn ?: false
            ) {
                when (mainViewModel.notificationMode) {
                    NotificationMode.SOUND -> {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val volume =
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                        val maxVolume =
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

                        if (maxVolume == 0.0F) return
                        val normalizedVolume = volume / maxVolume

                        try {
                            spool.play(soundID, normalizedVolume, normalizedVolume, 1, 0, 1f)
                        } catch (e: Exception) {
                        }
                    }
                    NotificationMode.VIBRO -> {
                        val effect =
                            VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrate(effect)
                    }
                    NotificationMode.SILENT -> {
                    }
                }
                mainViewModel.onLoginPressed(context, communicationViewModel)
            }
        }
    }

    override fun vibrate(effect: VibrationEffect) {
        vibe.vibrate(effect)
    }

}


interface MainCallbacks {
    fun setToolbarTitle(rString: Int)
    fun onLogin(dashboardResponse: DashboardResponse?, showTransition: Boolean = true)
    fun composeError(loginError: LoginError): String
    fun onErrorLogin(error: String)
    fun vibrate(effect: VibrationEffect)
}