// https://github.com/JuulLabs/sensortag/blob/7078e3775010dfaae81be129cbdc8450ddc822d6/app/src/commonMain/kotlin/SensorTag.kt
package com.starklosch.invernadero

import android.bluetooth.le.ScanSettings
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType.WithoutResponse
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging.Level.Events
import com.starklosch.invernadero.Response.InvalidResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb"
private const val dataUuid = "0000ffe1-0000-1000-8000-00805f9b34fb"

private val characteristic = characteristicOf(
    service = serviceUuid,
    characteristic = dataUuid,
)

val scanner = Scanner {
    ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    logging {
        level = Events
    }
}

class ArduinoPeripheral(
    private val peripheral: Peripheral
) : Peripheral by peripheral {

    val operations = peripheral
        .observe(characteristic).map {
            val firstByte = it.firstOrNull()
            val operationType = OperationType.fromByte(firstByte)
            when (operationType){
                OperationType.ReadValues -> Response.ValuesResponse(it.copyOfRange(1, it.size))
                OperationType.ReadSettings -> Response.SettingsResponse(it.copyOfRange(1, it.size))
                OperationType.ReadInformation -> Response.InformationResponse(
                    it.copyOfRange(
                        1,
                        it.size
                    )
                )
                else -> InvalidResponse
            }
        }

    private val mutex = Mutex()

    suspend fun request(request: Request) {
        mutex.withLock {
            val data = request.toByteArray()
            peripheral.write(characteristic, data, WithoutResponse)
            // wait between request
            delay(200)
        }
    }
}