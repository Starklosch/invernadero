@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starklosch.invernadero.Setting.*
import com.starklosch.invernadero.extensions.*
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import java.util.*
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

class SettingActivity : ComponentActivity() {
    var setting: Setting? = null
    var error: Float = 0f
    var value: Float = 0.5f
    var minutes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.parcelable<Setting>(EXTRA_SETTING)?.let {
            setting = it
            val isLight = it is Light
            if (isLight)
                minutes = (it as Light).minutes.toInt()

            val min = fromReadable(it.min.toUShort(), isLight)
            val max = fromReadable(it.max.toUShort(), isLight)
            value = (min + max) / 2f
            //   Toast.makeText(this, "$min (${it.min}) - $max (${it.max}): $value", Toast.LENGTH_SHORT).show()
        }
        intent?.getShortExtra(EXTRA_ERROR, 0)?.let {
            error = fromReadable(it, setting is Light)
        }

        setContent {
            InvernaderoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main()
                }
            }
        }
    }

    companion object {
        const val EXTRA_SETTING = "setting"
        const val EXTRA_ERROR = "error"
    }
}

@Composable
private fun Main() {
    Scaffold(
        topBar = { TopBar() }
    )
    { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Display()
        }
    }
}

@Composable
private fun TopBar() {
    val activity = LocalContext.current as SettingActivity
    val title = activity.setting?.string() ?: "Setting"
    TopAppBar(title = { Text(text = title) })
}

@Composable
private fun Display() {
    val activity = LocalContext.current as SettingActivity
    var pos by remember { mutableStateOf(activity.value) }
    val isLight = activity.setting is Light

    var hours: Int by remember { mutableStateOf(activity.minutes / 60) }
    var minutes: Int by remember { mutableStateOf(activity.minutes % 60) }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item {
            Input(value = pos, onValueChange = { pos = it }, isLight)
            Slider(
                value = pos,
                onValueChange = { pos = it },
                modifier = Modifier.padding(16.dp)
            )
        }

        if (isLight) {
//            val duration = hours.hours + minutes.minutes
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    stringResource(R.string.light_time),
                    fontSize = 20.sp
                )
//            Text(duration.toString(), fontSize = 18.sp, modifier = Modifier.padding(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.widthIn(max = 120.dp).weight(1f, false)) {
                        val hoursText =
                            stringResource(R.string.hours).replaceFirstChar { it.uppercase() }
                        Text(hoursText)
                        NumericInput(hours, onValueChange = { hours = it })
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    Column(modifier = Modifier.widthIn(max = 120.dp).weight(1f, false)) {
                        val minutesText =
                            stringResource(R.string.minutes).replaceFirstChar { it.uppercase() }
                        Text(minutesText)
                        NumericInput(minutes, onValueChange = { minutes = it })
                    }
                }
            }
        }

        item {
            Button(onClick = {
                val offset = activity.error
                val min = toReadable(pos - offset, isLight).toShort()
                val max = toReadable(pos + offset, isLight).toShort()
                val newSetting = when (activity.setting) {
                    is Light -> Light(min, max, (minutes + hours * 60).toShort())
                    is Temperature -> Temperature(min, max)
                    is Humidity -> Humidity(min, max)
                    is SoilHumidity -> SoilHumidity(min, max)
                    else -> {
                        Invalid()
                    }
                }

                val intent = Intent()
                intent.putExtra(SettingActivity.EXTRA_SETTING, newSetting)
                activity.setResult(Activity.RESULT_OK, intent)
                activity.finish()
            }) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

@Composable
private fun Input(value: Float, onValueChange: (Float) -> Unit, exponential: Boolean) {
    val range = if (exponential) 1f..65535f else 0f..100f
    val default: Int = if (exponential) 1 else 0
    TextField(
        value = "${toReadable(value, exponential)}",
        onValueChange = {
            val intValue = it.toIntOrNull() ?: default
            val reversed = fromReadable(intValue, exponential)
            onValueChange(reversed.coerceIn(range))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun NumericInput(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = "$value",
        onValueChange = {
            val intValue = it.toIntOrNull() ?: 0
            onValueChange(intValue.coerceAtLeast(0))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

// value 0-1
fun toReadable(value: Float, exponential: Boolean): Int {
    val inRange = value.coerceIn(0f..1f)
    if (exponential)
        return 65535f.pow(inRange).roundToInt()
    return (inRange * 100f).roundToInt()
}

fun fromReadable(value: Int, exponential: Boolean): Float {
    // val range = if (exponential) 0f..65535f else 0f..100f
    val inRange = value.toFloat()//.coerceIn(range)
    if (exponential)
        return log(inRange, 65535f).coerceIn(0f..1f)
    return value.div(100f).coerceIn(0f..1f)
}

fun fromReadable(value: Short, exponential: Boolean) =
    fromReadable(value.toInt(), exponential)

fun fromReadable(value: UShort, exponential: Boolean) =
    fromReadable(value.toInt(), exponential)