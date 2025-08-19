package sdk.chat.demo.robot.ui
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout

class InputLayoutBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<LinearLayout>(context, attrs) {

    private var maxHeight = 0
    private var keyboardVisible = false

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: LinearLayout,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        if (maxHeight == 0) {
            // 计算最大允许高度（屏幕高度的80%）
            val metrics = Resources.getSystem().displayMetrics
            maxHeight = (metrics.heightPixels * 0.8).toInt()
        }

        // 获取当前可用高度
        val availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec) - heightUsed

        // 限制输入区域高度
        val heightSpec = if (availableHeight > maxHeight) {
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        } else {
            View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST)
        }

        // 测量子视图
        parent.onMeasureChild(
            child,
            parentWidthMeasureSpec,
            widthUsed,
            heightSpec,
            heightUsed
        )
        return true
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: LinearLayout,
        dependency: View
    ): Boolean {
        // 处理与聊天列表的交互
        return false
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: LinearLayout,
        layoutDirection: Int
    ): Boolean {
        // 确保始终位于底部
        parent.onLayoutChild(child, layoutDirection)
        child.translationY = 0f
        return true
    }
}