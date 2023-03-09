package com.starklosch.invernadero

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

val prefixes = arrayOf("","k", "m", "g", "t")

data class Measurement(val value : Float, val unit : String) {

    private val thousandsFactor = if (value != 0f) (log10(value) / 3).toInt() else 0

//    init {
//        NumberFormat.getPercentInstance().
//    }

    val near = value / 1000f.pow(thousandsFactor)
    val prefix = prefixes[thousandsFactor]

    fun format(): String{
        return "${near.roundToInt()} $prefix$unit"
    }
}