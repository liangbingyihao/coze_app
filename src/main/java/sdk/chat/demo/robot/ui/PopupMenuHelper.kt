package sdk.chat.demo.robot.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.extensions.dpToPx

class PopupMenuHelper(
    private val context: Context,
    private val anchorView: View,
    private val onItemSelected: (v: View) -> Unit,
    private val menuResId: Int = 0,
    private val clickableResIds: IntArray = intArrayOf()
) {

    private lateinit var popupWindow: PopupWindow
    private var isShowing = false

    fun dismiss() {
        popupWindow.dismiss()
    }

    fun setPopupWindow(pos: Int, resId: Int) {
        //pos:0 在anchorView下方，1 在anchorView上方
        val popupView = LayoutInflater.from(context).inflate(resId, null)

//        val viewStub = popupView.findViewById<ViewStub>(R.id.menu_stub)
//        viewStub.layoutResource = resId // 或其他布局
//        viewStub.inflate()
        var textViews: MutableList<TextView> = mutableListOf()
        clickableResIds.forEach { resId ->
            var tv = popupView.findViewById<TextView>(resId)
            textViews.add(tv)
            tv.setOnClickListener { view ->
                onItemSelected(view)
                popupWindow.dismiss()
            }
        }

        adjustMenuWidth(popupView.findViewById<CardView>(R.id.menu_container), textViews)


        if (pos == 0) {
            popupView.findViewById<View>(R.id.arrow_bottom)?.visibility = View.GONE
        } else {
            popupView.findViewById<View>(R.id.arrow_top)?.visibility = View.GONE
        }

        // 3. 创建PopupWindow
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 20f
            isOutsideTouchable = true
            animationStyle = -1
            setOnDismissListener { this@PopupMenuHelper.isShowing = false }
        }

    }

    fun adjustMenuWidth2(cardView: CardView?, textViews: List<TextView>) {

        textViews.forEach { tv ->
            tv.apply {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                isSingleLine = true
                ellipsize = null
            }
        }

        cardView?.post {
            // 计算最大宽度
            val maxWidth = textViews.maxOf { tv ->
                // 图标宽度（右侧图标）
                val drawableWidth = tv.compoundDrawablesRelative[2]?.intrinsicWidth ?: 0
                tv.paint.measureText(tv.text.toString()) +
                        tv.paddingStart + tv.paddingEnd +
                        drawableWidth +
                        (if (drawableWidth > 0) tv.compoundDrawablePadding else 0)
            }.toInt() + cardView.paddingLeft + cardView.paddingRight

            // 设置宽度（不低于160dp）
            cardView.layoutParams.width = maxWidth.coerceAtLeast(160.dpToPx(MainApp.getContext()))
            cardView.requestLayout()
        }

    }

    fun adjustMenuWidth(cardView: CardView?, textViews: List<TextView>) {
        if (cardView == null) return

        // 1. 先设置TextView单行显示
        textViews.forEach { tv ->
            tv.apply {
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT  // 改为 WRAP_CONTENT
                isSingleLine = true
                ellipsize = null
            }
        }

        // 2. 强制立即测量TextView的宽度（同步）
        cardView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 3. 计算最大宽度（同步）
        val maxWidth = textViews.maxOf { tv ->
            val drawableWidth = tv.compoundDrawablesRelative[2]?.intrinsicWidth ?: 0
            tv.paint.measureText(tv.text.toString()) +
                    tv.paddingStart + tv.paddingEnd +
                    drawableWidth +
                    (if (drawableWidth > 0) tv.compoundDrawablePadding else 0)
        }.toInt() + cardView.paddingLeft + cardView.paddingRight

        // 4. 直接设置最终宽度
        cardView.layoutParams.width = maxWidth.coerceAtLeast(160.dpToPx(MainApp.getContext()))
    }

    fun show() {
        if (isShowing) return
        // 1. 创建内容视图
        // 确保视图已测量
//        if (menuResId > 0) {
//            setPopupWindow(menuResId)
//            popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END)
//            isShowing = true
//            return
//        }
        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val screenHeight = context.resources.displayMetrics.heightPixels

        if (anchorLocation[1] + anchorView.height + 150 > screenHeight) {
            setPopupWindow(1, menuResId)
            anchorView.post {
                popupWindow.showAsDropDown(
                    anchorView,
                    0,
                    -anchorView.height - 150,
                    Gravity.END
                )
            }

        } else {
            // 显示在下方
            setPopupWindow(0, menuResId)
            anchorView.post { popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END) }

        }

        isShowing = true

    }

    private fun calculatePopupPosition(popupHeight: Int): Pair<Int, Int> {
        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val screenHeight = context.resources.displayMetrics.heightPixels

        return if (anchorLocation[1] + anchorView.height + popupHeight > screenHeight) {
            // 显示在上方
            anchorLocation[0] to (anchorLocation[1] - popupHeight)
        } else {
            // 显示在下方
            anchorLocation[0] to (anchorLocation[1] + anchorView.height)
        }
    }

}