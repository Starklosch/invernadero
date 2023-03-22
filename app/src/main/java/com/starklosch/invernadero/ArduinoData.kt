package com.starklosch.invernadero

//import androidx.compose.ui.res.*
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.starklosch.invernadero.extensions.ifNegative
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class Setting(@StringRes val id: Int, open val min: Short, open val max: Short) : Parcelable {
    @Composable
    fun string() : String = androidx.compose.ui.res.stringResource(id)
    
    @Parcelize
    class Invalid : Setting(0, -1, -1)

    @Parcelize
    class Temperature(override val min: Short, override val max: Short) : Setting(R.string.temperature, min, max)

    @Parcelize
    class Light(override val min: Short, override val max: Short, val minutes: Short) : Setting(R.string.light, min, max)

    @Parcelize
    class Humidity(override val min: Short, override val max: Short) : Setting(R.string.humidity, min, max)

    @Parcelize
    class SoilHumidity(override val min: Short, override val max: Short) : Setting(R.string.soil_humidity, min, max)
}
/*

enum class Setting(@StringRes val id : Int) {
    Light(R.string.light),
    Temperature(R.string.temperature),
    Humidity(R.string.humidity),
    SoilHumidity(R.string.soil_humidity);
    
    @Composable
    fun string() : String = androidx.compose.ui.res.stringResource(id)
   
    companion object {
        fun fromName(name: String) = Setting.values().firstOrNull { it.name == name }
    }
}*/

data class Settings(
    val expectedLightMinutes: Short = -1,
    val minLight: Short = 0,
    val maxLight: Short = 0,
    val minHumidity: Short = 0,
    val maxHumidity: Short = 0,
    val minSoilMoisture: Short = 0,
    val maxSoilMoisture: Short = 0,
    val minTemperature: Short = 0,
    val maxTemperature: Short = 0
) {
    //val light = Setting.Light(minLight, maxLight, expectedLightMinutes)
    //val temperature = Setting.Light(minTemperature, maxTemperature)
    //val humidity = Setting.Light(minHumidity, maxHumidity)
    //val soilHumidity = Setting.Light(minSoilHumidity, maxSoilHumidity)
    
    fun toByteArray(): ByteArray {
        val buffer = allocate(bytes)
        buffer.putShort(expectedLightMinutes)
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
    /*
    fun ifNegative(other: Settings): Settings {
        val newExpectedLightMinutes = expectedLightMinutes.ifNegative(other.expectedLightMinutes)
        val newMinLight = minLight.ifNegative(other.minLight)
        val newMaxLight = maxLight.ifNegative(other.maxLight)
        val newMinHumidity = minHumidity.ifNegative(other.minHumidity)
        val newMaxHumidity = maxHumidity.ifNegative(other.maxHumidity)
        val newMinSoilMoisture = minSoilMoisture.ifNegative(other.minSoilMoisture)
        val newMaxSoilMoisture = maxSoilMoisture.ifNegative(other.maxSoilMoisture)
        val newMinTemperature = minTemperature.ifNegative(other.minTemperature)
        val maxTemperature  = maxTemperature.ifNegative(other.maxTemperature)
        
        return Settings(
            newExpectedLightMinutes,
            newMinLight,
            newMaxLight,
            newMinHumidity,
            newMaxHumidity,
            newMinSoilMoisture,
            newMaxSoilMoisture,
            newMinTemperature,
            maxTemperature
        )
    }*/
    
    companion object {
        fun fromByteArray(array: ByteArray): Settings {
            val buffer = wrap(array)
            return Settings(
                expectedLightMinutes = buffer.getShort(),
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

        const val bytes = 18
    }
}

data class Values(
    val light: Short = 0,
    val humidity: Short = 0,
    val soilMoisture: Short = 0,
    val temperature: Short = 0
) {

    fun ifNegative(other: Values): Values {
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
    val lightError: Short = 0,
    val humidityError: Short = 0,
    val soilMoistureError: Short = 0,
    val temperatureError: Short = 0
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