package sdk.chat.demo.robot.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class LoadMoreSwipeRefreshLayout(
    context: Context,
    attrs: AttributeSet
) : SwipeRefreshLayout(context, attrs) {

    private var loadMoreListener: OnLoadMoreListener? = null
    private var isLoadingMore = false

    interface OnLoadMoreListener {
        fun onLoadMore()
    }

    // 设置加载更多监听
    fun setOnLoadMoreListener(listener: OnLoadMoreListener) {
        this.loadMoreListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 如果正在加载更多，则拦截事件防止冲突
        return isLoadingMore || super.onInterceptTouchEvent(ev)
    }

    // 标记加载状态
    fun setLoadingMore(loading: Boolean) {
        isLoadingMore = loading
    }
}