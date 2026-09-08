package com.visualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SuperellipsoidView(context: Context) : View(context) {

    private var rotX = 0f
    private var rotY = 0f
    private var scaleFactor = 300f

    private var s1 = 0.87f
    private var s2 = 0.91f
    private var drawMesh = false

    private val etaSteps = 60
    private val omegaSteps = 120

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E66EFF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D4032E6")
        strokeWidth = 9f
        style = Paint.Style.STROKE
    }

    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80E66EFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
    }

    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2D1946")
        style = Paint.Style.FILL
    }

    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C86EFF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(100f, 900f)
            invalidate()
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#08040F"))

        val cx = width / 2f
        val cy = height / 2f - 100f

        rotY += 0.005f

        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val cosY = cos(rotY)
        val sinY = sin(rotY)

        val etaMin = -Math.PI.toFloat() / 2f
        val etaMax = Math.PI.toFloat() / 2f
        val omegaMin = -Math.PI.toFloat()
        val omegaMax = Math.PI.toFloat()

        val dEta = (etaMax - etaMin) / etaSteps
        val dOmega = (omegaMax - omegaMin) / omegaSteps

        if (drawMesh) {
            val path = Path()
            for (i in 0 until etaSteps) {
                val eta = etaMin + i * dEta
                val etaNext = etaMin + (i + 1) * dEta
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j * dOmega
                    val p1 = project(evaluateSuperellipsoid(eta, omega, s1, s2, scaleFactor), cosX, sinX, cosY, sinY, cx, cy)
                    val p2 = project(evaluateSuperellipsoid(etaNext, omega, s1, s2, scaleFactor), cosX, sinX, cosY, sinY, cx, cy)

                    if (j == 0) path.moveTo(p1[0], p1[1])
                    else path.lineTo(p1[0], p1[1])
                    path.lineTo(p2[0], p2[1])
                }
            }
            canvas.drawPath(path, meshPaint)
        } else {
            val points = FloatArray((etaSteps + 1) * (omegaSteps + 1) * 2)
            var idx = 0
            for (i in 0..etaSteps) {
                val eta = etaMin + i * dEta
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j * dOmega
                    val p = evaluateSuperellipsoid(eta, omega, s1, s2, scaleFactor)
                    val projected = project(p, cosX, sinX, cosY, sinY, cx, cy)
                    points[idx++] = projected[0]
                    points[idx++] = projected[1]
                }
            }
            canvas.drawPoints(points, haloPaint)
            canvas.drawPoints(points, pointPaint)
        }

        drawHUD(canvas)
        postInvalidateOnAnimation()
    }

    private fun project(v: Vector3, cosX: Float, sinX: Float, cosY: Float, sinY: Float, cx: Float, cy: Float): FloatArray {
        val x1 = v.x * cosY + v.z * sinY
        val y1 = v.y
        val z1 = -v.x * sinY + v.z * cosY

        val x2 = x1
        val y2 = y1 * cosX - z1 * sinX
        val z2 = y1 * sinX + z1 * cosX

        val distance = 1000f
        val fov = distance / (distance + z2)
        return floatArrayOf(x2 * fov + cx, y2 * fov + cy)
    }

    private fun drawHUD(canvas: Canvas) {
        val uiY = height - 320f
        canvas.drawRoundRect(20f, uiY, width - 20f, height - 40f, 30f, 30f, buttonBgPaint)
        canvas.drawRoundRect(20f, uiY, width - 20f, height - 40f, 30f, 30f, buttonBorderPaint)

        canvas.drawText(String.format("s1 = %.2f", s1), 50f, uiY + 70f, textPaint)
        canvas.drawText(String.format("s2 = %.2f", s2), 50f, uiY + 150f, textPaint)

        drawBtn(canvas, width - 290f, uiY + 25f, 100f, 60f, "-")
        drawBtn(canvas, width - 160f, uiY + 25f, 100f, 60f, "+")

        drawBtn(canvas, width - 290f, uiY + 105f, 100f, 60f, "-")
        drawBtn(canvas, width - 160f, uiY + 105f, 100f, 60f, "+")

        val btnW = (width - 140f) / 2f
        drawBtn(canvas, 50f, uiY + 195f, btnW, 65f, if (drawMesh) "Points" else "Mesh")
        drawBtn(canvas, width / 2f + 20f, uiY + 195f, btnW, 65f, "Reset")
    }

    private fun drawBtn(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String) {
        canvas.drawRoundRect(x, y, x + w, y + h, 15f, 15f, buttonBgPaint)
        canvas.drawRoundRect(x, y, x + w, y + h, 15f, 15f, buttonBorderPaint)
        val prevAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x + w / 2f, y + h / 2f + 12f, textPaint)
        textPaint.textAlign = prevAlign
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val uiY = height - 320f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                if (event.y >= uiY) {
                    if (isTouchInside(event.x, event.y, width - 290f, uiY + 25f, 100f, 60f)) s1 = (s1 - 0.05f).coerceAtLeast(0.05f)
                    if (isTouchInside(event.x, event.y, width - 160f, uiY + 25f, 100f, 60f)) s1 = (s1 + 0.05f).coerceAtMost(3.0f)

                    if (isTouchInside(event.x, event.y, width - 290f, uiY + 105f, 100f, 60f)) s2 = (s2 - 0.05f).coerceAtLeast(0.05f)
                    if (isTouchInside(event.x, event.y, width - 160f, uiY + 105f, 100f, 60f)) s2 = (s2 + 0.05f).coerceAtMost(3.0f)

                    val btnW = (width - 140f) / 2f
                    if (isTouchInside(event.x, event.y, 50f, uiY + 195f, btnW, 65f)) drawMesh = !drawMesh
                    if (isTouchInside(event.x, event.y, width / 2f + 20f, uiY + 195f, btnW, 65f)) {
                        s1 = 0.87f
                        s2 = 0.91f
                        scaleFactor = 300f
                        rotX = 0f
                        rotY = 0f
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.y < uiY && !scaleDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    rotY += dx * 0.008f
                    rotX -= dy * 0.008f
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
        }
        invalidate()
        return true
    }

    private fun isTouchInside(tx: Float, ty: Float, x: Float, y: Float, w: Float, h: Float): Boolean {
        return tx in x..(x + w) && ty in y..(y + h)
    }
}
