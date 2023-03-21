package com.starklosch.invernadero

fun Float.ifNotFinite(value: Float): Float {
    return if (isFinite()) this else value
}