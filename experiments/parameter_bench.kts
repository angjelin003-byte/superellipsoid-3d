#!/usr/bin/env kotlin

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.system.measureNanoTime

fun benchmarkEvaluation(iterations: Int = 100_000) {
    val eta = 0.45f
    val omega = 1.22f
    val s1 = 0.87f
    val s2 = 0.91f

    var checksum = 0.0f
    val elapsedTime = measureNanoTime {
        for (i in 0 until iterations) {
            val cosEta = cos(eta)
            val sinEta = sin(eta)
            val cosOmega = cos(omega)
            val sinOmega = sin(omega)

            val x = sign(cosEta * cosOmega) * abs(cosEta).pow(s1) * abs(cosOmega).pow(s2)
            val y = sign(cosEta * sinOmega) * abs(cosEta).pow(s1) * abs(sinOmega).pow(s2)
            val z = sign(sinEta) * abs(sinEta).pow(s1)
            checksum += x + y + z
        }
    }

    val avgNs = elapsedTime.toDouble() / iterations
    println("Benchmarked $iterations iterations in ${elapsedTime / 1_000_000.0} ms ($avgNs ns/eval). Checksum: $checksum")
}

benchmarkEvaluation()
