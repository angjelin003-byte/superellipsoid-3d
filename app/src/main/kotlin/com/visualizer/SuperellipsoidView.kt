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

class SuperellipsoidView(context: Context) : View(context) {

    private var rotX: Float = 0f
    private var rotY: Float = 0f
    private var scaleFactor: Float = 300f

    private var s1: Float = 0.87f
    private var s2: Float = 0.91f
    private var drawMesh: Boolean = false

    private val etaSteps: Int = 60
    private val omegaSteps: Int = 120

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

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

        val cx = width.toFloat() / 2.0f
        val cy = height.toFloat() / 2.0f - 100.0f

        rotY += 0.005f

        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val cosY = cos(rotY)
        val sinY = sin(rotY)

        val piFloat = Math.PI.toFloat()
        val etaMin = -piFloat / 2.0f
        val etaMax = piFloat / 2.0f
        val omegaMin = -piFloat
        val omegaMax = piFloat

        val dEta = (etaMax - etaMin) / etaSteps.toFloat()
        val dOmega = (omegaMax - omegaMin) / omegaSteps.toFloat()

        if (drawMesh) {
            val path = Path()
            for (i in 0 until etaSteps) {
                val eta = etaMin + i.toFloat() * dEta
                val etaNext = etaMin + (i + 1).toFloat() * dEta
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j.toFloat() * dOmega
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
                val eta = etaMin + i.toFloat() * dEta
                for (j in 0..omegaSteps) {
                    val omega = omegaMin + j.toFloat() * dOmega
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

        val distance = 1000.0f
        val fov = distance / (distance + z2)
        return floatArrayOf(x2 * fov + cx, y2 * fov + cy)
    }

    private fun drawHUD(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val uiY = h - 320.0f

        val rect = RectF(20.0f, uiY, w - 20.0f, h - 40.0f)
        canvas.drawRoundRect(rect, 30.0f, 30.0f, buttonBgPaint)
        canvas.drawRoundRect(rect, 30.0f, 30.0f, buttonBorderPaint)

        canvas.drawText(String.format("s1 = %.2f", s1), 50.0f, uiY + 70.0f, textPaint)
        canvas.drawText(String.format("s2 = %.2f", s2), 50.0f, uiY + 150.0f, textPaint)

        drawBtn(canvas, w - 290.0f, uiY + 25.0f, 100.0f, 60.0f, "-")
        drawBtn(canvas, w - 160.0f, uiY + 25.0f, 100.0f, 60.0f, "+")

        drawBtn(canvas, w - 290.0f, uiY + 105.0f, 100.0f, 60.0f, "-")
        drawBtn(canvas, w - 160.0f, uiY + 105.0f, 100.0f, 60.0f, "+")

        val btnW = (w - 140.0f) / 2.0f
        drawBtn(canvas, 50.0f, uiY + 195.0f, btnW, 65.0f, if (drawMesh) "Points" else "Mesh")
        drawBtn(canvas, w / 2.0f + 20.0f, uiY + 195.0f, btnW, 65.0f, "Reset")
    }

    private fun drawBtn(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 15.0f, 15.0f, buttonBgPaint)
        canvas.drawRoundRect(rect, 15.0f, 15.0f, buttonBorderPaint)
        val prevAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x + w / 2.0f, y + h / 2.0f + 12.0f, textPaint)
        textPaint.textAlign = prevAlign
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val w = width.toFloat()
        val h = height.toFloat()
        val uiY = h - 320.0f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                if (event.y >= uiY) {
                    if (isTouchInside(event.x, event.y, w - 290.0f, uiY + 25.0f, 100.0f, 60.0f)) s1 = (s1 - 0.05f).coerceAtLeast(0.05f)
                    if (isTouchInside(event.x, event.y, w - 160.0f, uiY + 25.0f, 100.0f, 60.0f)) s1 = (s1 + 0.05f).coerceAtMost(3.0f)

                    if (isTouchInside(event.x, event.y, w - 290.0f, uiY + 105.0f, 100.0f, 60.0f)) s2 = (s2 - 0.05f).coerceAtLeast(0.05f)
                    if (isTouchInside(event.x, event.y, w - 160.0f, uiY + 105.0f, 100.0f, 60.0f)) s2 = (s2 + 0.05f).coerceAtMost(3.0f)

                    val btnW = (w - 140.0f) / 2.0f
                    if (isTouchInside(event.x, event.y, 50.0f, uiY + 195.0f, btnW, 65.0f)) drawMesh = !drawMesh
                    if (isTouchInside(event.x, event.y, w / 2.0f + 20.0f, uiY + 195.0f, btnW, 65.0f)) {
                        s1 = 0.87f
                        s2 = 0.91f
                        scaleFactor = 300.0f
                        rotX = 0.0f
                        rotY = 0.0f
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
