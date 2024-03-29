package com.starklosch.invernadero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Advertisement
import com.starklosch.invernadero.ScanStatus.Scanning
import com.starklosch.invernadero.ScanStatus.Stopped
import com.starklosch.invernadero.extensions.cancelChildren
import com.starklosch.invernadero.extensions.childScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

private val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)

sealed class ScanStatus {
    object Stopped : ScanStatus()
    object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}

class DeviceSelectionViewModel : ViewModel() {

    private val scanScope = viewModelScope.childScope()
    private val found = hashMapOf<String, Advertisement>()

    private val _scanStatus = MutableStateFlow<ScanStatus>(Stopped)
    val scanStatus = _scanStatus.asStateFlow()

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements = _advertisements.asStateFlow()

    fun startScanning() {
        if (_scanStatus.value == Scanning) return // Scan already in progress.
        _scanStatus.value = Scanning
        clear()

        scanScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> _scanStatus.value =
                        ScanStatus.Failed(cause.message ?: "Unknown reason")
                    }
                    .onCompletion { cause -> if (cause == null || cause is CancellationException) _scanStatus.value = Stopped }
                    .collect { advertisement ->
                        found[advertisement.address] = advertisement
                        _advertisements.value = found.values.toList()
                    }
            }
        }
    }

    fun stopScanning() {
        scanScope.cancelChildren()
    }

    fun clear() {
        _advertisements.value = emptyList()
    }
}
