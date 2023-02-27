@file:OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)

package com.starklosch.invernadero

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.juul.kable.Advertisement
import com.juul.kable.Bluetooth
import com.starklosch.invernadero.ui.theme.InvernaderoTheme

class DeviceSelectionActivity : ComponentActivity() {
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
    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()
        viewModel.stop()
        viewModel.clear()
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Main() {
        val activity = LocalContext.current as DeviceSelectionActivity
        val bluetoothPermission = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )

        val bluetoothAvailability by Bluetooth.availability.collectAsStateWithLifecycle(
            Bluetooth.Availability.Unavailable(null)
        )
        val isAvailable = bluetoothAvailability is Bluetooth.Availability.Available

        val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
        val refreshing = scanStatus is ScanStatus.Scanning

        val pullRefreshState =
            rememberPullRefreshState(refreshing, { if (isAvailable) viewModel.start() })

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBar() }
        )
        { padding ->
            Box(Modifier.padding(padding).fillMaxSize().pullRefresh(pullRefreshState)) {
                if (!bluetoothPermission.allPermissionsGranted)
                    PermissionRequest(bluetoothPermission)

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = {}
                )

                if (isAvailable) {
                    LaunchedEffect(isAvailable) {
                        viewModel.start()
                    }
                    ShowDevices()
                } else {
                    Button(
                        onClick = {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            launcher.launch(enableBtIntent)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Enable Bluetooth")
                    }
                }

                PullRefreshIndicator(
                    refreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun ShowDevices() {
        val discoveredDevices by viewModel.advertisements.collectAsStateWithLifecycle()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(discoveredDevices) {
                BluetoothDeviceEntry(it)
            }
        }
    }
}

@Composable
private fun TopBar() {
    val context = LocalContext.current
    TopAppBar(title = { Text(text = context.resources.getString(R.string.devices)) })
}

@Composable
private fun PermissionRequest(bluetoothPermission: MultiplePermissionsState) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (bluetoothPermission.shouldShowRationale)
            Text("Please grant permission")
        else
            Text("No permission")

        Button(onClick = { bluetoothPermission.launchMultiplePermissionRequest() }) {
            Text("Request")
        }
    }
//    item {
//        Text("Missing permissions:")
//    }
//    items(bluetoothScanPermission.revokedPermissions) {
//        Text("- ${it.permission.split('.').last()}")
//    }
//    item {
//        Button(onClick = { bluetoothScanPermission.launchMultiplePermissionRequest() }) {
//            Text("Request")
//        }
//    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothDeviceEntry(device: Advertisement) {
    val activity = LocalContext.current as Activity

    Log.d("UI", "Device")
    Card(
        onClick = {
            val intent = Intent()
            intent.putExtra("device", device.address)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
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