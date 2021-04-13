package ru.adminmk.mydashboard.viewmodel

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import ru.adminmk.mydashboard.model.initCredentialsInModel
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val sharedPreferences: SharedPreferences,
                           owner: SavedStateRegistryOwner,
                           defaultArgs: Bundle? = null): AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val result: T?
        if(modelClass === MainViewModel::class.java){
            result =  MainViewModel(handle) as T
            initCredentialsInModel(sharedPreferences, result as MainViewModel)
        }else{
            throw IllegalArgumentException("Unknown ViewModel class!")
        }

        return result
    }
}