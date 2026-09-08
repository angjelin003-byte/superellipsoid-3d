package com.visualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

enum class WireframeStyle(val label: String) {
    POINTS("Points"),
    GRID("Grid Mesh"),
    LATITUDE("Latitude Rings"),
    LONGITUDE("Longitude Rings")
}

class SuperellipsoidView(context: Context) : View(context) {

    private var rotX: Float = 0f
    private var rotY: Float = 0f
    private var scaleFactor: Float = 300f

    private var s1: Float = 0.87f
    private var s2: Float = 0.91f
    private var rotSpeedY: Float = 0.005f
    private var bgHue: Float = 260f

    private var isPerspective: Boolean = true
    private var wireframeStyleIndex: Int = 1

    // Mesh Resolution
    private val etaSteps: Int = 40
    private val omegaSteps: Int = 80
    private val totalPoints = (etaSteps + 1) * (omegaSteps + 1)

    // Pre-allocated Cache Buffers (Zero Allocation in onDraw)
    private val baseMesh = FloatArray(totalPoints * 3)
    private val projectedMesh = FloatArray(totalPoints * 3) // x, y, depth
    private val pointBuffer = FloatArray(totalPoints * 2)
    private val reusablePath = Path()
    private val hsvArray = floatArrayOf(260f, 0.65f, 0.08f)

    // Touch & HUD Tracking
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var activeSlider: Int = -1
    private var isUserInteracting: Boolean = false

    // FPS Counter
    private var lastFrameTime = System.currentTimeMillis()
    private var fps = 0
    private var frameCount = 0
    private var fpsTimer = System.currentTimeMillis()

