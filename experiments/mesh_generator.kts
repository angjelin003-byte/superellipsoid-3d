#!/usr/bin/env kotlin

import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

data class Point3D(val x: Float, val y: Float, val z: Float)

fun generateSuperellipsoid(
    s1: Float,
    s2: Float,
    etaSteps: Int = 30,
    omegaSteps: Int = 60,
    scale: Float = 1.0f
): List<Point3D> {
    val vertices = mutableListOf<Point3D>()
    val piFloat = Math.PI.toFloat()
    val etaMin = -piFloat / 2.0f
    val etaMax = piFloat / 2.0f
    val omegaMin = -piFloat
    val omegaMax = piFloat

    val dEta = (etaMax - etaMin) / etaSteps
    val dOmega = (omegaMax - omegaMin) / omegaSteps

    for (i in 0..etaSteps) {
        val eta = etaMin + i * dEta
        val cosEta = cos(eta)
        val sinEta = sin(eta)

        for (j in 0..omegaSteps) {
            val omega = omegaMin + j * dOmega
            val cosOmega = cos(omega)
            val sinOmega = sin(omega)

            val x = sign(cosEta * cosOmega) * abs(cosEta).pow(s1) * abs(cosOmega).pow(s2) * scale
            val y = sign(cosEta * sinOmega) * abs(cosEta).pow(s1) * abs(sinOmega).pow(s2) * scale
            val z = sign(sinEta) * abs(sinEta).pow(s1) * scale

            vertices.add(Point3D(x, y, z))
        }
    }
    return vertices
}

val vertices = generateSuperellipsoid(s1 = 0.87f, s2 = 0.91f)
val outputFile = File("superellipsoid_mesh.obj")

outputFile.bufferedWriter().use { writer ->
    writer.write("# Superellipsoid Mesh Export\n")
    vertices.forEach { v ->
        writer.write("v ${v.x} ${v.y} ${v.z}\n")
    }
}

println("Exported ${vertices.size} vertices to ${outputFile.absolutePath}")
