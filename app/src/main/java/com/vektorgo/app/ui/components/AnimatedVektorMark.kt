package com.vektorgo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * The Vektor "V" mark used in the app's top bar, filled with a
 * light-to-dark gradient that continuously drifts up and down — same
 * navy-to-cyan family as the logo, but with motion instead of a fixed
 * gradient, since a launcher icon (static by OS constraint) can't animate
 * but an in-app composable can.
 */
@Composable
fun AnimatedVektorMark(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vektor_mark_shimmer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vektor_mark_phase"
    )

    Canvas(
        modifier = modifier.background(Color(0xFF0A0C10), RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height

        val vPath = Path().apply {
            moveTo(w * 0.08f, h * 0.20f)
            lineTo(w * 0.28f, h * 0.20f)
            lineTo(w * 0.47f, h * 0.62f)
            lineTo(w * 0.76f, h * 0.12f)
            lineTo(w * 0.94f, h * 0.12f)
            lineTo(w * 0.55f, h * 0.90f)
            lineTo(w * 0.38f, h * 0.90f)
            close()
        }

        // The gradient's own start/end drift with `phase`, so the
        // light/dark bands slide through the shape instead of sitting
        // still — a slower, subtler cousin of a loading shimmer.
        val travel = h * 0.6f
        val brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4AD8F0), // cyan — logo's light end
                Color(0xFF2D7FE0), // mid blue
                Color(0xFF131F5C)  // navy — logo's dark end
            ),
            startY = -travel + (travel * 2f) * phase,
            endY = h - travel + (travel * 2f) * phase
        )

        drawPath(path = vPath, brush = brush)
    }
}
