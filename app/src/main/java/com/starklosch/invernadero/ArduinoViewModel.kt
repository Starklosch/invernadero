@file:OptIn(ExperimentalCoroutinesApi::class)

package com.starklosch.invernadero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.ConnectionLostException
import com.juul.kable.State
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ArduinoViewModel(address: String) : ViewModel() {
    private val peripheral = viewModelScope.peripheral(address)

    val viewState: Flow<State> = peripheral.state.flatMapLatest { state -> flowOf(state) }

    fun connect(mac: String){
        viewModelScope.connect()
    }

    private fun CoroutineScope.connect() {
        launch {
            try {
                peripheral.connect()
            } catch (e: ConnectionLostException) {

            }
        }
    }
}