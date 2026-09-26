package com.lis.clash.desktop

import kotlin.math.floor
import kotlin.math.min

/** Screen coordinates are column/x and row/y; DAT storage always has 100 cells per row. */
data class MapViewport(val cellSize: Float = 8f, val x: Float = 24f, val y: Float = 24f) {
    fun tileAt(screenX: Float, screenY: Float, rows: Int = 100, columns: Int = 100): Int? {
        val column = floor((screenX - x) / cellSize).toInt()
        val row = floor((screenY - y) / cellSize).toInt()
        return if (row in 0 until rows && column in 0 until columns) row * 100 + column else null
    }

    fun zoom(factor: Float, anchorX: Float, anchorY: Float): MapViewport {
        val size = (cellSize * factor).coerceIn(2f, 72f)
        val ratio = size / cellSize
        return MapViewport(size, anchorX - (anchorX - x) * ratio, anchorY - (anchorY - y) * ratio)
    }

    fun pan(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)

    companion object {
        fun fit(width: Float, height: Float, rows: Int, columns: Int): MapViewport {
            val size = min((width - 48f) / columns.coerceAtLeast(1), (height - 48f) / rows.coerceAtLeast(1)).coerceIn(2f, 72f)
            return MapViewport(size, (width - columns * size) / 2f, (height - rows * size) / 2f)
        }
    }
}
