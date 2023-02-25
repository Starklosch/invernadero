// https://github.com/JuulLabs/sensortag/blob/7078e3775010dfaae81be129cbdc8450ddc822d6/app/src/commonMain/kotlin/SensorTag.kt
package com.starklosch.invernadero

import android.bluetooth.le.ScanSettings
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType.WithoutResponse
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging.Level.Events
import kotlinx.coroutines.flow.map

private const val GYRO_MULTIPLIER = 500f / 65536f

private val serviceUuid = uuidFrom("0000ffe0-0000-1000-8000-00805f9b34fb")
private val dataUuid = uuidFrom("0000ffe1-0000-1000-8000-00805f9b34fb")

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
//    filters = listOf(Filter.Service(uuidFrom(sensorTagUuid)))
//    filters = listOf(
//        Filter.Service(uuidFrom("0000aa80-0000-1000-8000-00805f9b34fb")), // SensorTag
//    )
}


val services = listOf(
    serviceUuid,
    dataUuid
)

class SensorTag(
    private val peripheral: Peripheral
) : Peripheral by peripheral {

    val rawData = peripheral
        .observe(characteristic).map {
            Operation.fromByteArray(it)
        }
//
//    var expectedLightMinutes: Short = 0
//        private set
//    var minLight: Float = 0f
//        private set
//    var maxLight: Float = 100f
//        private set
//    var minHumidity: Float = 0f
//        private set
//    var maxHumidity: Float = 100f
//        private set
//    var minSoilMoisture: Float = 0f
//        private set
//    var maxSoilMoisture: Float = 100f
//        private set
//    var minTemperatureInCelsius: Float = 0f
//        private set
//    var maxTemperatureInCelsius: Float = 100f
//        private set

    /** Set period, allowable range is 100-2550 ms. */
    suspend fun write(operation: Operation) {
        val data = operation.toByteArray()
//        val ser = peripheral.services
//        ser?.forEach {
//            Log.d("BLUETOOTH", "Service: " + it.serviceUuid)
//            it.characteristics.forEach {
//                Log.d("BLUETOOTH", "Char: " + it.characteristicUuid)
//                Log.d("BLUETOOTH", "Char: " + it.properties)
//            }
//        }
        peripheral.write(characteristic, data, WithoutResponse)
    }

    /** Period (in milliseconds) within the range 100-2550 ms. */
    suspend fun read(): Operation {
        val value = peripheral.read(characteristic)
        return Operation.fromByteArray(value)
    }
}

private fun characteristicOf(service: Uuid, characteristic: Uuid) =
    characteristicOf(service.toString(), characteristic.toString())