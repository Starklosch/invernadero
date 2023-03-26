package com.starklosch.invernadero

//import androidx.compose.ui.res.*
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.starklosch.invernadero.Setting.*
import com.starklosch.invernadero.extensions.ifNegative
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class Setting(@StringRes val id: Int, open val min: Short, open val max: Short) : Parcelable {
    @Composable
    fun string() : String = androidx.compose.ui.res.stringResource(id)

    @Parcelize
    object Invalid : Setting(0, 0, 0)

    @Parcelize
    class Temperature(override val min: Short, override val max: Short) : Setting(R.string.temperature, min, max)

    @Parcelize
    class Light(override val min: Short, override val max: Short, val minutes: Short) : Setting(R.string.light, min, max)

    @Parcelize
    class Humidity(override val min: Short, override val max: Short) : Setting(R.string.humidity, min, max)

    @Parcelize
    class SoilHumidity(override val min: Short, override val max: Short) : Setting(R.string.soil_humidity, min, max)
}

@Suppress("PROPERTY_WONT_BE_SERIALIZED")
@Parcelize
data class Settings(
    val expectedLightMinutes: Short = 0,
    val minLight: Short = 0,
    val maxLight: Short = 0,
    val minHumidity: Short = 0,
    val maxHumidity: Short = 0,
    val minSoilHumidity: Short = 0,
    val maxSoilHumidity: Short = 0,
    val minTemperature: Short = 0,
    val maxTemperature: Short = 0
) : Parcelable {
    val light = Light(minLight, maxLight, expectedLightMinutes)
    val temperature = Temperature(minTemperature, maxTemperature)
    val humidity = Humidity(minHumidity, maxHumidity)
    val soilHumidity = SoilHumidity(minSoilHumidity, maxSoilHumidity)

    fun toByteArray(): ByteArray = with(allocate(bytes)){
        putShort(expectedLightMinutes)
        putShort(minLight)
        putShort(maxLight)
        putShort(minHumidity)
        putShort(maxHumidity)
        putShort(minSoilHumidity)
        putShort(maxSoilHumidity)
        putShort(minTemperature)
        putShort(maxTemperature)
    }.array()

    companion object {
        fun fromByteArray(array: ByteArray): Settings {
            val buffer = wrap(array)
            return Settings(
                expectedLightMinutes = buffer.short,
                minLight = buffer.short,
                maxLight = buffer.short,
                minHumidity = buffer.short,
                maxHumidity = buffer.short,
                minSoilHumidity = buffer.short,
                maxSoilHumidity = buffer.short,
                minTemperature = buffer.short,
                maxTemperature = buffer.short
            )
        }

        const val bytes = 18
    }
}

@Parcelize
data class Values(
    val light: Short = 0,
    val humidity: Short = 0,
    val soilHumidity: Short = 0,
    val temperature: Short = 0
) : Parcelable {

    fun ifNegative(other: Values): Values {
        val newLight = light.ifNegative(other.light)
        val newHumidity = humidity.ifNegative(other.humidity)
        val newSoilHumidity = soilHumidity.ifNegative(other.soilHumidity)
        val newTemperature = temperature.ifNegative(other.temperature)
        return Values(newLight, newHumidity, newSoilHumidity, newTemperature)
    }

    companion object {
        fun fromByteArray(array: ByteArray): Values {
            val buffer = wrap(array)
            return Values(
                light = buffer.short,
                humidity = buffer.short,
                soilHumidity = buffer.short,
                temperature = buffer.short
            )
        }

        const val bytes = 8
    }
}

@Parcelize
data class Information(
    val lightError: Short = 0,
    val humidityError: Short = 0,
    val soilHumidityError: Short = 0,
    val temperatureError: Short = 0
) : Parcelable {

    companion object {
        fun fromByteArray(array: ByteArray): Information {
            val buffer = wrap(array)
            return Information(
                lightError = buffer.short,
                humidityError = buffer.short,
                soilHumidityError = buffer.short,
                temperatureError = buffer.short
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