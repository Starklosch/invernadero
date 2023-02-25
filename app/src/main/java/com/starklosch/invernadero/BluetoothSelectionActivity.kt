package com.starklosch.invernadero

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.juul.kable.Advertisement
import com.starklosch.invernadero.ui.theme.InvernaderoTheme

@OptIn(ExperimentalPermissionsApi::class)
class BluetoothSelectionActivity : ComponentActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
//    private var discoveredDevices = mutableStateListOf<BluetoothDevice>()
    private val viewModel by viewModels<ScanViewModel>()

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

        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return
        }
    }

    private fun getAdapter(): BluetoothAdapter {
        if (bluetoothAdapter == null)
            throw Exception("Null adapter")

        return bluetoothAdapter as BluetoothAdapter
    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()
        viewModel.clear()
        getAdapter().cancelDiscovery()
    }

    private inline fun MultiplePermissionsState.allRequiredPermissionsGranted(requiredPermissions: Collection<String>): Boolean {
        return revokedPermissions.none { permissionState -> permissionState.permission in requiredPermissions }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Main() {
        val activity = LocalContext.current as BluetoothSelectionActivity
        val bluetoothPermission = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bluetoothPermission.allRequiredPermissionsGranted(listOf(Manifest.permission.BLUETOOTH_CONNECT))) {
                item {
                    BluetoothEnabled(activity)
                }
            } else {
                item {
                    if (bluetoothPermission.shouldShowRationale)
                        Text("Please grant permission")
                    else
                        Text("No permission")

                    Button(onClick = { bluetoothPermission.launchMultiplePermissionRequest() }) {
                        Text("Request")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun BluetoothEnabled(
        activity: BluetoothSelectionActivity
    ) {
        val adapter = getAdapter()
        var bluetoothEnabled by remember { mutableStateOf(adapter.isEnabled) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { }
        )

        SystemBroadcastReceiver(
            BluetoothAdapter.ACTION_STATE_CHANGED,
        ) { intent ->
            if (intent == null)
                return@SystemBroadcastReceiver

            when (intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )) {
                BluetoothAdapter.STATE_ON -> bluetoothEnabled = true
                BluetoothAdapter.STATE_OFF -> bluetoothEnabled = false
            }
        }

        if (bluetoothEnabled) {
            ShowDevices(adapter, activity)
        } else {
            Button(onClick = {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(enableBtIntent)
            }) {
                Text("Enable Bluetooth")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun ShowDevices(
        adapter: BluetoothAdapter,
        activity: BluetoothSelectionActivity
    ) {
        val context = LocalContext.current
        var discoveringState = viewModel.status.collectAsStateWithLifecycle().value
        val bluetoothScanPermission = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        val discoveredDevices = viewModel.advertisements.collectAsStateWithLifecycle().value

//        SystemBroadcastReceivers(
//            arrayOf(
//                BluetoothAdapter.ACTION_DISCOVERY_STARTED,
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED
//            )
//        ) { intent ->
//            if (intent == null)
//                return@SystemBroadcastReceivers
//
//            when (intent.action) {
//                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
//                    discoveredDevices.clear()
//                    Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show()
//                    isDiscovering = true
//                }
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                    Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show()
//                    isDiscovering = false
//                }
//            }
//        }
//
//        SystemBroadcastReceiver(BluetoothDevice.ACTION_FOUND) { intent ->
//            run {
//                if (intent != null) {
//                    val device: BluetoothDevice? =
//                        intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
//
//                    if (device != null) {
//                        if (device.name != null)
//                            Log.d("BLUETOOTH", device.name)
//
//                        if (!discoveredDevices.contains(device))
//                            discoveredDevices.add(device)
//                    }
//                }
//            }
//        }

//        val pairedDevices = adapter.bondedDevices
//        if (pairedDevices.isNotEmpty()) {
//            Box(modifier = Modifier.padding(16.dp)) {
//                Text("Paired", fontSize = 20.sp)
//            }
//            pairedDevices.forEach { device ->
//                BluetoothDeviceEntry(device)
//            }
//        }

        if (discoveredDevices.isNotEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text("Discovered", fontSize = 20.sp)
            }
            discoveredDevices.forEach { device ->
                BluetoothDeviceEntry(device)
            }
        }

        if (!bluetoothScanPermission.allPermissionsGranted) {
            Text("Missing permissions:")
            for (permission in bluetoothScanPermission.revokedPermissions)
                Text("- ${permission.permission.split('.').last()}")

            Button(onClick = { bluetoothScanPermission.launchMultiplePermissionRequest() }) {
                Text("Request")
            }
        }

        when (discoveringState){
            ScanStatus.Scanning -> CancelDiscoverButton(bluetoothScanPermission.allPermissionsGranted, adapter)
            else ->StartDiscoveryButton(bluetoothScanPermission.allPermissionsGranted, adapter)
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun StartDiscoveryButton(
        enabled: Boolean,
        adapter: BluetoothAdapter
    ) {
        Button(enabled = enabled,
            onClick = viewModel::start
//            onClick = {
////                bluetoothAdapter.bluetoothLeScanner.startScan()
//                adapter.startDiscovery()
//            }
        ) {
            Text("Start discovery")
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun CancelDiscoverButton(
        enabled: Boolean,
        adapter: BluetoothAdapter
    ) {
        Button(enabled = enabled,
            onClick = viewModel::clear
//            onClick = {
//                adapter.cancelDiscovery()
//            }
        ) {
            Text("Cancel discovery")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingPermission")
    private
    @Composable
    fun BluetoothDeviceEntry(device: Advertisement) {
        val context = LocalContext.current

        Card(
            onClick = {
//                device = _device
                val intent = Intent()
                intent.putExtra("device", device.address)
                setResult(Activity.RESULT_OK, intent)
                finish()
//                val intent = Intent(context, BluetoothCommunicationActivity::class.java)
//                intent.putExtra("device", device)
//                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                device.name?.let { Text(text = it) }
                Text(text = device.address)
            }
        }
    }
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @SuppressLint("MissingPermission")
//    private
//    @Composable
//    fun BluetoothDeviceEntry(device: BluetoothDevice) {
//        val context = LocalContext.current
//
//        Card(
//            onClick = {
//                val intent = Intent(context, BluetoothCommunicationActivity::class.java)
//                intent.putExtra("device", device)
//                context.startActivity(intent)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 8.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp)
//            ) {
//                if (device.name != null)
//                    Text(text = device.name)
//                Text(text = device.address)
//            }
//        }
//    }
}

