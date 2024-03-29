@file:OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
@file:Suppress("FunctionName")

package com.starklosch.invernadero

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.juul.kable.Advertisement
import com.juul.kable.Bluetooth
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import com.starklosch.invernadero.ui.theme.iconModifier

class DeviceSelectionActivity : ComponentActivity() {
    private val viewModel by viewModels<DeviceSelectionViewModel>()

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

    override fun onPause() {
        super.onPause()
        viewModel.stopScanning()
        viewModel.clear()
    }

    @Composable
    private fun Main() {
        val bluetoothPermission = rememberMultiplePermissionsState(
            getBluetoothPermissions()
        )

        val bluetoothAvailability by Bluetooth.availability.collectAsStateWithLifecycle(
            Bluetooth.Availability.Unavailable(null)
        )
        val bluetoothIsAvailable = bluetoothAvailability is Bluetooth.Availability.Available

        val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
        val scanning = bluetoothIsAvailable && scanStatus is ScanStatus.Scanning

        val pullRefreshState =
            rememberPullRefreshState(
                refreshing = scanning,
                onRefresh = { if (bluetoothIsAvailable) viewModel.startScanning() })

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    bluetoothIsAvailable,
                    scanning = scanning,
                    onScan = { viewModel.startScanning() },
                    onStop = { viewModel.stopScanning() }
                )
            }
        )
        { padding ->
            Box(Modifier.padding(padding).fillMaxSize().pullRefresh(pullRefreshState)) {
                if (bluetoothPermission.allPermissionsGranted) {
                    if (bluetoothIsAvailable) {
                        LaunchedEffect(true) {
                            viewModel.startScanning()
                        }
                        ShowDevices()
                    } else {
                        EnableBluetoothButton(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else
                    PermissionRequest(bluetoothPermission)

                PullRefreshIndicator(
                    scanning,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    @Composable
    private fun ShowDevices() {
        val discoveredDevices by viewModel.advertisements.collectAsStateWithLifecycle()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(discoveredDevices) {
                BluetoothDevice(it)
            }
        }
    }
}

@Composable
private fun TopBar(
    bluetoothAvailable: Boolean,
    scanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit
) {
    val iconId = if (scanning) R.drawable.stop else R.drawable.search
    val onClick = if (scanning) onStop else onScan
    TopAppBar(title = { Text(text = stringResource(R.string.devices)) }, actions = {
        if (bluetoothAvailable)
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(iconId),
                    contentDescription = null,
                    modifier = iconModifier
                )
            }
    })
}

@Composable
private fun EnableBluetoothButton(modifier: Modifier = Modifier) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    LaunchedEffect(true) {
        launcher.launch(enableBtIntent)
    }

    Button(
        onClick = {
            launcher.launch(enableBtIntent)
        },
        modifier = modifier
    ) {

        Text(stringResource(R.string.enable_bluetooth))
    }
}

@Composable
private fun PermissionRequest(bluetoothPermission: MultiplePermissionsState) {
    LaunchedEffect(true) {
        bluetoothPermission.launchMultiplePermissionRequest()
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        if (bluetoothPermission.shouldShowRationale)
            Text("Please grant permission")
        else {
            for (it in bluetoothPermission.revokedPermissions) {
                Text("- ${it.permission.split('.').last()}")
            }
            Text("No permission")
        }

        Button(onClick = { bluetoothPermission.launchMultiplePermissionRequest() }) {
            Text(stringResource(R.string.request_permissions))
        }
    }
}

@Composable
private fun BluetoothDevice(device: Advertisement) {
    val activity = LocalContext.current as Activity

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
            device.name?.let {
                Text(text = it, fontWeight = FontWeight.Bold)
            }
            Text(text = device.address)
        }
    }
}