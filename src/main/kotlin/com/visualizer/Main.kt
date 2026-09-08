package com.visualizer

import processing.core.PApplet
import processing.core.PConstants
import processing.event.MouseEvent

class SuperellipsoidVisualizer : PApplet() {

    private var rotX = 0f
    private var rotY = 0f
    private var scaleFactor = 220f

    // Parameter exponents (modifiable via controls)
    private var s1 = 0.87f
    private var s2 = 0.91f

    // Toggle states
    private var drawMesh = false

    // Resolution steps
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

        // 3D Scene Transformation
        pushMatrix()
        translate(width / 2f, height / 2f, 0f)

        if (mousePressed && mouseButton == LEFT) {
            rotY += (mouseX - pmouseX) * 0.01f
            rotX -= (mouseY - pmouseY) * 0.01f
        } else {
            rotY += 0.003f // Continuous subtle auto-rotation
        }

        rotateX(rotX)
        rotateY(rotY)

        renderSurface()
        popMatrix()

        // 2D Heads-Up Display (HUD)
        drawHUD()
    }

    private fun renderSurface() {
        val etaMin = -PConstants.HALF_PI
        val etaMax = PConstants.HALF_PI
        val omegaMin = -PConstants.PI
        val omegaMax = PConstants.PI

        val dEta = (etaMax - etaMin) / etaSteps
        val dOmega = (omegaMax - omegaMin) / omegaSteps

        if (drawMesh) {
            // Quad mesh rendering mode
            noFill()
            stroke(230, 110, 255, 120)
            strokeWeight(1.0f)

            for (i in 0 until etaSteps) {
                val eta1 = etaMin + i * dEta
                val eta2 = etaMin + (i + 1) * dEta

                beginShape(QUAD_STRIP)
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j * dOmega
                    val p1 = evaluateSuperellipsoid(eta1, omega, s1, s2, scaleFactor)
                    val p2 = evaluateSuperellipsoid(eta2, omega, s1, s2, scaleFactor)
                    vertex(p1.x, p1.y, p1.z)
                    vertex(p2.x, p2.y, p2.z)
                }
                endShape()
            }
        } else {
            // Core point cloud pass
            strokeWeight(1.8f)
            stroke(230, 110, 255, 220)
            beginShape(POINTS)
            for (i in 0..etaSteps) {
                val eta = etaMin + i * dEta
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j * dOmega
                    val p = evaluateSuperellipsoid(eta, omega, s1, s2, scaleFactor)
                    vertex(p.x, p.y, p.z)
                }
            }
            endShape()

            // Glowing ambient halo pass
            strokeWeight(3.5f)
            stroke(180, 50, 230, 45)
            beginShape(POINTS)
            for (i in 0..etaSteps step 2) {
                val eta = etaMin + i * dEta
                for (j in 0..omegaSteps step 2) {
                    val omega = omegaMin + j * dOmega
                    val p = evaluateSuperellipsoid(eta, omega, s1, s2, scaleFactor)
                    vertex(p.x, p.y, p.z)
                }
            }
            endShape()
        }
    }

    private fun drawHUD() {
        hint(PConstants.DISABLE_DEPTH_TEST)
        camera()
        noLights()

        fill(255, 255, 255, 220)
        textSize(13f)
        textAlign(PConstants.LEFT, PConstants.TOP)

        val hudText = """
            [CONTROLS]
            Drag Left Mouse  : Rotate 3D surface
            Mouse Wheel      : Zoom in / out (Scale: ${scaleFactor.toInt()})
            Up / Down Keys   : Adjust s1 = ${String.format("%.2f", s1)}
            Left / Right Keys: Adjust s2 = ${String.format("%.2f", s2)}
            M Key            : Toggle Surface (Points / Quad Mesh)
            R Key            : Reset parameters to default
        """.trimIndent()

        text(hudText, 20f, 20f)
        hint(PConstants.ENABLE_DEPTH_TEST)
    }

    override fun mouseWheel(event: MouseEvent) {
        val count = event.count
        scaleFactor -= count * 15f
        scaleFactor = scaleFactor.coerceIn(50f, 600f)
    }

    override fun keyPressed() {
        when (key) {
            'r', 'R' -> {
                s1 = 0.87f
                s2 = 0.91f
                scaleFactor = 220f
                rotX = 0f
                rotY = 0f
            }
            'm', 'M' -> drawMesh = !drawMesh
        }

        when (keyCode) {
            PConstants.UP -> s1 = (s1 + 0.02f).coerceAtMost(3.0f)
            PConstants.DOWN -> s1 = (s1 - 0.02f).coerceAtLeast(0.05f)
            PConstants.RIGHT -> s2 = (s2 + 0.02f).coerceAtMost(3.0f)
            PConstants.LEFT -> s2 = (s2 - 0.02f).coerceAtLeast(0.05f)
        }
    }
}

fun main() {
    PApplet.main("com.visualizer.SuperellipsoidVisualizer")
}
