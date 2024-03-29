package com.starklosch.invernadero

import java.nio.ByteBuffer

enum class OperationType(val id: Byte) {
    ReadValues('V'),
    ReadSettings('S'),
    ReadInformation('I'),
    SetSettings('W');

    constructor (char: Char) : this(char.code.toByte())

    companion object {
        fun fromByte(value: Byte?) = OperationType.values().firstOrNull { it.id == value }
    }
}

sealed class Response {
    object InvalidResponse : Response()

    class SettingsResponse(data: ByteArray = byteArrayOf()) : Response() {
        init {
            if (data.size < Settings.bytes)
                throw Exception("Too few bytes")
        }

        val settings = Settings.fromByteArray(data)
    }

    class InformationResponse(data: ByteArray = byteArrayOf()) : Response() {
        init {
            if (data.size < Information.bytes)
                throw Exception("Too few bytes")
        }

        val information = Information.fromByteArray(data)
    }

    class ValuesResponse(data: ByteArray = byteArrayOf()) : Response() {
        init {
            if (data.size < Values.bytes)
                throw Exception("Too few bytes")
        }

        val values = Values.fromByteArray(data)
    }
}


sealed class Request(private val operationType: OperationType, private val data: ByteArray = byteArrayOf()){
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(1 + data.size)
        buffer.put(operationType.id)
        if (data.isNotEmpty())
            buffer.put(data)
        return buffer.array()
    }

    object ValuesRequest : Request(OperationType.ReadValues)
    object InformationRequest : Request(OperationType.ReadInformation)
    object SettingsRequest : Request(OperationType.ReadSettings)
    class SetSettingsRequest(settings: Settings) : Request(OperationType.SetSettings, settings.toByteArray())
}
