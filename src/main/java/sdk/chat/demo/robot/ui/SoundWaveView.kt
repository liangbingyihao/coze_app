package sdk.chat.demo.robot.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Random
import androidx.core.graphics.toColorInt

class SoundWaveView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val wavePaint = Paint().apply {
        color = "#D9D9D9".toColorInt()
        strokeWidth = 12f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var amplitudes = FloatArray(64) { 0f }
    private val random = Random()

    fun updateAmplitude(newAmplitude: Float) {
        // 移动所有振幅值
        System.arraycopy(amplitudes, 1, amplitudes, 0, amplitudes.size - 1)
        amplitudes[amplitudes.size - 1] = newAmplitude
        invalidate()
    }

    fun reset(){
        amplitudes = FloatArray(64) { 0f }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        val widthPerSample = width.toFloat() / amplitudes.size

        // 绘制音波线
        for (i in 1 until amplitudes.size) {
            val prevX = (i - 1) * widthPerSample
            val prevY = centerY - amplitudes[i - 1] * centerY
            val x = i * widthPerSample
            val y = centerY - amplitudes[i] * centerY
            canvas.drawLine(prevX, prevY, x, y, wavePaint)
        }
    }
}