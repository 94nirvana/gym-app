package com.gymapp.minimalist.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class ChartPoint(
    val timestamp: Long,
    val value: Float,
    val label: String = ""
)

class ProgressChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val points = mutableListOf<ChartPoint>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981") // Accent emerald
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(16f)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }

    private val pointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D1117") // Background color
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#21262D")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val path = Path()
    private val fillPath = Path()
    private val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())

    fun setData(newPoints: List<ChartPoint>) {
        points.clear()
        points.addAll(newPoints.takeLast(10)) // Mostra até 10 sessões mais recentes
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0 || h <= 0) return

        val paddingLeft = 50f
        val paddingRight = 50f
        val paddingTop = 60f
        val paddingBottom = 60f

        val contentWidth = w - paddingLeft - paddingRight
        val contentHeight = h - paddingTop - paddingBottom

        if (points.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8B949E")
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Sem histórico de treinos ainda", w / 2f, h / 2f, emptyPaint)
            return
        }

        // Desenha linha de base horizontal da grade
        canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, gridPaint)

        if (points.size == 1) {
            val single = points[0]
            val cx = w / 2f
            val cy = h / 2f
            canvas.drawCircle(cx, cy, 10f, pointPaint)
            canvas.drawCircle(cx, cy, 5f, pointInnerPaint)
            canvas.drawText("${single.value.toInt()} kg", cx, cy - 25f, valueTextPaint)
            val dateLabel = if (single.label.isNotBlank()) single.label else dateFormatter.format(Date(single.timestamp))
            canvas.drawText(dateLabel, cx, h - paddingBottom + 40f, textPaint)
            return
        }

        var minValue = points.minOf { it.value }
        var maxValue = points.maxOf { it.value }

        // Evita divisão por zero se todos os valores forem iguais
        if (maxValue == minValue) {
            maxValue += 10f
            minValue = max(0f, minValue - 10f)
        } else {
            val margin = (maxValue - minValue) * 0.15f
            maxValue += margin
            minValue = max(0f, minValue - margin)
        }

        val stepX = contentWidth / (points.size - 1)
        val valueRange = maxValue - minValue

        path.reset()
        fillPath.reset()

        val coords = mutableListOf<Pair<Float, Float>>()

        for (i in points.indices) {
            val p = points[i]
            val x = paddingLeft + (i * stepX)
            val normalizedY = (p.value - minValue) / valueRange
            val y = (h - paddingBottom) - (normalizedY * contentHeight)
            coords.add(Pair(x, y))

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - paddingBottom)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(coords.last().first, h - paddingBottom)
        fillPath.close()

        // Gradiente vertical para o preenchimento suave
        fillPaint.shader = LinearGradient(
            0f, paddingTop,
            0f, h - paddingBottom,
            Color.parseColor("#3310B981"),
            Color.parseColor("#0510B981"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        // Desenha os pontos, rótulos de data e valores
        for (i in coords.indices) {
            val (x, y) = coords[i]
            val p = points[i]

            canvas.drawCircle(x, y, 9f, pointPaint)
            canvas.drawCircle(x, y, 4.5f, pointInnerPaint)

            // Valor em kg acima do ponto
            val valueLabel = if (p.value >= 1000f) {
                "%.1ft".format(p.value / 1000f)
            } else {
                "${p.value.toInt()}kg"
            }
            canvas.drawText(valueLabel, x, y - 20f, valueTextPaint)

            // Rótulo de data abaixo da linha de base
            val dateLabel = if (p.label.isNotBlank()) p.label else dateFormatter.format(Date(p.timestamp))
            canvas.drawText(dateLabel, x, h - paddingBottom + 40f, textPaint)
        }
    }
}
