package com.devsoto.monical.data.model

import kotlin.math.roundToLong

/** Round to 2 decimals (matches the prototype's Math.round(v*100)/100). */
fun round2(v: Double): Double = (v * 100).roundToLong() / 100.0
