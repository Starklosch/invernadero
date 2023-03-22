package com.starklosch.invernadero

import android.Manifest
import android.os.Build
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

val list1 = listOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN
)

val list2 = listOf(
    
    Manifest.permission.ACCESS_COARSE_LOCATION
)

val list3 = listOf(

    Manifest.permission.ACCESS_FINE_LOCATION
)

fun getBluetoothPermissions() = when (Build.VERSION.SDK_INT) {
    in 1..28 -> list2
    29, 30 -> list3
    else -> list1
}