package com.happymax.notificationsync

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainScreenViewModel : ViewModel() {
    private var _isInited = mutableStateOf(false)
    private var _isClient = mutableStateOf(false)
    private var _token = mutableStateOf(" ")

    var isInited : MutableState<Boolean> = _isInited
    var isClient : MutableState<Boolean> = _isClient
    var token : MutableState<String> = _token

}