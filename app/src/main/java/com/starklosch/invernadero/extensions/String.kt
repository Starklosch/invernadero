package com.starklosch.invernadero.extensions

fun String.capitalize() = replaceFirstChar { it.uppercase() }