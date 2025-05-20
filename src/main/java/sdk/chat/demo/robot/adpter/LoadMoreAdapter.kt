package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R;

abstract class LoadMoreAdapter<T : Any>(
    private val layoutId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
    }

    protected val items = mutableListOf<T>()
    private var isLoading = false

    // 设置新数据（完全替换）
    fun setItems(newItems: List<T>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // 添加更多数据（分页加载）
    fun addItems(newItems: List<T>) {
        val positionStart = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(positionStart, newItems.size)
    }

    // 显示加载更多指示器
    fun showLoading() {
        isLoading = true
        notifyItemInserted(items.size)
    }

    // 隐藏加载更多指示器
    fun hideLoading() {
        isLoading = false
        notifyItemRemoved(items.size)
    }

    override fun getItemCount(): Int = items.size + if (isLoading) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && isLoading) TYPE_LOADING else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LOADING) {
            LoadingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
            )
        } else {
            createItemViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LoadMoreAdapter<*>.ItemViewHolder -> bind(holder, items[position])
            is LoadingViewHolder -> holder.bind()
        }
    }

    // 抽象方法 - 创建ItemViewHolder
    protected abstract fun createItemViewHolder(parent: ViewGroup): ItemViewHolder

    // 抽象方法 - 绑定数据
    protected abstract fun bind(holder: LoadMoreAdapter<*>.ItemViewHolder, item: T)

    // ItemViewHolder基类
    abstract inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // LoadingViewHolder
    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // 加载更多视图绑定逻辑（如有需要）
        }
    }
}