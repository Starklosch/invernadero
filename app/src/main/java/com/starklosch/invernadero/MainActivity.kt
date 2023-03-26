@file:OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@file:Suppress("FunctionName")

package com.starklosch.invernadero

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import com.starklosch.invernadero.extensions.parcelable
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import com.starklosch.invernadero.ui.theme.iconModifier
import kotlinx.coroutines.delay
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
private fun Main(viewModel: MainActivityViewModel = viewModel()) {
    val values by viewModel.values.collectAsStateWithLifecycle()
    val information by viewModel.information.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBar(information, settings, values) }
    )
    { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppContent(viewModel, values, information, settings)
        }
    }

}

private val valueRefreshTime = 5.seconds

@Composable
private fun AppContent(
    viewModel: MainActivityViewModel,
    values: Values,
    information: Information,
    settings: Settings
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()

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
    val name = deviceName.ifEmpty { stringResource(R.string.unknown_device) }
    val resourceId = when (state) {
        is State.Connecting -> R.string.connecting_to
        is State.Connected -> R.string.connected_to
        is State.Disconnecting -> R.string.disconnecting_from
        is State.Disconnected -> R.string.disconnected
    }

    val string = stringResource(resourceId)
    val formatted = string.format(name)
    val start = string.indexOf('%')
    if (start < 0) {
        Text(formatted)
    } else {
        val spanStyles = listOf(
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Bold),
                start = start,
                end = start + name.length
            )
        )
        Text(AnnotatedString(formatted, spanStyles))
    }
}

@Composable
private fun Information(
    connected: Boolean,
    values: Values,
    information: Information,
    settings: Settings,
    onSettingsChanged: (Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    val temperature = values.temperature
    val humidity = values.humidity
    val soilHumidity = values.soilHumidity
    val light = values.light

    val activity = LocalContext.current as Activity
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val setting: Setting? = result.data?.parcelable(SettingActivity.EXTRA_SETTING)

                if (setting == null || setting is Setting.Invalid)
                    return@rememberLauncherForActivityResult

                val newSettings = when (setting) {
                    is Setting.Light ->
                        settings.copy(
                            minLight = setting.min,
                            maxLight = setting.max,
                            expectedLightMinutes = setting.minutes
                        )
                    is Setting.Temperature -> settings.copy(
                        minTemperature = setting.min,
                        maxTemperature = setting.max
                    )
                    is Setting.Humidity -> settings.copy(
                        minHumidity = setting.min,
                        maxHumidity = setting.max
                    )
                    is Setting.SoilHumidity -> settings.copy(
                        minSoilHumidity = setting.min,
                        maxSoilHumidity = setting.max
                    )
                    else -> Settings()
                }

                onSettingsChanged(newSettings)
            }
        }

    val launch = { setting: Setting, error: Short ->
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
                onClick = {
                    launch(
                        settings.temperature, information.temperatureError
                    )
                }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = "${humidity}%",
                icon = R.drawable.water_droplet,
                fontSize = 28.sp,
                onClick = {
                    launch(
                        settings.humidity,
                        information.humidityError
                    )
                }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
        ) {
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = "${soilHumidity}%",
                icon = R.drawable.soil_humidity,
                fontSize = 28.sp,
                onClick = {
                    launch(
                        settings.soilHumidity, information.soilHumidityError
                    )
                }
            )
            Sensor(
                modifier = Modifier.fillMaxWidth(),
                content = Measurement(light.toInt(), "lx").format(),
                icon = R.drawable.light,
                fontSize = 28.sp,
                onClick = {
                    launch(
                        settings.light, information.lightError
                    )
                }
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
private fun TopBar(information: Information, settings: Settings, values: Values) {
    val context = LocalContext.current
    TopAppBar(title = { Text(text = stringResource(R.string.app_name)) }, actions = {
        if (BuildConfig.DEBUG) {
            IconButton(onClick = {
                val intent = Intent(context, DebugActivity::class.java)
                intent.putExtra(DebugActivity.EXTRA_INFO, information)
                intent.putExtra(DebugActivity.EXTRA_SETTINGS, settings)
                intent.putExtra(DebugActivity.EXTRA_VALUES, values)
                context.startActivity(intent)
            }) {
                Icon(painterResource(R.drawable.bug), contentDescription = "Debug", modifier = iconModifier)
            }
        }
    })
}