    // Paints
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E66EFF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80E66EFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
    }

    private val panelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60D071A")
        style = Paint.Style.FILL
    }

    private val panelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C86EFF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val sliderTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3B69")
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val sliderFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C86EFF")
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val sliderThumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFC8")
        style = Paint.Style.FILL
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(100f, 900f)
            return true
        }
    })

    init {
        recalculateBaseMesh()
    }

    /**
     * Pre-calculates 3D coordinates into baseMesh buffer.
     * Called ONLY when exponents s1 or s2 change.
     */
    private fun recalculateBaseMesh() {
        val piFloat = Math.PI.toFloat()
        val etaMin = -piFloat / 2.0f
        val etaMax = piFloat / 2.0f
        val omegaMin = -piFloat
        val omegaMax = piFloat

        val dEta = (etaMax - etaMin) / etaSteps.toFloat()
        val dOmega = (omegaMax - omegaMin) / omegaSteps.toFloat()

        var idx = 0
        for (i in 0..etaSteps) {
            val eta = etaMin + i.toFloat() * dEta
            val cosEta = cos(eta)
            val sinEta = sin(eta)
            val absCosEtaPow = abs(cosEta).pow(s1)
            val z = sign(sinEta) * abs(sinEta).pow(s1)

            for (j in 0..omegaSteps) {
                val omega = omegaMin + j.toFloat() * dOmega
                val cosOmega = cos(omega)
                val sinOmega = sin(omega)

                val x = sign(cosEta * cosOmega) * absCosEtaPow * abs(cosOmega).pow(s2)
                val y = sign(cosEta * sinOmega) * absCosEtaPow * abs(sinOmega).pow(s2)

                baseMesh[idx++] = x
                baseMesh[idx++] = y
                baseMesh[idx++] = z
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate FPS
        val currentTime = System.currentTimeMillis()
        frameCount++
        if (currentTime - fpsTimer >= 1000) {
            fps = frameCount
            frameCount = 0
            fpsTimer = currentTime
        }

        // Draw Dynamic Background
        hsvArray[0] = bgHue
        canvas.drawColor(Color.HSVToColor(hsvArray))

        val cx = width.toFloat() / 2.0f
        val cy = height.toFloat() / 2.0f - 180.0f

        rotY += rotSpeedY

        // Precompute rotation matrices
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val cosY = cos(rotY)
        val sinY = sin(rotY)

        // Project 3D Mesh to 2D Screen Space
        var meshIdx = 0
        val stepStride = if (isUserInteracting && activeSlider == -1) 2 else 1 // Dynamic LOD during fast manual drag

        for (i in 0 until totalPoints) {
            val vx = baseMesh[meshIdx] * scaleFactor
            val vy = baseMesh[meshIdx + 1] * scaleFactor
            val vz = baseMesh[meshIdx + 2] * scaleFactor

            // Matrix Rotation
            val x1 = vx * cosY + vz * sinY
            val y1 = vy
            val z1 = -vx * sinY + vz * cosY

            val x2 = x1
            val y2 = y1 * cosX - z1 * sinX
            val z2 = y1 * sinX + z1 * cosX

            // Perspective / Isometric Projection
            if (isPerspective) {
                val distance = 1000.0f
                val fov = distance / (distance + z2)
                projectedMesh[meshIdx] = x2 * fov + cx
                projectedMesh[meshIdx + 1] = y2 * fov + cy
                projectedMesh[meshIdx + 2] = fov
            } else {
                projectedMesh[meshIdx] = x2 + cx
                projectedMesh[meshIdx + 1] = y2 + cy
                projectedMesh[meshIdx + 2] = 1.0f
            }
            meshIdx += 3
        }

        // Render Geometry
        when (WireframeStyle.values()[wireframeStyleIndex]) {
            WireframeStyle.POINTS -> {
                var ptr = 0
                for (i in 0 until totalPoints step stepStride) {
                    val pIdx = i * 3
                    pointBuffer[ptr++] = projectedMesh[pIdx]
                    pointBuffer[ptr++] = projectedMesh[pIdx + 1]
                }
                canvas.drawPoints(pointBuffer, 0, ptr, pointPaint)
            }
            WireframeStyle.GRID, WireframeStyle.LATITUDE, WireframeStyle.LONGITUDE -> {
                reusablePath.reset()
                val style = WireframeStyle.values()[wireframeStyleIndex]

                if (style == WireframeStyle.GRID || style == WireframeStyle.LATITUDE) {
                    for (i in 0..etaSteps step stepStride) {
                        for (j in 0..omegaSteps step stepStride) {
                            val pIdx = (i * (omegaSteps + 1) + j) * 3
                            val px = projectedMesh[pIdx]
                            val py = projectedMesh[pIdx + 1]

                            if (j == 0) reusablePath.moveTo(px, py)
                            else reusablePath.lineTo(px, py)
                        }
                    }
                }

                if (style == WireframeStyle.GRID || style == WireframeStyle.LONGITUDE) {
                    for (j in 0..omegaSteps step stepStride) {
                        for (i in 0..etaSteps step stepStride) {
                            val pIdx = (i * (omegaSteps + 1) + j) * 3
                            val px = projectedMesh[pIdx]
                            val py = projectedMesh[pIdx + 1]

                            if (i == 0) reusablePath.moveTo(px, py)
                            else reusablePath.lineTo(px, py)
                        }
                    }
                }
                canvas.drawPath(reusablePath, meshPaint)
            }
        }

        drawHUD(canvas)
        postInvalidateOnAnimation()
    }

    private fun drawHUD(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val uiY = h - 500.0f

        val rect = RectF(16.0f, uiY, w - 16.0f, h - 20.0f)
        canvas.drawRoundRect(rect, 24.0f, 24.0f, panelBgPaint)
        canvas.drawRoundRect(rect, 24.0f, 24.0f, panelBorderPaint)

        // Performance FPS Display
        textPaint.color = Color.GREEN
        canvas.drawText("FPS: $fps", 40.0f, 60.0f, textPaint)
        textPaint.color = Color.WHITE

        val trackWidth = w - 300.0f
        val sliderX = 230.0f

        drawSlider(canvas, "s1 Exp", String.format("%.2f", s1), s1, 0.1f, 3.0f, sliderX, uiY + 50.0f, trackWidth)
        drawSlider(canvas, "s2 Exp", String.format("%.2f", s2), s2, 0.1f, 3.0f, sliderX, uiY + 130.0f, trackWidth)

        val speedText = if (abs(rotSpeedY) < 0.001f) "0 (Paused)" else String.format("%.3f", rotSpeedY)
        drawSlider(canvas, "Rot Speed", speedText, rotSpeedY, -0.05f, 0.05f, sliderX, uiY + 210.0f, trackWidth)

        drawSlider(canvas, "BG Hue", String.format("%.0f°", bgHue), bgHue, 0f, 360f, sliderX, uiY + 290.0f, trackWidth)

        val btnY = uiY + 370.0f
        val btnW = (w - 80.0f) / 3.0f

        drawBtn(canvas, 32.0f, btnY, btnW, 75.0f, if (isPerspective) "Perspective" else "Isometric")
        drawBtn(canvas, 48.0f + btnW, btnY, btnW, 75.0f, WireframeStyle.values()[wireframeStyleIndex].label)
        drawBtn(canvas, 64.0f + btnW * 2, btnY, btnW, 75.0f, "Reset")
    }

    private fun drawSlider(
        canvas: Canvas,
        label: String,
        valueStr: String,
        value: Float,
        min: Float,
        max: Float,
        x: Float,
        y: Float,
        width: Float
    ) {
        val prevAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("$label: $valueStr", 40.0f, y + 8.0f, textPaint)
        textPaint.textAlign = prevAlign

        val progress = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val thumbX = x + progress * width

        canvas.drawLine(x, y, x + width, y, sliderTrackPaint)
        canvas.drawLine(x, y, thumbX, y, sliderFillPaint)
        canvas.drawCircle(thumbX, y, 18.0f, sliderThumbPaint)
    }

    private fun drawBtn(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 14.0f, 14.0f, panelBgPaint)
        canvas.drawRoundRect(rect, 14.0f, 14.0f, panelBorderPaint)
        val prevAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x + w / 2.0f, y + h / 2.0f + 10.0f, textPaint)
        textPaint.textAlign = prevAlign
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val w = width.toFloat()
        val h = height.toFloat()
        val uiY = h - 500.0f
        val trackWidth = w - 300.0f
        val sliderX = 230.0f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isUserInteracting = true
                lastTouchX = event.x
                lastTouchY = event.y

                if (event.y >= uiY) {
                    activeSlider = when {
                        isTouchNearSlider(event.y, uiY + 50.0f) -> 0
                        isTouchNearSlider(event.y, uiY + 130.0f) -> 1
                        isTouchNearSlider(event.y, uiY + 210.0f) -> 2
                        isTouchNearSlider(event.y, uiY + 290.0f) -> 3
                        else -> -1
                    }

                    if (activeSlider != -1) {
                        updateSliderValue(activeSlider, event.x, sliderX, trackWidth)
                    } else {
                        val btnY = uiY + 370.0f
                        val btnW = (w - 80.0f) / 3.0f

                        if (isTouchInside(event.x, event.y, 32.0f, btnY, btnW, 75.0f)) {
                            isPerspective = !isPerspective
                        } else if (isTouchInside(event.x, event.y, 48.0f + btnW, btnY, btnW, 75.0f)) {
                            wireframeStyleIndex = (wireframeStyleIndex + 1) % WireframeStyle.values().size
                        } else if (isTouchInside(event.x, event.y, 64.0f + btnW * 2, btnY, btnW, 75.0f)) {
                            s1 = 0.87f
                            s2 = 0.91f
                            rotSpeedY = 0.005f
                            bgHue = 260f
                            isPerspective = true
                            wireframeStyleIndex = 1
                            scaleFactor = 300.0f
                            rotX = 0.0f
                            rotY = 0.0f
                            recalculateBaseMesh()
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeSlider != -1) {
                    updateSliderValue(activeSlider, event.x, sliderX, trackWidth)
                } else if (event.y < uiY && !scaleDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    rotY += dx * 0.008f
                    rotX -= dy * 0.008f
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserInteracting = false
                activeSlider = -1
            }
        }
        return true
    }

    private fun isTouchNearSlider(touchY: Float, sliderCenterY: Float): Boolean {
        return abs(touchY - sliderCenterY) <= 35.0f
    }

    private fun updateSliderValue(sliderIndex: Int, touchX: Float, sliderX: Float, trackWidth: Float) {
        val fraction = ((touchX - sliderX) / trackWidth).coerceIn(0f, 1f)
        when (sliderIndex) {
            0 -> {
                s1 = 0.1f + fraction * (3.0f - 0.1f)
                recalculateBaseMesh()
            }
            1 -> {
                s2 = 0.1f + fraction * (3.0f - 0.1f)
                recalculateBaseMesh()
            }
            2 -> {
                val rawVal = -0.05f + fraction * 0.10f
                rotSpeedY = if (abs(rawVal) < 0.002f) 0.0f else rawVal
            }
            3 -> bgHue = fraction * 360.0f
        }
    }

    private fun isTouchInside(tx: Float, ty: Float, x: Float, y: Float, w: Float, h: Float): Boolean {
        return tx in x..(x + w) && ty in y..(y + h)
    }
}
