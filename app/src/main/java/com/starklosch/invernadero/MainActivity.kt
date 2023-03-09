@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import android.app.Activity
import android.content.*
import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import android.widget.Toast
import androidx.compose.ui.text.style.*
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlinx.coroutines.delay
import kotlin.math.round
import kotlin.math.roundToInt

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

@Preview
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
        Information(values)
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
private fun Information(values: Values, modifier: Modifier = Modifier) {
    val temperature = values.temperature
    val humidity = values.humidity
    val soilMoisture = values.soilMoisture
    val light = values.light

    val activity = LocalContext.current as Activity
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val setting = result.data?.getStringExtra("setting")
                val min = result.data?.getIntExtra("min", 0)
                val max = result.data?.getIntExtra("max", 100)
                
                Toast.makeText(activity, "$setting min $min, max $max", Toast.LENGTH_SHORT).show()
            }
        }


    val columnMinWidth = 150.dp
    val columnMaxWidth = 240.dp
 /*   Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp).widthIn(max = 500.dp)
    ) {*/
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp).widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, false)
             //   .widthIn(max = columnMaxWidth)
        ) {
            Sensor(
                title = stringResource(R.string.temperature),
                content = "${temperature}º",
                icon = R.drawable.thermometer,
                onClick = {
                    val intent = Intent(activity, SettingActivity::class.java)
                    intent.putExtra("setting", "temperature")
                    launcher.launch(intent)
                }
            )
            Sensor(
                title = stringResource(R.string.humidity),
                content = "${humidity}%",
                icon = R.drawable.water_droplet,
                onClick = {}
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
           //     .widthIn(max = columnMaxWidth)
        ) {
            Sensor(
                title = stringResource(R.string.soilHumidity),
                content = "${soilMoisture}%",
                icon = R.drawable.moisture_soil,
                onClick = {}
            )
            Sensor(
                title = stringResource(R.string.light),
                content = format(light, "lx"),
                icon = R.drawable.light,
                onClick = {}
            )
        }
    }
     /*   Sensor(
            title = stringResource(R.string.light),
            content = format(light, "s"),
            icon = R.drawable.light,
            onClick = {},
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }*/
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
    onClick: () -> Unit,
    title: String = "Test",
    @DrawableRes icon: Int = R.drawable.water_droplet,
    content: String = "100%",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .then(modifier)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
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
                textAlign = TextAlign.Center,
                fontSize = 32.sp,
                modifier = Modifier.weight(1f)
            )
        }

    }
}

private fun roundDecimals(number: Float) =
    (round(number * 100) / 100).toString()

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

private fun format(value: Short, unit: String) : String = format(value.toFloat(), unit)