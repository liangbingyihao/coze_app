package sdk.chat.demo.robot.ui
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import sdk.chat.demo.pre.R

class VerticalDashedLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // 默认值
    private var lineColor = Color.BLACK
    private var lineWidth = 2f
    private var dashLength = 4f
    private var gapLength = 4f
    private var linePosition = "center" // center, left, right

    init {
        initAttributes(attrs)
        setupPaint()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray: TypedArray? = context.obtainStyledAttributes(
            attrs,
            R.styleable.VerticalDashedLineView
        )

        typedArray?.let {
            lineColor = it.getColor(
                R.styleable.VerticalDashedLineView_lineColor,
                Color.BLACK
            )
            lineWidth = it.getDimension(
                R.styleable.VerticalDashedLineView_lineWidth,
                2f
            )
            dashLength = it.getDimension(
                R.styleable.VerticalDashedLineView_dashLength,
                4f
            )
            gapLength = it.getDimension(
                R.styleable.VerticalDashedLineView_gapLength,
                4f
            )
            linePosition = "center"

            it.recycle()
        }
    }

    private fun setupPaint() {
        paint.color = lineColor
        paint.strokeWidth = lineWidth
        paint.pathEffect = DashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val x = when (linePosition) {
            "left" -> paddingStart.toFloat() + lineWidth / 2
            "right" -> width - paddingEnd.toFloat() - lineWidth / 2
            else -> width / 2f // center
        }

        val startY = paddingTop.toFloat()
        val endY = height - paddingBottom.toFloat()

        canvas.drawLine(x, startY, x, endY, paint)
    }

    // 公共方法用于动态修改属性
    fun setLineColor(color: Int) {
        lineColor = color
        paint.color = color
        invalidate()
    }

    fun setLineWidth(width: Float) {
        lineWidth = width
        paint.strokeWidth = width
        invalidate()
    }

    fun setDashPattern(dash: Float, gap: Float) {
        dashLength = dash
        gapLength = gap
        paint.pathEffect = DashPathEffect(floatArrayOf(dash, gap), 0f)
        invalidate()
    }

    fun setLinePosition(position: String) {
        linePosition = position
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 宽度只需要足够显示虚线即可，高度跟随父布局
        val desiredWidth = (lineWidth + paddingStart + paddingEnd).toInt()
        val desiredHeight = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        setMeasuredDimension(width, desiredHeight)
    }
}