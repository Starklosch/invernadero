package com.starklosch.invernadero

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class BluetoothModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val bluetoothManager: BluetoothManager =
        app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter = bluetoothManager.adapter
    private val _isCompatible: MutableStateFlow<Boolean> =
        MutableStateFlow(bluetoothManager.adapter == null)
    private val _isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(adapter.isEnabled)
    private val _isDiscovering: MutableStateFlow<Boolean> = MutableStateFlow(adapter.isEnabled)

    private val _discoveredDevices: MutableStateFlow<MutableList<BluetoothDevice>> =
        MutableStateFlow(
            mutableListOf()
        )

    private val intentFilters: Array<String> = arrayOf(
        BluetoothAdapter.ACTION_STATE_CHANGED,
        BluetoothAdapter.ACTION_DISCOVERY_STARTED,
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED
    )

    fun startDiscovery(): Boolean {
        return adapter.startDiscovery()
    }

    fun stopDiscovery(): Boolean {
        return adapter.startDiscovery()
    }

    inner class BluetoothBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null)
                return

            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> stateChanged(intent)
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> setDiscovering(true)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> setDiscovering(false)
                BluetoothDevice.ACTION_FOUND -> deviceFound(intent)
            }
        }

        private fun stateChanged(intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> setEnabled(true)
                BluetoothAdapter.STATE_OFF -> setEnabled(false)
            }
        }

        private fun deviceFound(intent: Intent) {
            val device: BluetoothDevice =
                intent.parcelable(BluetoothDevice.EXTRA_DEVICE) ?: return

            if (device.name != null)
                Log.d("BLUETOOTH", "Found " + device.name)

            addDevice(device)
        }

        fun register() {
            intentFilters.forEach {
                getApplication<Application>().registerReceiver(
                    this,
                    IntentFilter(it)
                )
            }
        }

        fun unregister() {
            getApplication<Application>().unregisterReceiver(
                this
            )
        }
    }

    var isCompatible: StateFlow<Boolean> = _isCompatible.asStateFlow()
    var isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    var isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private fun setEnabled(value: Boolean) = _isEnabled.update { it.apply { value } }
    private fun setDiscovering(value: Boolean) = _isDiscovering.update { it.apply { value } }
    private fun addDevice(device: BluetoothDevice) {
        if (_discoveredDevices.value.contains(device))
            return
        _discoveredDevices.update { it.apply { add(device) } }
    }
}