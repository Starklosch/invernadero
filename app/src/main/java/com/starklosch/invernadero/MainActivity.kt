@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlinx.coroutines.delay
import kotlin.math.round
import kotlin.math.roundToInt

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

class MainActivity : ComponentActivity() {

//    private val viewModel by viewModels<MainActivityViewModel>()

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

@SuppressLint("MissingPermission")
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
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val address = result.data?.getStringExtra("device")
                if (address != null)
                    viewModel.connect(address)
            }
        }

    val device by viewModel.device.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subscribe = device?.rawData?.collectAsStateWithLifecycle(Operation(OperationType.ReadInformation))

//    val test by viewModel.test2.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(state) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            while(state is State.Connected) {
                viewModel.write()
                delay(2000)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("State: ${state.toString().takeWhile { it != '(' }}")
//        Text("Counter: $test")

        Information(subscribe?.value.takeIf { it?.operationType == OperationType.ReadValues }?.values)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val intent = Intent(context, DeviceSelectionActivity::class.java)
                launcher.launch(intent)
            }) {
                Text("Select device")
            }

            if (state is State.Connected)
                Button(onClick = viewModel::disconnect){
                    Text("Disconnect")
                }
        }
    }
}

@Composable
private fun Information(values: Values?, modifier: Modifier = Modifier) {
    val temperature = if (values != null && !values.temperatureInCelsius.isNaN()) values.temperatureInCelsius else 25f
    val humidity  = if (values != null && !values.humidity.isNaN()) values.humidity else 25f
    val soilMoisture  = if (values != null && !values.soilMoisture.isNaN()) values.soilMoisture else 25f
    val light  = if (values != null && !values.light.isNaN()) values.light else 25f

    val context = LocalContext.current

    val columnMinWidth = 150.dp
    val columnMaxWidth = 240.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f, false)
                .widthIn(max = columnMaxWidth)
        ) {
            Sensor(
                title = stringResource(R.string.temperature),
                content = "${temperature.roundToInt()}º",
                icon = R.drawable.thermometer,
                onClick = { Toast.makeText(context, stringResource(R.string.temperature), Toast.LENGTH_SHORT).show() }
            )
            Sensor(
                title = stringResource(R.string.humidity),
                content = "${humidity.roundToInt()} %",
                icon = R.drawable.water_droplet,
                onClick = {}
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
                .widthIn(max = columnMaxWidth)
        ) {
            Sensor(
                title = stringResource(R.string.soilHumidity),
                content = "${soilMoisture.roundToInt()} %",
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
//        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
//        elevation = 2.dp
        modifier = Modifier
            .padding(16.dp)
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
//            Column(horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.fillMaxWidth()) {
            val t = Text(
                text = content,
                fontSize = 32.sp,
                //                modifier = Modifier.weight(1f)
            )
//            }
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

private fun format(value: ArduinoFloat, unit: String): String {
    var _value = value
    var _unit = unit
    if (value > 1000){
        _value = value / 1000
        _unit = "k" + unit
    }

    return "${_value.roundToInt()} $_unit"
}
