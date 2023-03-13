package com.starklosch.invernadero

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.ConnectionLostException
import com.juul.kable.State
import com.juul.kable.State.Disconnected
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private val device = MutableStateFlow<ArduinoPeripheral?>(null)
    private val _deviceName = MutableStateFlow("")
    private val _state = MutableStateFlow<State>(Disconnected())
    private val _settings = MutableStateFlow(Settings())
    private val _values = MutableStateFlow(Values())
    private val _information = MutableStateFlow(Information())

    val deviceName = _deviceName.asStateFlow()
    val connectionState = _state.asStateFlow()
    val values = _values.asStateFlow()
    val settings = _settings.asStateFlow()
    val information = _information.asStateFlow()

    fun connect(mac: String) {
        val peripheral = viewModelScope.peripheral(mac)
        val tag = ArduinoPeripheral(peripheral)
        disconnect()

        _deviceName.value = peripheral.name ?: ""
        device.value = tag
        device.value?.let {
            viewModelScope.observe(tag)
            viewModelScope.connect(it)
        }
    }

    fun disconnect() {
        viewModelScope.disconnect()
    }

    fun updateValues() {
        viewModelScope.launch {
            device.value?.request(Request.ValuesRequest())
            Log.d("FLOW", "Updating values")
        }
    }
    
    fun setSettings(settings: Settings){
        launch {
            val request = Request.SetSettingsRequest(settings)
            peripheral.request(request)
        }
    }

    private fun CoroutineScope.observe(peripheral: ArduinoPeripheral) {
        launch {
            peripheral.state.collect { _state.value = it }
        }
        launch {
            peripheral.operations.collect {
                when (it) {
                    is Response.ValuesResponse -> _values.value = it.values.ifNegative(_values.value)
                    is Response.InformationResponse -> _information.value = it.information
                    is Response.SettingsResponse -> _settings.value = it.settings
                    else -> {}
                }
            }
        }
    }

    private fun CoroutineScope.connect(peripheral: ArduinoPeripheral) {
        launch {
            try {
                peripheral.connect()
                peripheral.request(Request.InformationRequest())
//                peripheral.request(Request.SettingsRequest())
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
            device.value = null
        }
    }
}