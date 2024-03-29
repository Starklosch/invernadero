package com.starklosch.invernadero

import android.Manifest
import android.os.Build

@Suppress("InlinedApi")
private val latest = listOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN
)

private val legacy = listOf(
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private val apis29and30 = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION
)

fun getBluetoothPermissions() = when (Build.VERSION.SDK_INT) {
    in 1..28 -> legacy
    29, 30 -> apis29and30
    else -> latest
}