package com.visualizer.experiments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.visualizer.Vector3
import com.visualizer.evaluateSuperellipsoid
import kotlin.math.cos
import kotlin.math.sin

class ExperimentalMorphView(context: Context) : View(context) {

    private var morphProgress: Float = 0f
    private var rotX: Float = 0f
    private var rotY: Float = 0f

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val scale = 250f

        val etaSteps = 30
        val omegaSteps = 60

        val pi = Math.PI.toFloat()
        val dEta = pi / etaSteps
        val dOmega = (2 * pi) / omegaSteps

        path.reset()

        for (i in 0..etaSteps) {
            val eta = -pi / 2f + i * dEta
            for (j in 0..omegaSteps) {
                val omega = -pi + j * dOmega

                val s1 = 1f - morphProgress * 0.8f
                val s2 = 1f - morphProgress * 0.8f

                val v: Vector3 = evaluateSuperellipsoid(eta, omega, s1, s2, scale)

                val x1 = v.x * cos(rotY) + v.z * sin(rotY)
                val y1 = v.y
                val z1 = -v.x * sin(rotY) + v.z * cos(rotY)

                val x2 = x1
                val y2 = y1 * cos(rotX) - z1 * sin(rotX)

                val screenX = x2 + cx
                val screenY = y2 + cy

                if (j == 0) {
                    path.moveTo(screenX, screenY)
                } else {
                    path.lineTo(screenX, screenY)
                }
            }
        }

        canvas.drawPath(path, paint)
        postInvalidateOnAnimation()
    }

    fun setMorphProgress(progress: Float) {
        this.morphProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }
}
