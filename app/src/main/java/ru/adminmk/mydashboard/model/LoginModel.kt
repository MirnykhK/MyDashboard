package ru.adminmk.mydashboard.model

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import ru.adminmk.mydashboard.viewmodel.MainViewModel

fun initCredentialsInModel(sharedPreferences: SharedPreferences, result: MainViewModel){
    val serverAdress = sharedPreferences.getString(MainViewModel.SERVER_ADRESS, null)
//    result.serverAdress = "http://10.10.10.36/1cMyDashboard/hs/Dashboard/"
    val dataBase = sharedPreferences.getString(MainViewModel.DATABASE, null)
    val login = sharedPreferences.getString(MainViewModel.LOGIN, null)
    val password = sharedPreferences.getString(MainViewModel.PASSWORD, null)
    val id = sharedPreferences.getString(MainViewModel.ID, null)
    val autologin = sharedPreferences.getBoolean(MainViewModel.SETTINGS_AUTOLOGIN, false)
    val notificationMode = sharedPreferences.getInt(MainViewModel.SETTINGS_NOTIFICATION_MODE, 0)

    result.initCredentials(serverAdress, dataBase, login, password, id, autologin, notificationMode)
}

fun saveCredentialsInModel(sharedPreferences: SharedPreferences, field: String, value: Any?){
    val editor = sharedPreferences.edit()

    when(field){
        MainViewModel.SERVER_ADRESS -> editor.putString(field, value as String?)
        MainViewModel.DATABASE -> editor.putString(field, value as String?)
        MainViewModel.LOGIN -> editor.putString(field, value as String?)
        MainViewModel.PASSWORD -> editor.putString(field, value as String?)
        MainViewModel.ID -> editor.putString(field, value as String?)
        MainViewModel.SETTINGS_AUTOLOGIN ->  editor.putBoolean(field, value as Boolean)
        MainViewModel.SETTINGS_NOTIFICATION_MODE -> editor.putInt(field, value as Int)
    }

    editor.apply()
}

fun hasNetwork(context: Context?): Boolean {
    var isConnected = false

    val connectionType = getConnectionType(context)
    if(connectionType != 0) isConnected = true

    return isConnected
}

fun getConnectionType(context: Context?): Int {
    var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi

    context?.let {  val cm = it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            result = 2
                        }
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            result = 1
                        }
                        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                            result = 3
                        }
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            result = 2
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            result = 1
                        }
                        ConnectivityManager.TYPE_VPN -> {
                            result = 3
                        }
                    }
                }
            }
        } }


    return result
}