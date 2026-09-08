package com.visualizer.experiments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.visualizer.Vector3
import com.visualizer.evaluateSuperellipsoid
import kotlin.math.sin

class ExperimentalMorphView(context: Context) : View(context) {

    private var animTime = 0.0f
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFC8")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#050A14"))

        animTime += 0.03f
        val dynamicS1 = 0.5f + 0.5f * (1.0f + sin(animTime))
        val dynamicS2 = 0.5f + 0.5f * (1.0f + sin(animTime * 0.7f))

        val cx = width.toFloat() / 2.0f
        val cy = height.toFloat() / 2.0f
        val scale = 250.0f

        val points = FloatArray(31 * 61 * 2)
        var idx = 0

        val piFloat = Math.PI.toFloat()
        val dEta = piFloat / 30.0f
        val dOmega = (2.0f * piFloat) / 60.0f

        for (i in 0..30) {
            val eta = -piFloat / 2.0f + i * dEta
            for (j in 0..60) {
                val omega = -piFloat + j * dOmega
                val v: Vector3 = evaluateSuperellipsoid(eta, omega, dynamicS1, dynamicS2, scale)
                points[idx++] = v.x + cx
                points[idx++] = v.y + cy
            }
        }

        canvas.drawPoints(points, pointPaint)
        postInvalidateOnAnimation()
    }
}
