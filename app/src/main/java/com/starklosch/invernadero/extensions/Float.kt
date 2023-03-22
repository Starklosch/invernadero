package com.starklosch.invernadero.extensions

fun Float.ifNotFinite(value: Float): Float {
    return if (isFinite()) this else value
}