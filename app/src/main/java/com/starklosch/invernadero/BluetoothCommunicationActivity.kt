package com.starklosch.invernadero

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import java.util.*

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

class BluetoothCommunicationActivity : ComponentActivity() {
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? by mutableStateOf(null)
    private val serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private val gattCallback = GattCallback(serviceUUID, characteristicUUID) { characteristic = it }
    private var characteristic: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (savedInstanceState == null)
//            finish()

        if (intent != null) {
            bluetoothDevice = intent.parcelable("device")
            if (bluetoothDevice != null) {
                Log.d("BLUETOOTH", "Trying to connect to " + bluetoothDevice!!.name)
                bluetoothGatt =
                    bluetoothDevice!!.connectGatt(this, false, gattCallback)
            }
        }

//        bluetoothGatt.connect()

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

    private fun getCharacteristic(): BluetoothGattCharacteristic {
        return characteristic!!
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun send(text: String) {
        bluetoothGatt?.writeCharacteristic(
            getCharacteristic(),
            text.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        )
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothGatt != null) {
            Log.d("BLUETOOTH", "Closing")
            bluetoothGatt!!.close()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @SuppressLint("MissingPermission")
    @Composable
    private fun Main(viewModel: CommunicationModel = viewModel()) {
        val activity = LocalContext.current as BluetoothCommunicationActivity

        val messages by viewModel.getMessages().collectAsState()
        var text by rememberSaveable { mutableStateOf("") }

        Column(Modifier.padding(16.dp)) {
            Messages(messages)
            Input(text) {
                text = it
            }
            Row {
                Button(onClick = {
                    if (text.isNotEmpty()) {
                        send(text)
                        viewModel.addMessage(text)
                        text = ""
                    }
                }) {
                    Text("Send")
                }
                Button(onClick = {
                    finish()
                }) {
                    Text("Stop")
                }
            }
        }
    }

    @Composable
    private fun Messages(messageList: List<String>) {
        Box {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(10.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                items(messageList) {
                    Message(it)
                }
            }
        }
    }

    @Composable
    private fun Message(text: String) {
        Text(text)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Input(text: String, onValueChange: (String) -> Unit) {
        TextField(value = text, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())
    }
}