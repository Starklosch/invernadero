package com.starklosch.invernadero.extensions

fun Short.ifNegative(value: Short): Short {
    return if (this > 0) this else value
}