package sdk.chat.demo.robot.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CustomDivider (
    private val thickness: Int,
    private val colorResId: Int,
    private val insetStart: Int = 0,
    private val insetEnd: Int = 0
) : RecyclerView.ItemDecoration() {

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val paint = Paint().apply {
            color = ContextCompat.getColor(parent.context, colorResId)
            style = Paint.Style.FILL
        }

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val left = parent.paddingLeft + insetStart
            val right = parent.width - parent.paddingRight - insetEnd
            val top = child.bottom + params.bottomMargin
            val bottom = top + thickness

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, thickness)
    }
}