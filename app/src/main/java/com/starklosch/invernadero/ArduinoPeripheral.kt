// https://github.com/JuulLabs/sensortag/blob/7078e3775010dfaae81be129cbdc8450ddc822d6/app/src/commonMain/kotlin/SensorTag.kt
package com.starklosch.invernadero

import android.bluetooth.le.ScanSettings
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType.WithoutResponse
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging.Level.Events
import com.starklosch.invernadero.Response.*
import kotlinx.coroutines.flow.map

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
            if (it.isEmpty())
                return@map InvalidResponse()

            val firstByte = it.first()
            val operationType = OperationType.fromByte(firstByte)
            return@map when (operationType){
                OperationType.ReadValues -> ValuesResponse(it.copyOfRange(1, it.size))
                OperationType.ReadSettings -> SettingsResponse(it.copyOfRange(1, it.size))
                OperationType.ReadInformation -> InformationResponse(it.copyOfRange(1, it.size))
                else -> InvalidResponse()
            }
        }

    /** Set period, allowable range is 100-2550 ms. */
    suspend fun request(request: Request) {
        val data = request.toByteArray()
        peripheral.write(characteristic, data, WithoutResponse)
    }
}