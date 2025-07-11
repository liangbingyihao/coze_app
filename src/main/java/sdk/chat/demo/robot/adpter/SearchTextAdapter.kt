package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.data.SearchText
import sdk.chat.demo.robot.extensions.dpToPx

class SearchTextAdapter(
    private var items: List<SearchText> = emptyList(),
    private val onItemClick: (SearchText.Tag) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TAG = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SearchText.Header -> TYPE_HEADER
            is SearchText.Tag -> TYPE_TAG
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val delSearch: View = view.findViewById(R.id.del)
        fun bind(header: SearchText.Header) {
            delSearch.setOnClickListener {
                Toast.makeText(delSearch.context, "Clicked: delSearch", Toast.LENGTH_SHORT).show()
//                toggleSelection(adapterPosition) // 更新选中状态
            }
        }
    }

    // ViewHolder 类
    inner class FlexViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_item)

        fun bind(item: SearchText.Tag) {
            textView.text = item.text
//            textView.setBackgroundColor(item.color)
//            textView.isSelected = item.isSelected

            // 点击事件
            itemView.setOnClickListener {
                onItemClick(item)
//                toggleSelection(adapterPosition) // 更新选中状态
            }
        }
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_header, parent, false)
                // 关键：Header使用全宽度布局
                view.layoutParams = FlexboxLayoutManager.LayoutParams(
                    FlexboxLayout.LayoutParams.MATCH_PARENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                )
                HeaderViewHolder(view)
            }

            TYPE_TAG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_text, parent, false)

                // 关键：设置 Flexbox 布局参数
                view.layoutParams = FlexboxLayoutManager.LayoutParams(
                    FlexboxLayoutManager.LayoutParams.WRAP_CONTENT,
                    FlexboxLayoutManager.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        8.dpToPx(parent.context),
                        4.dpToPx(parent.context),
                        8.dpToPx(parent.context),
                        4.dpToPx(parent.context)
                    )
//            flexGrow = 1f // 允许扩展填充空间
                }

                return FlexViewHolder(view)
            }

            else -> throw IllegalArgumentException()
        }


    }

    // 绑定数据
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as SearchText.Header)
            is FlexViewHolder -> holder.bind(items[position] as SearchText.Tag)
        }
    }

    override fun getItemCount() = items.size

    // 更新数据（使用 DiffUtil 优化）
    fun submitList(newItems: List<SearchText>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val oldItem = items[oldPos]
                val newItem = newItems[newPos]
                return when {
                    oldItem is SearchText.Header && newItem is SearchText.Header ->
                        true

                    oldItem is SearchText.Tag && newItem is SearchText.Tag ->
                        oldItem.id == newItem.id

                    else -> false
                }
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos] == newItems[newPos]
            }
        })
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

//    // 选中状态切换
//    private fun toggleSelection(position: Int) {
//        items = items.mapIndexed { index, item ->
//            if (index == position) item.copy(isSelected = !item.isSelected) else item
//        }
//        notifyItemChanged(position)
//    }

//    // dp 转 px 工具函数
//    private fun Int.dpToPx(context: Context): Int {
//        return TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            this.toFloat(),
//            context.resources.displayMetrics
//        ).toInt()
//    }
}