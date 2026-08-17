package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ArcaQrCodeView(
    qrData: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 16.dp)) {
            val canvasSize = size.width
            val gridSize = 21 // Standard QR Version 1 grid size
            val cellSize = canvasSize / gridSize
            val hash = qrData.hashCode()

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            drawFinderPattern(0f, 0f, cellSize)
            drawFinderPattern((gridSize - 7) * cellSize, 0f, cellSize)
            drawFinderPattern(0f, (gridSize - 7) * cellSize, cellSize)

            // Draw Matrix Data modules (pseudo-matrix based on hash & data)
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    // Skip finder pattern zones
                    val inTopLeft = row < 8 && col < 8
                    val inTopRight = row < 8 && col >= gridSize - 8
                    val inBottomLeft = row >= gridSize - 8 && col < 8

                    if (!inTopLeft && !inTopRight && !inBottomLeft) {
                        // Generate deterministic module state
                        val cellSeed = (row * 37 + col * 19 + hash + qrData.length)
                        val isFilled = (cellSeed % 7 == 0) || (cellSeed % 3 == 0) || ((row + col) % 2 == 0 && (row * col) % 3 == 0)

                        if (isFilled) {
                            drawRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize * 0.95f, cellSize * 0.95f)
                            )
                        }
                    }
                }
            }

            // Alignment pattern
            val alignCenter = 14
            val alignTopLeft = (alignCenter - 2) * cellSize
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(alignTopLeft, alignTopLeft),
                size = Size(5 * cellSize, 5 * cellSize),
                style = Stroke(width = cellSize)
            )
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(alignCenter * cellSize, alignCenter * cellSize),
                size = Size(cellSize, cellSize)
            )
        }
    }
}

private fun DrawScope.drawFinderPattern(x: Float, y: Float, cellSize: Float) {
    val outerSize = 7 * cellSize
    // Outer black box
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x, y),
        size = Size(outerSize, outerSize)
    )
    // Inner white gap
    drawRect(
        color = Color.White,
        topLeft = Offset(x + cellSize, y + cellSize),
        size = Size(5 * cellSize, 5 * cellSize)
    )
    // Center black module (3x3)
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x + 2 * cellSize, y + 2 * cellSize),
        size = Size(3 * cellSize, 3 * cellSize)
    )
}
