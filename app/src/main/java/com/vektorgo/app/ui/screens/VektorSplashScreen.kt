package com.vektorgo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandStart = Color(0xFF2255A4)
private val BrandMid = Color(0xFF1B3E8F)
private val BrandEnd = Color(0xFF16307A)
private val WaveInner = Color(0xFF5DD3E5)
private val WaveMid = Color(0xFF94E4EF)
private val WaveOuter = Color(0xFFC6F3FA)

/**
 * Splash sequence, played once on cold start before the Dashboard: the V
 * draws itself stroke-first, fills solid, the three signal waves cascade
 * in, and only once everything has landed does a light sweep cross the
 * icon — mirroring how the animated top-bar mark reads as "alive", but as
 * a one-shot intro instead of a loop.
 */
@Composable
fun VektorSplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1800, easing = LinearEasing))
        onFinished()
    }
    val p = progress.value

    // Timeline, as fractions of the 1.8s run:
    // 0.00-0.38 draw the V outline · 0.38-0.48 fill solid
    // 0.44-0.64 waves cascade in, staggered · 0.56-0.86 shine sweep
    // 0.62-0.77 wordmark fades up
    val drawFraction = (p / 0.38f).coerceIn(0f, 1f)
    val fillAlpha = ((p - 0.38f) / 0.10f).coerceIn(0f, 1f)
    val wave1Alpha = ((p - 0.44f) / 0.08f).coerceIn(0f, 1f)
    val wave2Alpha = ((p - 0.52f) / 0.08f).coerceIn(0f, 1f)
    val wave3Alpha = ((p - 0.58f) / 0.08f).coerceIn(0f, 1f)
    val shineProgress = ((p - 0.56f) / 0.30f).coerceIn(0f, 1f)
    val wordAlpha = ((p - 0.62f) / 0.15f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BrandStart, BrandMid, BrandEnd))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val vPath = Path().apply {
                    moveTo(w * 0.20f, h * 0.25f)
                    lineTo(w * 0.35f, h * 0.25f)
                    lineTo(w * 0.50f, h * 0.62f)
                    lineTo(w * 0.65f, h * 0.25f)
                    lineTo(w * 0.80f, h * 0.25f)
                    lineTo(w * 0.55f, h * 0.82f)
                    lineTo(w * 0.45f, h * 0.82f)
                    close()
                }

                if (drawFraction < 1f) {
                    val measure = PathMeasure().apply { setPath(vPath, false) }
                    val partial = Path()
                    measure.getSegment(0f, measure.length * drawFraction, partial, true)
                    drawPath(
                        partial,
                        color = Color.White,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                if (fillAlpha > 0f) {
                    drawPath(vPath, color = Color.White.copy(alpha = fillAlpha))
                }

                fun waveArc(alpha: Float, offset: Float, strokeColor: Color) {
                    if (alpha <= 0f) return
                    val arc = Path().apply {
                        moveTo(w * (0.62f + offset), h * (0.38f - offset))
                        cubicTo(
                            w * (0.68f + offset), h * (0.42f - offset),
                            w * (0.71f + offset), h * (0.48f - offset),
                            w * (0.71f + offset), h * 0.54f
                        )
                    }
                    drawPath(
                        arc,
                        color = strokeColor.copy(alpha = alpha),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                waveArc(wave1Alpha, 0f, WaveInner)
                waveArc(wave2Alpha, 0.06f, WaveMid)
                waveArc(wave3Alpha, 0.12f, WaveOuter)
            }

            if (shineProgress > 0f && shineProgress < 1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bandWidth = w * 0.35f
                    val centerX = -bandWidth + (w + bandWidth * 2f) * shineProgress
                    val skew = h * 0.3f
                    val shinePath = Path().apply {
                        moveTo(centerX - bandWidth / 2f, -skew)
                        lineTo(centerX + bandWidth / 2f, -skew)
                        lineTo(centerX + bandWidth / 2f + skew, h + skew)
                        lineTo(centerX - bandWidth / 2f + skew, h + skew)
                        close()
                    }
                    drawPath(shinePath, color = Color.White.copy(alpha = 0.35f))
                }
            }
        }

        Text(
            text = "Vektor Go",
            color = Color.White.copy(alpha = wordAlpha),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}
