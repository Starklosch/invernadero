package com.starklosch.invernadero

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.ConnectionLostException
import com.juul.kable.State
import com.juul.kable.State.Disconnected
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private val _device = MutableStateFlow<SensorTag?>(null)
    private val _state = MutableStateFlow<State>(Disconnected())

    val device = _device.asStateFlow()
    val state = _state.asStateFlow()
    val test = flow {
        var i = 0
        while (true) {
            Log.d("FLOW", "Emitting $i")
            emit(i++)
            delay(1000)
        }
    }

    val test2 = test.stateIn(
        initialValue = 0,
        scope = viewModelScope,
        started = WhileSubscribed(5000)
    )

    fun connect(mac: String) {
        val peripheral = viewModelScope.peripheral(mac)
        val tag = SensorTag(peripheral)
        _device.value = tag

        viewModelScope.observe(tag)
        viewModelScope.connect(_device.value!!)
    }

    fun disconnect() {
        viewModelScope.disconnect()
    }

    fun write() {
        viewModelScope.launch {
            _device.value?.write(Operation(OperationType.ReadValues))
            Log.d("FLOW", "Writing")
        }
    }

    private fun CoroutineScope.observe(peripheral: SensorTag) {
        launch {
            peripheral.state.collect { _state.value = it }
        }
    }

    private fun CoroutineScope.connect(peripheral: SensorTag) {
        launch {
            try {
                peripheral.connect()
            } catch (_: ConnectionLostException) {

            }
        }
    }

    private fun CoroutineScope.disconnect() {
        if (device.value == null)
            return

        launch {
            try {
                device.value!!.disconnect()
            } catch (_: ConnectionLostException) {

            }
        }.invokeOnCompletion {
            _state.value = Disconnected()
            _device.value = null
        }
    }
}