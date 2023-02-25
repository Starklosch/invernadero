@file:OptIn(ExperimentalMaterial3Api::class)

package com.starklosch.invernadero

import MyApp
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.State
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlinx.coroutines.delay
import kotlin.math.round
import kotlin.math.roundToInt

var precision = 1.0f

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
//                    val connected = viewModel.connected.collectAsStateWithLifecycle().value
//                    Text(if (connected) "Connected" else "F")
//                    Button(onClick = {
//                        viewModel.connect()
//                    }){
//                        Text("Connect")
//                    }
                    Main()
                }
            }
        }
    }

    fun getApp(): MyApp {
        return application as MyApp
    }
}

@SuppressLint("MissingPermission")
@Preview
@Composable
private fun Main() {
    val context = LocalContext.current as MainActivity
    Scaffold(
        topBar = { TopBar() }
    )
    { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
//            contentAlignment = Alignment.TopCenter
        ) {
//            val context = LocalContext.current
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
                Log.d("BLUETOOTH", "RESULT")
                val address = result.data?.getStringExtra("device")
                if (address != null) viewModel.connect(address)
            }
        }

    val device = viewModel.device.collectAsStateWithLifecycle().value
    val state = device?.state?.collectAsStateWithLifecycle()?.value
    val subscribe = device?.rawData?.collectAsStateWithLifecycle(Operation(OperationType.ReadInformation))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (device != null){
            Text("Device: " + device.name)
            Text("State: " + state)
        }

        LaunchedEffect(state) {
            while(state == State.Connected) {
                viewModel.write()
                delay(10000)
            }
        }

        Information(subscribe?.value.takeIf { it?.operationType == OperationType.ReadValues }?.values)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val intent = Intent(context, BluetoothSelectionActivity::class.java)
//                context.startActivity(intent)
                launcher.launch(intent)
            }) {
                Text("Bluetooth")
            }
            Button(onClick = {
                val intent = Intent(context, BluetoothCommunicationActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Comm")
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

    val columnMinWidth = 150.dp
    var columnMaxWidth = 240.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f, false)
                .widthIn(max = columnMaxWidth)
        ) {
            Version2(
                title = "Temperatura",
                content = "${temperature.roundToInt()}º",
                icon = R.drawable.thermometer,
            )
            Version2(
                title = "Humedad",
                content = "${humidity.roundToInt()} %",
                icon = R.drawable.water_droplet,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, false)
                .widthIn(max = columnMaxWidth)
        ) {
            Version2(
                title = "Humedad de suelo",
                content = "${soilMoisture.roundToInt()} %",
                icon = R.drawable.moisture_soil
            )
            Version2(
                title = "Luz",
                content = "${light.roundToInt()} lx",
                icon = R.drawable.light
            )

        }
    }
//    LazyVerticalGrid(
//        columns = GridCells.Fixed(2),
//        verticalArrangement = Arrangement.Top,
//        horizontalArrangement = Arrangement.spacedBy(180.dp),
//        modifier = modifier
//    ) {
//        item() {
//            Version2(title = "Temperatura", content = "25º", icon = R.drawable.thermometer)
//        }
//        item {
//            Version2(title = "Humedad", content = "25%", icon = R.drawable.water_droplet)
//        }
//        item {
//            Version2(
//                title = "Humedad de suelo",
//                content = "25%",
//                icon = R.drawable.moisture_soil
//            )
//        }
//        item {
//            Version2(title = "Luz", content = "25%", icon = R.drawable.light)
//        }
//    }
}

@Composable
private fun SampleList() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Version1(title = "Temperatura", content = "25º", icon = R.drawable.thermometer)
        Card()
        {
            Box(modifier = Modifier.padding(16.dp)) {
                Version1(title = "Temperatura", content = "25º", icon = R.drawable.thermometer)
            }
        }
        Version2(title = "Temperatura", content = "25º", icon = R.drawable.thermometer)
//                Version3(title = "Temperatura", content = "25º", icon = R.drawable.thermometer)
    }
}

@Composable
private fun Version1(
    title: String = "Test",
    @DrawableRes icon: Int = R.drawable.water_droplet,
    content: String = "100%"
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "Icon",
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title)
            Text(text = content)
        }
    }
}

@Composable
private fun Version2(
    title: String = "Test",
    @DrawableRes icon: Int = R.drawable.water_droplet,
    content: String = "100%",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
//        shape = RoundedCornerShape(20.dp),
        onClick = { },
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

@Composable
private fun MyCard(text: String, @DrawableRes icon: Int = R.drawable.water_droplet) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
//            .height(200.dp)
            .padding(15.dp),
        onClick = { expanded = !expanded },
//        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            CardContent(expanded, icon, text)
        }
    }
}

@Composable
private fun CardContent(expanded: Boolean, @DrawableRes icon: Int, text: String) {

//                    AsyncImage(model = "https://cdn-icons-png.flaticon.com/512/7721/7721672.png", contentDescription = null)
    var sliderPos by remember { mutableStateOf(100f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        val t = Text(
            text = text, modifier = Modifier.padding(start = 6.dp)
        )
//        Log.d("COLOR", LocalContentColor.current)
    }
    Divider(Modifier.padding(top = 8.dp, bottom = 12.dp))
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = roundDecimals(sliderPos - precision)
        )
        Text(
            text = roundDecimals(sliderPos + precision)
        )
    }
    Slider(
        value = sliderPos,
        onValueChange = { sliderPos = it },
        valueRange = 0f..100f
    )
}

private fun roundDecimals(number: Float) =
    (round(number * 100) / 100).toString()

@Composable
private fun TopBar() {
    val context = LocalContext.current
    TopAppBar(title = { Text(text = context.resources.getString(R.string.app_name)) })
}

@Composable
private fun Greeting(name: String) {
    Text(text = "Hello $name!")
}