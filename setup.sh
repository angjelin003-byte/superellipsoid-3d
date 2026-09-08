#!/usr/bin/env bash
set -e

echo "Creating directory structure..."
mkdir -p .github/workflows
mkdir -p app/src/main/res/values
mkdir -p app/src/main/kotlin/com/visualizer

echo "Generating settings.gradle.kts..."
cat << 'EOF' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "superellipsoid-3d"
include(":app")
EOF

echo "Generating root build.gradle.kts..."
cat << 'EOF' > build.gradle.kts
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
EOF

echo "Generating app/build.gradle.kts..."
cat << 'EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.visualizer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.visualizer.superellipsoid"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.processing:android:4.2.0")
}
EOF

echo "Generating AndroidManifest.xml..."
cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:glEsVersion="0x00020000" android:required="true" />

    <application
        android:allowBackup="true"
        android:label="Superellipsoid 3D"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|keyboardHidden|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
EOF

echo "Generating strings.xml..."
cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">Superellipsoid 3D</string>
</resources>
EOF

echo "Generating Math.kt..."
cat << 'EOF' > app/src/main/kotlin/com/visualizer/Math.kt
package com.visualizer

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

data class Vector3(val x: Float, val y: Float, val z: Float)

fun evaluateSuperellipsoid(
    eta: Float,
    omega: Float,
    s1: Float = 0.87f,
    s2: Float = 0.91f,
    scale: Float = 220f
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
EOF

echo "Generating MainActivity.kt..."
cat << 'EOF' > app/src/main/kotlin/com/visualizer/MainActivity.kt
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
EOF

echo "Generating SuperellipsoidVisualizer.kt..."
cat << 'EOF' > app/src/main/kotlin/com/visualizer/SuperellipsoidVisualizer.kt
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
            if (touch.y < height - 350) {
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
            rotY += 0.003f
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

        if (isTouched(width - 290f, uiY + 25f, 100f, 60f)) s1 = (s1 - 0.05f).coerceAtLeast(0.05f)
        if (isTouched(width - 160f, uiY + 25f, 100f, 60f)) s1 = (s1 + 0.05f).coerceAtMost(3.0f)

        if (isTouched(width - 290f, uiY + 105f, 100f, 60f)) s2 = (s2 - 0.05f).coerceAtLeast(0.05f)
        if (isTouched(width - 160f, uiY + 105f, 100f, 60f)) s2 = (s2 + 0.05f).coerceAtMost(3.0f)

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
EOF

echo "Generating .github/workflows/android.yml..."
cat << 'EOF' > .github/workflows/android.yml
name: Build Android APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build-android:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload Android APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

echo "Done! All files created successfully."
