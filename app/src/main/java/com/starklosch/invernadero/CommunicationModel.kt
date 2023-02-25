package com.starklosch.invernadero

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CommunicationModel : ViewModel() {
    private val messages = MutableStateFlow<MutableList<String>>(mutableListOf())
    private val test = MutableStateFlow("Hola")

    fun getMessages(): StateFlow<List<String>> = messages

    fun getTest(): StateFlow<String> = test

    fun addMessage(message : String) {
        messages.update {
            it.apply { it.add(message) }
        }
    }

    fun setTest(value : String){
        test.update { value }
    }
}