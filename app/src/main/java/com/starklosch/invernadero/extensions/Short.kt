package com.starklosch.invernadero

fun Short.ifNegative(value: Short): Short {
    return if (this > 0) this else value
}