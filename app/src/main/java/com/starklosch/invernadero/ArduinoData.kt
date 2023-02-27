package com.starklosch.invernadero

import java.nio.ByteBuffer
import java.nio.ByteOrder

typealias ArduinoInt = Short
typealias ArduinoFloat = Float

enum class OperationType(val id: Byte) {
    ReadValues('V'.code.toByte()),
    ReadSettings('S'.code.toByte()),
    ReadInformation('I'.code.toByte()),
    SetSettings('W'.code.toByte());

    companion object {
        fun fromByte(value: Byte) = OperationType.values().first { it.id == value }
    }
}

class ReadSettingsOperation {
    private val data: ByteArray = byteArrayOf()
    val settings : Settings
        get() = Settings.fromByteArray(data)
}


class Operation(val operationType: OperationType, private val data: ByteArray = byteArrayOf()) {
    fun toByteArray(): ByteArray {
        val buffer = allocate(1 + data.size)
        buffer.put(operationType.id)
        if (data.isNotEmpty())
            buffer.put(data)
        return buffer.array()
    }

    val settings: Settings
        get() {
            if (operationType == OperationType.ReadSettings)
                throw Exception("Cannot get Configuration from a \"GetInformation\" operation")

            if (data.size < Settings.bytes)
                throw Exception("Too few bytes")

            return Settings.fromByteArray(data)
        }

    val values: Values
        get() {
            if (operationType != OperationType.ReadValues)
                throw Exception("Cannot get Information from a \"$operationType\" operation")

            if (data.size < Values.bytes)
                throw Exception("Too few bytes")

            return Values.fromByteArray(data)
        }

    companion object {
        fun getConfiguration(): Operation {
            return Operation(operationType = OperationType.ReadSettings)
        }

        fun setConfiguration(settings: Settings): Operation {
            return Operation(
                operationType = OperationType.SetSettings,
                settings.toByteArray()
            )
        }

        fun getInformation(): Operation {
            return Operation(operationType = OperationType.ReadValues)
        }

        fun fromByteArray(array: ByteArray): Operation {
            val buffer = wrap(array)
            val operationType = buffer.get()
            val size = buffer.remaining()
            val data = ByteArray(size)
            buffer.get(data)
            return Operation(OperationType.fromByte(operationType), data)
        }
    }
}

data class Settings(
    val expectedLightMinutes: ArduinoInt = 0,
    val minLight: ArduinoFloat = 0f,
    val maxLight: ArduinoFloat = 100f,
    val minHumidity: ArduinoFloat = 0f,
    val maxHumidity: ArduinoFloat = 100f,
    val minSoilMoisture: ArduinoFloat = 0f,
    val maxSoilMoisture: ArduinoFloat = 100f,
    val minTemperatureInCelsius: ArduinoFloat = 0f,
    val maxTemperatureInCelsius: ArduinoFloat = 100f
) {
    fun toByteArray(): ByteArray {
        val buffer = allocate(bytes)
        buffer.putShort(expectedLightMinutes)
        buffer.putFloat(minLight)
        buffer.putFloat(maxLight)
        buffer.putFloat(minHumidity)
        buffer.putFloat(maxHumidity)
        buffer.putFloat(minSoilMoisture)
        buffer.putFloat(maxSoilMoisture)
        buffer.putFloat(minTemperatureInCelsius)
        buffer.putFloat(maxTemperatureInCelsius)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(array: ByteArray): Settings {
            val buffer = wrap(array)
            return Settings(
                expectedLightMinutes = buffer.getShort(0),
                minLight = buffer.getFloat(2),
                maxLight = buffer.getFloat(6),
                minHumidity = buffer.getFloat(10),
                maxHumidity = buffer.getFloat(14),
                minSoilMoisture = buffer.getFloat(18),
                maxSoilMoisture = buffer.getFloat(22),
                minTemperatureInCelsius = buffer.getFloat(26),
                maxTemperatureInCelsius = buffer.getFloat(30),
            )
        }

        const val bytes = 34
    }
}

data class Values(
    val light: ArduinoFloat = 0f,
    val humidity: ArduinoFloat = 0f,
    val soilMoisture: ArduinoFloat = 0f,
    val temperatureInCelsius: ArduinoFloat = 0f
) {
    companion object {
        fun fromByteArray(array: ByteArray): Values {
            val buffer = wrap(array)
            return Values(
                light = buffer.getFloat(0),
                humidity = buffer.getFloat(4),
                soilMoisture = buffer.getFloat(8),
                temperatureInCelsius = buffer.getFloat(12)
            )
        }

        const val bytes = 16
    }
}

data class Information(
    val lightError: ArduinoInt,
    val humidityError: ArduinoInt,
    val soilMoistureError: ArduinoInt,
    val temperatureError: ArduinoInt
) {

    companion object {
        fun fromByteArray(array: ByteArray): Information {
            val buffer = wrap(array)
            return Information(
                lightError = buffer.getShort(0),
                humidityError = buffer.getShort(2),
                soilMoistureError = buffer.getShort(4),
                temperatureError = buffer.getShort(6)
            )
        }

        const val bytes = 8
    }
}

private fun allocate(bytes: Int): ByteBuffer {
    return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)
}

private fun wrap(array: ByteArray): ByteBuffer {
    return ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)
}