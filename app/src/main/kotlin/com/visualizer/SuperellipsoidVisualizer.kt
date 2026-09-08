package com.visualizer

import processing.core.PApplet
import processing.core.PConstants

class SuperellipsoidVisualizer : PApplet() {

    private var rotX = 0f
    private var rotY = 0f
    private var scaleFactor = 320f

    private var s1 = 0.87f
    private var s2 = 0.91f
    private var drawMesh = false

    private val etaSteps = 90
    private val omegaSteps = 180

    // Multi-touch tracking for pinch zoom
    private var previousPinchDistance = -1f

    override fun settings() {
        fullScreen(P3D)
        smooth(8)
    }

    override fun setup() {
        frameRate(60)
    }

    override fun draw() {
        background(8, 4, 15)

        handleTouchGestures()

        pushMatrix()
        translate(width / 2f, height / 2f - 80f, 0f)

        rotateX(rotX)
        rotateY(rotY)

        renderSurface()
        popMatrix()

        drawOnScreenControls()
    }

    private fun handleTouchGestures() {
        if (touches.size == 1) {
            previousPinchDistance = -1f
            val touch = touches[0]
            if (touch.y < height - 350) { // Rotate only if touching outside bottom UI region
                rotY += (mouseX - pmouseX) * 0.008f
                rotX -= (mouseY - pmouseY) * 0.008f
            }
        } else if (touches.size == 2) {
            val t1 = touches[0]
            val t2 = touches[1]
            val currentDist = dist(t1.x, t1.y, t2.x, t2.y)

            if (previousPinchDistance > 0) {
                val delta = currentDist - previousPinchDistance
                scaleFactor = (scaleFactor + delta * 0.8f).coerceIn(100f, 800f)
            }
            previousPinchDistance = currentDist
        } else {
            previousPinchDistance = -1f
            rotY += 0.003f // Continuous rotation when untouched
        }
    }

    private fun renderSurface() {
        val etaMin = -PConstants.HALF_PI
        val etaMax = PConstants.HALF_PI
        val omegaMin = -PConstants.PI
        val omegaMax = PConstants.PI

        val dEta = (etaMax - etaMin) / etaSteps
        val dOmega = (omegaMax - omegaMin) / omegaSteps

        if (drawMesh) {
            noFill()
            stroke(230, 110, 255, 130)
            strokeWeight(1.2f)

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
            strokeWeight(2.2f)
            stroke(230, 110, 255, 230)
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

            strokeWeight(4.5f)
            stroke(180, 50, 230, 50)
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

    private fun drawOnScreenControls() {
        hint(PConstants.DISABLE_DEPTH_TEST)
        camera()

        val uiY = height - 320f
        fill(15, 10, 25, 200)
        stroke(180, 80, 220, 100)
        strokeWeight(1f)
        rect(20f, uiY, width - 40f, 280f, 30f)

        fill(255)
        textSize(36f)
        textAlign(PConstants.LEFT, PConstants.TOP)
        text("s1 = ${String.format("%.2f", s1)}", 50f, uiY + 30f)
        text("s2 = ${String.format("%.2f", s2)}", 50f, uiY + 110f)

        // Touch Control Buttons
        drawButton(width - 290f, uiY + 25f, 100f, 60f, "-")
        drawButton(width - 160f, uiY + 25f, 100f, 60f, "+")

        drawButton(width - 290f, uiY + 105f, 100f, 60f, "-")
        drawButton(width - 160f, uiY + 105f, 100f, 60f, "+")

        drawButton(50f, uiY + 195f, (width - 140f) / 2f, 65f, if (drawMesh) "Points" else "Mesh")
        drawButton(width / 2f + 20f, uiY + 195f, (width - 140f) / 2f, 65f, "Reset")

        hint(PConstants.ENABLE_DEPTH_TEST)
    }

    private fun drawButton(x: Float, y: Float, w: Float, h: Float, label: String) {
        fill(45, 25, 70, 220)
        stroke(200, 100, 250, 180)
        rect(x, y, w, h, 15f)

        fill(255)
        textSize(32f)
        textAlign(PConstants.CENTER, PConstants.CENTER)
        text(label, x + w / 2f, y + h / 2f - 4f)
    }

    override fun touchStarted() {
        val uiY = height - 320f

        // s1 controls
        if (isTouched(width - 290f, uiY + 25f, 100f, 60f)) s1 = (s1 - 0.05f).coerceAtLeast(0.05f)
        if (isTouched(width - 160f, uiY + 25f, 100f, 60f)) s1 = (s1 + 0.05f).coerceAtMost(3.0f)

        // s2 controls
        if (isTouched(width - 290f, uiY + 105f, 100f, 60f)) s2 = (s2 - 0.05f).coerceAtLeast(0.05f)
        if (isTouched(width - 160f, uiY + 105f, 100f, 60f)) s2 = (s2 + 0.05f).coerceAtMost(3.0f)

        // Mesh toggle & Reset buttons
        if (isTouched(50f, uiY + 195f, (width - 140f) / 2f, 65f)) drawMesh = !drawMesh
        if (isTouched(width / 2f + 20f, uiY + 195f, (width - 140f) / 2f, 65f)) {
            s1 = 0.87f
            s2 = 0.91f
            scaleFactor = 320f
            rotX = 0f
            rotY = 0f
        }
    }

    private fun isTouched(x: Float, y: Float, w: Float, h: Float): Boolean {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }
}
