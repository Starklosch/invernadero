package com.starklosch.invernadero

import java.nio.ByteBuffer
import java.nio.ByteOrder

typealias ArduinoInt = Short
typealias ArduinoFloat = Float

data class Settings(
    val expectedLightMinutes: ArduinoInt = 0,
    val lightIntensity : ArduinoInt = 0,
    val minLight: ArduinoInt = 0,
    val maxLight: ArduinoInt = 100,
    val minHumidity: ArduinoInt = 0,
    val maxHumidity: ArduinoInt = 100,
    val minSoilMoisture: ArduinoInt = 0,
    val maxSoilMoisture: ArduinoInt = 100,
    val minTemperature: ArduinoInt = 0,
    val maxTemperature: ArduinoInt = 100
) {
    fun toByteArray(): ByteArray {
        val buffer = allocate(bytes)
        buffer.putShort(expectedLightMinutes)
        buffer.putShort(lightIntensity)
        buffer.putShort(minLight)
        buffer.putShort(maxLight)
        buffer.putShort(minHumidity)
        buffer.putShort(maxHumidity)
        buffer.putShort(minSoilMoisture)
        buffer.putShort(maxSoilMoisture)
        buffer.putShort(minTemperature)
        buffer.putShort(maxTemperature)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(array: ByteArray): Settings {
            val buffer = wrap(array)
            return Settings(
                expectedLightMinutes = buffer.getShort(),
                lightIntensity = buffer.getShort(),
                minLight = buffer.getShort(),
                maxLight = buffer.getShort(),
                minHumidity = buffer.getShort(),
                maxHumidity = buffer.getShort(),
                minSoilMoisture = buffer.getShort(),
                maxSoilMoisture = buffer.getShort(),
                minTemperature = buffer.getShort(),
                maxTemperature = buffer.getShort()
            )
        }

        const val bytes = 20
    }
}

data class Values(
    val light: ArduinoInt = 0,
    val humidity: ArduinoInt = 0,
    val soilMoisture: ArduinoInt = 0,
    val temperature: ArduinoInt = 0
) {

    fun copyToNonFinite(other: Values): Values {
        val newLight = light.ifNegative(other.light)
        val newHumidity = humidity.ifNegative(other.humidity)
        val newSoilMoisture = soilMoisture.ifNegative(other.soilMoisture)
        val newTemperature = temperature.ifNegative(other.temperature)
        return Values(newLight, newHumidity, newSoilMoisture, newTemperature)
    }

    companion object {
        fun fromByteArray(array: ByteArray): Values {
            val buffer = wrap(array)
            return Values(
                light = buffer.getShort(),
                humidity = buffer.getShort(),
                soilMoisture = buffer.getShort(),
                temperature = buffer.getShort()
            )
        }

        const val bytes = 8
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
                lightError = buffer.getShort(),
                humidityError = buffer.getShort(),
                soilMoistureError = buffer.getShort(),
                temperatureError = buffer.getShort()
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