package com.czaplicki.eproba.ui.compose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ComposeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Tworzenie prób jest obecnie niedostępne, skorzystaj ze strony internetowej"
    }
    val text: LiveData<String> = _text
}