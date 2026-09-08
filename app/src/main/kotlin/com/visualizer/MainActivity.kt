package com.visualizer

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import processing.android.PFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this).apply {
            id = FrameLayout.generateViewId()
        }
        setContentView(frame)

        val sketch = SuperellipsoidVisualizer()
        val fragment = PFragment(sketch)
        fragment.setView(frame, this)
    }
}
