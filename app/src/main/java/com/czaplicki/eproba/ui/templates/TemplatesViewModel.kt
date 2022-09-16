package com.czaplicki.eproba.ui.templates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TemplatesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Szablony nie są jeszcze dostępne w aplikacji mobilnej"
    }
    val text: LiveData<String> = _text
}