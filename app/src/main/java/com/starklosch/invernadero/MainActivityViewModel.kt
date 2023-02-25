package com.starklosch.invernadero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.ConnectionLostException
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private var connectedDevice = MutableStateFlow<SensorTag?>(null)

    val device = connectedDevice.asStateFlow()

    fun connect(mac: String){
        val peripheral = viewModelScope.peripheral(mac)
        connectedDevice.value = SensorTag(peripheral)
        viewModelScope.connect(connectedDevice.value!!)
    }

    fun write(){
        viewModelScope.launch {
            connectedDevice.value?.write(Operation(OperationType.ReadValues))
        }
    }

    private fun CoroutineScope.connect(peripheral: SensorTag) {
        launch {
            try {
                peripheral.connect()
            } catch (e: ConnectionLostException) {

            }
        }
    }
}