@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlinx.coroutines.delay
import kotlin.math.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            AppContent()
        }
    }

}

@Composable
private fun AppContent(viewModel: MainActivityViewModel = viewModel()) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val values by viewModel.values.collectAsStateWithLifecycle()
    val information by viewModel.information.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(state) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (state is State.Connected) {
                viewModel.updateValues()
                delay(1500)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StateIndicator(state, deviceName)
        Information(values = values, information = information)
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            ConnectionButton(
                state = state,
                onDeviceSelected = { viewModel.connect(it) },
                onDisconnect = viewModel::disconnect
            )
        }
    }
}

@Composable
private fun StateIndicator(state: State, deviceName: String) {
    val name = if (deviceName.isEmpty()) stringResource(R.string.unknown_device) else deviceName
    val resourceId = when (state) {
        is State.Connecting -> R.string.connecting_to
        is State.Connected -> R.string.connected_to
        is State.Disconnecting -> R.string.disconnecting_from
        is State.Disconnected -> R.string.disconnected
    }

    Text(stringResource(resourceId, name))
}

@Composable
private fun Information(values: Values, information: Information, modifier: Modifier = Modifier) {
    val temperature = values.temperature
    val humidity = values.humidity
    val soilMoisture = values.soilMoisture
    val light = values.light

    val activity = LocalContext.current as Activity
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val settingName = result.data?.getStringExtra(SettingActivity.EXTRA_SETTING)
                val setting = Setting.fromName(settingName)
                val min = result.data?.getIntExtra(SettingActivity.EXTRA_MIN, -1)
                val max = result.data?.getIntExtra(SettingActivity.EXTRA_MAX, -1)

                val settings = when (setting) {
                    Light -> Setting(minLight = min, maxLight = max),
                    Temperature -> Setting(minTemperature = min, maxTemperature = max),
                    Temperature -> Setting(minHumidity = min, maxHumidity = max),
                    Temperature -> Setting(minSoilMoisture = min, maxSoilMoisture = max)
                }
                Toast.makeText(activity, "$setting min $min, max $max", Toast.LENGTH_SHORT).show()
            }
        }

    val launch = { setting : Setting, error : Short ->
        val intent = Intent(activity, SettingActivity::class.java)
         intent.putExtra(SettingActivity.EXTRA_SETTING, setting.name)
         intent.putExtra(SettingActivity.EXTRA_ERROR, error)
        launcher.launch(intent)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp).widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, false)
        ) {
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.temperature),
                content = "${temperature}º",
                icon = R.drawable.thermometer,
                fontSize = 28.sp,
                onClick = { launch(Temperature, information.temperatureError) }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.humidity),
                content = "${humidity}%",
                icon = R.drawable.water_droplet,
                fontSize = 28.sp,
                onClick = { launch(Humidity, information.humidityError) }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
        ) {
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.soilHumidity),
                content = "${soilMoisture}%",
                icon = R.drawable.moisture_soil,
                fontSize = 28.sp,
                onClick = { launch(SoilHumidity, information.soilMoistureError) }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.light),
                content = Measurement(light.toInt(), "lx").format(),
                icon = R.drawable.light,
                fontSize = 28.sp,
                onClick = { launch(Light, information.lightError) }
            )
        }
    }
}

@Composable
private fun ConnectionButton(
    state: State,
    onDeviceSelected: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val address = result.data?.getStringExtra("device")
                if (address != null)
                    onDeviceSelected(address)
            }
        }

    val text = when (state) {
        is State.Connected -> stringResource(R.string.disconnect)
        is State.Connecting -> stringResource(R.string.connecting)
        is State.Disconnecting -> stringResource(R.string.disconnecting)
        is State.Disconnected -> stringResource(R.string.select_device)
    }

    val selectDevice = {
        val intent = Intent(context, DeviceSelectionActivity::class.java)
        launcher.launch(intent)
    }

    val action: () -> Unit = when (state) {
        is State.Connected,
        is State.Connecting -> onDisconnect
        is State.Disconnecting,
        is State.Disconnected -> selectDevice
    }

    val enabled = state is State.Connected || state is State.Disconnected

    Button(onClick = action, enabled = enabled) {
        Text(text)
    }
}

@Composable
private fun Sensor(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    title: String = "Test",
    @DrawableRes icon: Int = R.drawable.water_droplet,
    content: String = "100%",
    fontSize: TextUnit = 28.sp
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .then(modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = content,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TopBar() {
    val context = LocalContext.current
    TopAppBar(title = { Text(text = context.resources.getString(R.string.app_name)) })
}

private fun format(value: Float, unit: String): String {
    var _value = value
    var _unit = unit
    if (value > 1000) {
        _value = value / 1000
        _unit = "k" + unit
    }

    return "${_value.roundToInt()} $_unit"
}

private fun format(value: Short, unit: String): String = format(value.toFloat(), unit)

enum class Setting(val name : String){
    Temperature("temperature"),
    Light("light"),
    Humidity("humidity"),
    SoilHumidity("soilHumidity");
    
    companion object {
        fun fromName(value: String) = Setting.values().first { it.name == value }
    }
}