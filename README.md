# 3D Superellipsoid Parametric Visualizer

Interactive 3D visualization of a superellipsoid surface using Kotlin and Processing.

## Mathematical Formulation

$$\mathbf{r}(\eta, \omega) = \begin{pmatrix} \operatorname{sgn}(\cos \eta \cos \omega) |\cos \eta|^{s_1} |\cos \omega|^{s_2} \\ \operatorname{sgn}(\cos \eta \sin \omega) |\cos \eta|^{s_1} |\sin \omega|^{s_2} \\ \operatorname{sgn}(\sin \eta) |\sin \eta|^{s_1} \end{pmatrix}$$

### Parameters
* **$\eta$ Domain**: $[-\frac{\pi}{2}, \frac{\pi}{2}]$
* **$\omega$ Domain**: $[-\pi, \pi]$
* **Exponents**: $s_1 = 0.87$, $s_2 = 0.91$

## Run Project

```bash
./gradlew run
