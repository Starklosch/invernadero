@file:OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)

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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import com.starklosch.invernadero.extensions.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

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

private val valueRefreshTime = 5.seconds

@Composable
private fun AppContent(viewModel: MainActivityViewModel = viewModel()) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val values by viewModel.values.collectAsStateWithLifecycle()
    val information by viewModel.information.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(state) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (state is State.Connected) {
                viewModel.updateValues()
                delay(valueRefreshTime)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StateIndicator(state, deviceName)
        Information(
            connected = state is State.Connected,
            values = values,
            information = information,
            settings = settings,
            onSettingsChanged = {
                viewModel.setSettings(it)
            }
        )
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
    
    val string = stringResource(resourceId)
    val start = string.indexOf('%')
    if (start >= 0){
        val spanStyles = listOf(
            AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold),
            start = start,
            end = start + name.length
        ))
        Text(AnnotatedString(string.format(name), spanStyles))
    } else {
        Text(string.format(name))
    }
}

@Composable
private fun Information(connected: Boolean, values: Values, information: Information, settings: Settings, onSettingsChanged: (Settings) -> Unit, modifier: Modifier = Modifier) {
    val temperature = values.temperature
    val humidity = values.humidity
    val soilMoisture = values.soilMoisture
    val light = values.light

    val activity = LocalContext.current as Activity
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val setting : Setting? = result.data?.parcelable(SettingActivity.EXTRA_SETTING)
                
                if (setting == null || setting is Setting.Invalid)
                    return@rememberLauncherForActivityResult

                val newSettings = when (setting) {
                    is Setting.Light ->
                       settings.copy(minLight = setting.min, maxLight = setting.max, expectedLightMinutes = setting.minutes.toShort())
                    is Setting.Temperature -> settings.copy(minTemperature = setting.min, maxTemperature = setting.max)
                    is Setting.Humidity -> settings.copy(minHumidity = setting.min, maxHumidity = setting.max)
                    is Setting.SoilHumidity -> settings.copy(minSoilMoisture = setting.min, maxSoilMoisture = setting.max)
                    else -> Settings()
                }
                
                onSettingsChanged(newSettings)
               // Toast.makeText(activity, "Min ${setting.min.toUShort()}, max ${setting.max.toUShort()}", Toast.LENGTH_SHORT).show()
            }
        }

    val launch = { setting : Setting, error : Short ->
        if (connected) {
            val intent = Intent(activity, SettingActivity::class.java)
            intent.putExtra(SettingActivity.EXTRA_SETTING, setting)
            intent.putExtra(SettingActivity.EXTRA_ERROR, error)
            launcher.launch(intent)
        }
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
                content = "${temperature}º",
                icon = R.drawable.thermometer,
                fontSize = 28.sp,
                onClick = { launch(Setting.Temperature(settings.minTemperature, settings.maxTemperature), information.temperatureError) }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = "${humidity}%",
                icon = R.drawable.water_droplet,
                fontSize = 28.sp,
                onClick = { launch(Setting.Humidity(settings.minHumidity, settings.maxHumidity), information.humidityError) }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
        ) {
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = "${soilMoisture}%",
                icon = R.drawable.moisture_soil,
                fontSize = 28.sp,
                onClick = { launch(Setting.SoilHumidity(settings.minSoilMoisture, settings.maxSoilMoisture), information.soilMoistureError) }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = Measurement(light.toInt(), "lx").format(),
                icon = R.drawable.light,
                fontSize = 28.sp,
                onClick = { launch(Setting.Light(settings.minLight, settings.maxLight, settings.expectedLightMinutes), information.lightError) }
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