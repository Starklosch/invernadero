package com.starklosch.invernadero

import java.nio.ByteBuffer
import java.nio.ByteOrder

typealias ArduinoInt = Short
typealias ArduinoFloat = Float

data class Settings(
    val expectedLightMinutes: ArduinoInt = 0,
    val minLight: ArduinoFloat = 0f,
    val maxLight: ArduinoFloat = 100f,
    val minHumidity: ArduinoFloat = 0f,
    val maxHumidity: ArduinoFloat = 100f,
    val minSoilMoisture: ArduinoFloat = 0f,
    val maxSoilMoisture: ArduinoFloat = 100f,
    val minTemperature: ArduinoFloat = 0f,
    val maxTemperature: ArduinoFloat = 100f
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
        buffer.putFloat(minTemperature)
        buffer.putFloat(maxTemperature)
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
                minTemperature = buffer.getFloat(26),
                maxTemperature = buffer.getFloat(30),
            )
        }

        const val bytes = 34
    }
}

data class Values(
    val light: ArduinoFloat = 0f,
    val humidity: ArduinoFloat = 0f,
    val soilMoisture: ArduinoFloat = 0f,
    val temperature: ArduinoFloat = 0f
) {

    fun copyToNonFinite(other: Values): Values {
        val newLight = light.ifNotFinite(other.light)
        val newHumidity = humidity.ifNotFinite(other.humidity)
        val newSoilMoisture = soilMoisture.ifNotFinite(other.soilMoisture)
        val newTemperature = temperature.ifNotFinite(other.temperature)
        return Values(newLight, newHumidity, newSoilMoisture, newTemperature)
    }

    companion object {
        fun fromByteArray(array: ByteArray): Values {
            val buffer = wrap(array)
            return Values(
                light = buffer.getFloat(0),
                humidity = buffer.getFloat(4),
                soilMoisture = buffer.getFloat(8),
                temperature = buffer.getFloat(12)
            )
        }

        const val bytes = 16
    }
}

data class Information(
    val lightError: ArduinoInt = 0,
    val humidityError: ArduinoInt = 0,
    val soilMoistureError: ArduinoInt = 0,
    val temperatureError: ArduinoInt = 0
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