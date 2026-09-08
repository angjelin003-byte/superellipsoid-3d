package com.visualizer

import processing.core.PApplet
import processing.core.PConstants

class SuperellipsoidVisualizer : PApplet() {

    private var rotX = 0f
    private var rotY = 0f

    // Parameter exponents from the formula
    private val s1 = 0.87f
    private val s2 = 0.91f

    // Grid resolution
    private val etaSteps = 120
    private val omegaSteps = 240

    override fun settings() {
        size(900, 900, P3D)
        smooth(8)
    }

    override fun setup() {
        frameRate(60)
    }

    override fun draw() {
        background(8, 4, 15)

        translate(width / 2f, height / 2f, 0f)

        // Mouse rotation control
        if (mousePressed) {
            rotY += (mouseX - pmouseX) * 0.01f
            rotX -= (mouseY - pmouseY) * 0.01f
        } else {
            rotY += 0.005f
        }

        rotateX(rotX)
        rotateY(rotY)

        // Render multi-layered glowing point cloud & wireframe
        renderSurface()
    }

    private fun renderSurface() {
        val etaMin = -PConstants.HALF_PI
        val etaMax = PConstants.HALF_PI
        val omegaMin = -PConstants.PI
        val omegaMax = PConstants.PI

        val dEta = (etaMax - etaMin) / etaSteps
        val dOmega = (omegaMax - omegaMin) / omegaSteps

        // Core bright mesh points
        strokeWeight(1.8f)
        stroke(230, 110, 255, 220)
        beginShape(POINTS)
        for (i in 0..etaSteps) {
            val eta = etaMin + i * dEta
            for (j in 0..omegaSteps) {
                val omega = omegaMin + j * dOmega
                val p = evaluateSuperellipsoid(eta, omega, s1, s2)
                vertex(p.x, p.y, p.z)
            }
        }
        endShape()

        // Outer ambient halo/glow pass
        strokeWeight(3.5f)
        stroke(180, 50, 230, 45)
        beginShape(POINTS)
        for (i in 0..etaSteps step 2) {
            val eta = etaMin + i * dEta
            for (j in 0..omegaSteps step 2) {
                val omega = omegaMin + j * dOmega
                val p = evaluateSuperellipsoid(eta, omega, s1, s2)
                vertex(p.x, p.y, p.z)
            }
        }
        endShape()
    }
}

fun main() {
    PApplet.main("com.visualizer.SuperellipsoidVisualizer")
}
