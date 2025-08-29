package sdk.chat.demo.robot.ui

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Interpolator
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import kotlin.math.min
import sdk.chat.demo.pre.R


class CircleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 圆环画笔
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 10f
    }

    // 实心圆饼画笔
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }

    private var ringRadius = 0f
    private var circleRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000 // 动画时长1.5秒
        interpolator = AccelerateDecelerateInterpolator()
        repeatCount = ValueAnimator.INFINITE // 无限循环
        repeatMode = ValueAnimator.RESTART // 循环模式：重新开始

        addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            ringRadius = progress * maxRingRadius
            // 圆饼半径比圆环半径小，形成套着的效果
//            circleRadius = ringRadius - ringPaint.strokeWidth / 2
            invalidate()
        }

        // 添加动画监听器，用于更好的控制
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {
                // 每次循环开始时可以添加一些额外效果
            }
        })
    }

    private var maxRingRadius = 0f
    private var isAnimating = false

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CircleOverlayView, 0, 0).apply {
            try {
                ringPaint.color = getColor(R.styleable.CircleOverlayView_ringColor, Color.RED)
                ringPaint.strokeWidth = getDimension(R.styleable.CircleOverlayView_ringWidth, 10f)
                circlePaint.color = getColor(R.styleable.CircleOverlayView_circleColor, Color.BLUE)
                circleRadius = maxRingRadius - ringPaint.strokeWidth / 2
            } finally {
                recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        maxRingRadius = min(w, h) / 2f - ringPaint.strokeWidth
        circleRadius = maxRingRadius - ringPaint.strokeWidth / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 先绘制实心圆饼
        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)

        // 2. 再绘制圆环
        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
    }

    fun startAnimation() {
        if (!isAnimating) {
            animator.start()
            isAnimating = true
        }
    }

    fun stopAnimation() {
        if (isAnimating) {
            animator.cancel()
            isAnimating = false
        }
    }

    fun pauseAnimation() {
        if (isAnimating) {
            animator.pause()
            isAnimating = false
        }
    }

    fun resumeAnimation() {
        if (animator.isPaused) {
            animator.resume()
            isAnimating = true
        }
    }

    fun setAnimationDuration(duration: Long) {
        animator.duration = duration
    }

    fun setRingColor(@ColorInt color: Int) {
        ringPaint.color = color
        invalidate()
    }

    fun setRingWidth(width: Float) {
        ringPaint.strokeWidth = width
        maxRingRadius = min(width.toFloat(), height.toFloat()) / 2f - ringPaint.strokeWidth
        invalidate()
    }

    fun setCircleColor(@ColorInt color: Int) {
        circlePaint.color = color
        invalidate()
    }

    fun isAnimating(): Boolean = isAnimating

    fun reset() {
        stopAnimation()
        ringRadius = 0f
        circleRadius = 0f
        invalidate()
    }

    // 设置不同的插值器来改变动画效果
    fun setInterpolator(interpolator: Interpolator) {
        animator.interpolator = interpolator as TimeInterpolator?
    }

    // 设置循环次数
    fun setRepeatCount(count: Int) {
        animator.repeatCount = count
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 避免内存泄漏，在View detached时停止动画
        stopAnimation()
    }
}