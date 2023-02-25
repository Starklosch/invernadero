package com.starklosch.invernadero

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*

class GattCallback(serviceUUID: UUID, characteristicUUID: UUID, onFound: (BluetoothGattCharacteristic) -> Unit) : BluetoothGattCallback() {
    private val serviceUUID = serviceUUID
    private val characteristicUUID = characteristicUUID
    private val onFound = onFound

    var onNotify : ((ByteArray) -> Unit)? = null

    val map = mapOf(
        BluetoothGatt.GATT_SUCCESS to "SUCESS",
        BluetoothGatt.GATT_FAILURE to "FAILURE",
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED to "WRITE NOT PERMITTED",
        BluetoothGatt.GATT_READ_NOT_PERMITTED to "READ NOT PERMITTED"
    )

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.d("BLUETOOTH", "Changed")
        Log.d("BLUETOOTH", "New value: " + String(value))
        onNotify?.invoke(value)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        Log.d("BLUETOOTH", "Write status " + map[status])
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        Log.d("BLUETOOTH", "Read status " + map[status])
        Log.d("BLUETOOTH", "Read " + String(value))
    }

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // successfully connected to the GATT Server
            Log.d("BLUETOOTH", "Connected")
            gatt?.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // disconnected from the GATT Server
            Log.d("BLUETOOTH", "Disconnected")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission", "NewApi")
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("BLUETOOTH", "Success")

            val service = gatt?.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)


            if (characteristic != null) {
                onFound(characteristic)
                printProperties(characteristic.properties)
                gatt.setCharacteristicNotification(characteristic, true)
                gatt.writeCharacteristic(
                    characteristic,
                    "Test".toByteArray(),
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            }

            val properties = BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE
            val found = match(gatt!!.services, properties)


//            gatt?.services?.forEach {
//                Log.d("BLUETOOTH", if (it.type == BluetoothGattService.SERVICE_TYPE_PRIMARY) "PRIMARY" else "SECONDARY" )
//                Log.d("BLUETOOTH", "UUID: ${it.uuid}")
//                Log.d("BLUETOOTH", "Instance: ${it.instanceId}")
////                printCharacteristics(gatt, it.characteristics)
//            }
        }
    }

    private fun match(
        services: List<BluetoothGattService>,
        properties: Int
    ): BluetoothGattCharacteristic? {
        for (service in services)
            for (characteristic in service.characteristics)
                if ((characteristic.properties and properties) > 0)
                    return characteristic

        return null
    }

    private fun printCharacteristics(
        gatt: BluetoothGatt,
        chars: List<BluetoothGattCharacteristic>
    ) {
        Log.d("BLUETOOTH", "CHARACTERISTICS")
        chars.forEach { printCharacteristic(gatt, it) }
    }

    @SuppressLint("MissingPermission")
    private fun printCharacteristic(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
        Log.d("BLUETOOTH", "Instance: ${char.instanceId}")
        Log.d("BLUETOOTH", "UUID: ${char.uuid}")
//        Log.d("BLUETOOTH", "Descriptor: ${char.getDescriptor(char.uuid).characteristic.getStringValue(0)}")
        gatt.readCharacteristic(char)
        printType(char.writeType)
        printPermissions(char.permissions)
        printProperties(char.properties)
    }

    private val types = mapOf(
        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT to "DEFAULT",
        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE to "NO_RESPONSE",
        BluetoothGattCharacteristic.WRITE_TYPE_SIGNED to "SIGNED",
    )

    private val perms = mapOf(
        BluetoothGattCharacteristic.PERMISSION_READ to "READ",
        BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED to "READ_ENCRYPTED",
        BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM to "READ_ENCRYPTED_MITM",
        BluetoothGattCharacteristic.PERMISSION_WRITE to "WRITE",
        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED to "WRITE_ENCRYPTED",
        BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED to "WRITE_SIGNED",
        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM to "WRITE_ENCRYPTED_MITM",
        BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM to "WRITE_SIGNED_MITM"
    )

    private val props = mapOf(
        BluetoothGattCharacteristic.PROPERTY_BROADCAST to "BROADCAST",
        BluetoothGattCharacteristic.PROPERTY_INDICATE to "INDICATE",
        BluetoothGattCharacteristic.PROPERTY_NOTIFY to "NOTIFY",
        BluetoothGattCharacteristic.PROPERTY_READ to "READ",
        BluetoothGattCharacteristic.PROPERTY_WRITE to "WRITE",
        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE to "WRITE_NO_RESPONSE",
        BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS to "EXTENDED_PROPS",
    )

    private fun printType(type: Int) {
        Log.d("BLUETOOTH", "Types:")
        val concat =
            types.filter { it.key and type > 0 }.entries.joinTo(StringBuilder()) { it.value }
        Log.d("BLUETOOTH", concat.toString())
    }

    private fun printProperties(properties: Int) {
        Log.d("BLUETOOTH", "Properties:")
        val concat =
            props.filter { it.key and properties > 0 }.entries.joinTo(StringBuilder()) { it.value }
        Log.d("BLUETOOTH", concat.toString())
    }

    private fun printPermissions(permissions: Int) {
        Log.d("BLUETOOTH", "Permissions:")
        val concat =
            perms.filter { it.key and permissions > 0 }.entries.joinTo(StringBuilder()) { it.value }
        Log.d("BLUETOOTH", concat.toString())
    }
}