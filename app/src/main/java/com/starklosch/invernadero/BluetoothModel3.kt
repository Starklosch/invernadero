package com.starklosch.invernadero

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*

@SuppressLint("MissingPermission")
class BluetoothModel3(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val bluetoothManager: BluetoothManager =
        app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter = bluetoothManager.adapter

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    val advertisements: Flow<BluetoothDevice> = callbackFlow {
        val scanner = adapter.bluetoothLeScanner ?: throw Exception("Bluetooth disabled")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySendBlocking(result.device)
                    .onFailure {
                        Log.w("BLUETOOTH", "Unable to deliver scan result due to failure in flow or premature closing." )
                    }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                runCatching {
                    results.forEach {
                        trySendBlocking(it.device).getOrThrow()
                    }
                }.onFailure {
                    Log.w("BLUETOOTH", "Unable to deliver batch scan results due to failure in flow or premature closing." )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLUETOOTH", "Scan could not be started, error code $errorCode." )
                cancel("Bluetooth scan failed with error code: $errorCode")
            }
        }

        adapter.bluetoothLeScanner.startScan(callback)

        awaitClose {
            // Can't check BLE state here, only Bluetooth, but should assume `IllegalStateException` means BLE has been disabled.
            try {
                adapter.bluetoothLeScanner.stopScan(callback)
            } catch (e: IllegalStateException) {
                Log.w("BLUETOOTH", "Failed to stop scan. ")
            }
        }
    }

    private val _isCompatible: MutableStateFlow<Boolean> =
        MutableStateFlow(bluetoothManager.adapter == null)
    private val _isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(adapter.isEnabled)
    private val _isDiscovering: MutableStateFlow<Boolean> = MutableStateFlow(adapter.isEnabled)

    private val intentFilters: Array<String> = arrayOf(
        BluetoothAdapter.ACTION_STATE_CHANGED,
    )

    inner class BluetoothBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null)
                return

            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> stateChanged(intent)
            }
        }

        private fun stateChanged(intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> setEnabled(true)
                BluetoothAdapter.STATE_OFF -> setEnabled(false)
            }
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
//    private fun addDevice(device: BluetoothDevice) {
//        if (_discoveredDevices.value.contains(device))
//            return
//        _discoveredDevices.update { it.apply { add(device) } }
//    }

    init {
        BluetoothBroadcastReceiver().register()
        Log.d("BLUETOOTH", "INIT")
    }

//    fun start() {
//        if (_status.value == ScanStatus.Scanning) return // Scan already in progress.
//        _status.value = ScanStatus.Scanning
//
//        scanScope.launch {
//            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
//                scanner
//                    .advertisements
//                    .catch { cause -> _status.value = ScanStatus.Failed(cause.message ?: "Unknown error") }
//                    .onCompletion { cause -> if (cause == null || cause is CancellationException) _status.value =
//                        ScanStatus.Stopped
//                    }
//                    .collect { advertisement ->
//                        found[advertisement.address] = advertisement
//                        _advertisements.value = found.values.toList()
//                    }
//            }
//        }
//    }
//
//    fun stop() {
//        scanScope.cancelChildren()
//    }
//
//    fun clear() {
//        stop()
//        _advertisements.value = emptyList()
//    }
}