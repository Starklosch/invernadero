@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import java.util.*
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

class SettingActivity : ComponentActivity() {
    var setting: String = ""
    var error: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("setting")?.let { setting = it }
        intent?.getShortExtra("error", 0)?.let { error = it.toFloat() / 100f }

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
            Settings()
        }
    }
}

@Composable
private fun TopBar() {
    val activity = LocalContext.current as SettingActivity
    TopAppBar(title = { Text(text = activity.setting) })
}

@Composable
private fun Settings() {
    val activity = LocalContext.current as SettingActivity
    var pos by remember { mutableStateOf(0.5f) }
    val isLight = activity.setting == "light"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Input(value = pos, onValueChange = { pos = it }, isLight)
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = pos,
            onValueChange = { pos = it },
            modifier = Modifier.padding(16.dp)
        )

        if (isLight) {
            var selectedTimeText by remember { mutableStateOf("") }

            val timePicker = TimePickerDialog(
                activity,
                { _, selectedHour: Int, selectedMinute: Int ->
                    selectedTimeText = "$selectedHour:$selectedMinute"
                }, 0, 0, false
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.time))
            Text(selectedTimeText)
            Button(onClick = { timePicker.show() }) {
                Text(stringResource(R.string.select_time))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val offset = if (isLight) 0.02f else activity.error.toFloat()
            val min = toReadable(pos - offset, isLight)
            val max = toReadable(pos + offset, isLight)

            val intent = Intent()
            intent.putExtra("setting", activity.setting)
            intent.putExtra("min", min)
            intent.putExtra("max", max)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }) {
            Text(stringResource(R.string.done))
        }
    }
}

@Composable
private fun Input(value: Float, onValueChange: (Float) -> Unit, exponential: Boolean) {
    val range = if (exponential) 0f..1000000f else 0f..100f
    val default = if (exponential) 1 else 0
    TextField(
        value = "${toReadable(value, exponential)}",
        onValueChange = {
            val intValue = it.toIntOrNull() ?: default
            val reversed = fromReadable(intValue, exponential)
            onValueChange(reversed.coerceIn(range))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}


fun toReadable(value: Float, exponential: Boolean): Int {
    if (exponential)
        return 1000000f.pow(value).roundToInt()
    return (value * 100f).roundToInt()
}

fun fromReadable(value: Int, exponential: Boolean): Float {
    if (exponential)
        return log(value.toFloat(), 1000000f)
    return value.toFloat() / 100f
}