package com.starklosch.invernadero

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class BluetoothActivityModel : ViewModel() {

    private val _state = MutableStateFlow(BluetoothState())

    private val _discoveredDevices: MutableList<BluetoothDevice> = mutableStateListOf()

//    val discoveredDevices: List<BluetoothDevice> = _discoveredDevices

    fun addDiscoveredDevice(device: BluetoothDevice){
//        _state.value.discoveredDevices.
//        _discoveredDevices.value.add(device)
    }

//    private var state: MutableStateFlow<BluetoothState> =
}

data class BluetoothState(
    val discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
)