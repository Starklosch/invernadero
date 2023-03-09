package com.starklosch.invernadero

fun Float.ifNotFinite(value: Float): Float {
    return if (isFinite()) this else value
}

fun Short.ifNegative(value: Short): Short {
    return if (this > 0) this else value
}