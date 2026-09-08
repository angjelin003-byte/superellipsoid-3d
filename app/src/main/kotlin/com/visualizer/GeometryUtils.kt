package com.visualizer

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

data class Vector3(val x: Float, val y: Float, val z: Float)

fun evaluateSuperellipsoid(
    eta: Float,
    omega: Float,
    s1: Float,
    s2: Float,
    scale: Float
): Vector3 {
    val cosEta = cos(eta)
    val sinEta = sin(eta)
    val cosOmega = cos(omega)
    val sinOmega = sin(omega)

    val x = sign(cosEta * cosOmega) * abs(cosEta).pow(s1) * abs(cosOmega).pow(s2)
    val y = sign(cosEta * sinOmega) * abs(cosEta).pow(s1) * abs(sinOmega).pow(s2)
    val z = sign(sinEta) * abs(sinEta).pow(s1)

    return Vector3(x * scale, y * scale, z * scale)
}
