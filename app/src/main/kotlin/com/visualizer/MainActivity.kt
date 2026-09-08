package com.visualizer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.visualizer.experiments.ExperimentalMorphView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Switch views by changing the line below:
        setContentView(SuperellipsoidView(this))
        // setContentView(ExperimentalMorphView(this))
    }
}
