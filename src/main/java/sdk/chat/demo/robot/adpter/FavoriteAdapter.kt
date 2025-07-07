package sdk.chat.demo.robot.adpter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.FavoriteList
import androidx.core.graphics.toColorInt

class FavoriteAdapter(
    private val onItemClick: (FavoriteList.FavoriteItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData: MutableList<FavoriteList.FavoriteItem> =
        ArrayList<FavoriteList.FavoriteItem>()

    companion object {
        private const val TYPE_ITEM_USER = 0
        private const val TYPE_ITEM_AI = 1
        private const val TYPE_FOOTER = 2
        private val color_user = "#FFF8F7".toColorInt()
    }

    var isLoading = false
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(itemCount - 1)
            }
        }

    override fun getItemViewType(position: Int): Int {
//        if (position == listData.size) {
////            if (position == listData.size && mOnLoadMoreListener.isAllScreen) {
//            return TYPE_FOOTER
//        }
//        return TYPE_ITEM
        if(position == listData.size){
            return TYPE_FOOTER
        }
        var item = listData[position]
        if(item.contentType==GWApiManager.contentTypeAI){
            return TYPE_ITEM_AI
        }
        return TYPE_ITEM_USER
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == TYPE_FOOTER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_footer, parent, false)
            return FootViewHolder(view)
        } else if(viewType == TYPE_ITEM_AI) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_ai, parent, false)
            return MyViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_user, parent, false)
            return MyViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        try {
            when (holder) {
                is MyViewHolder -> {
                    if (position < listData.size) {
                        var context:Context = holder.textView.context
                        val item = listData[position]

                        Markwon.create(context)
                            .setMarkdown(holder.textView, item.content)
                        holder.tvTime.text = item.createdAt.replace("T", " ")
                        holder.itemView.setOnClickListener { onItemClick(item) }
                        holder.topic?.text = item.sessionName

                        holder.textView.post {
                            val lineCount = holder.textView.lineCount
                            holder.expand.visibility =
                                if (lineCount > 8) View.VISIBLE else View.GONE
                        }

                        // 测量实际行数（需post到布局完成后）
                        holder.textView.post {
                            val lineCount = holder.textView.lineCount
                            val shouldShowExpand = lineCount > 8

                            // 控制按钮可见性
                            holder.expand.visibility =
                                if (shouldShowExpand) View.VISIBLE else View.GONE

                            // 根据展开状态设置最大行数
                            if (item.isExpanded) {
                                holder.textView.maxLines = Int.MAX_VALUE
                                holder.expand.text = holder.expand.context.getString(R.string.fold)
                                holder.expand.setCompoundDrawablesWithIntrinsicBounds(
                                    null, // left (设为null表示不修改)
                                    null, // top
                                    ContextCompat.getDrawable(
                                        holder.expand.context,
                                        R.mipmap.ic_fold
                                    ), // right
                                    null  // bottom
                                )
                            } else {
                                holder.textView.maxLines = 8
                                holder.expand.text =
                                    holder.expand.context.getString(R.string.unfold)
                                holder.expand.setCompoundDrawablesWithIntrinsicBounds(
                                    null, // left (设为null表示不修改)
                                    null, // top
                                    ContextCompat.getDrawable(
                                        holder.expand.context,
                                        R.mipmap.ic_unfold
                                    ), // right
                                    null  // bottom
                                )
                            }
                        }

                        holder.expand.setOnClickListener {
                            item.isExpanded = !item.isExpanded
                            notifyItemChanged(position) // 只刷新当前item
//                        holder.textView.maxLines =
//                            if (holder.textView.maxLines == 8) Integer.MAX_VALUE else 8
//                        if (holder.textView.maxLines == 8) {
//                            holder.expand.text = holder.textView.context.getString(R.string.unfold)
//                        } else {
//                            holder.textView.maxLines = 8
//                            holder.expand.text = holder.textView.context.getString(R.string.fold)
//                        }
                        }

                    } else {
                        holder.textView.text = "" // 处理异常情况
                    }

                }

                is FootViewHolder -> {
                    // 始终保留Footer空间，仅控制进度条显隐
                    holder.contentLoadingProgressBar.visibility =
                        if (isLoading) View.VISIBLE else View.INVISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e("listFavorite1", "Binding error at pos $position", e)
        }
//        if (getItemViewType(position) === TYPE_FOOTER) {
//        } else {
//            val viewHolder = holder as MyViewHolder
//            viewHolder.textView.setText("第" + position + "行")
//        }
    }

    override fun getItemCount(): Int = listData.size + 1

    fun clear() {
        listData.clear()
        notifyDataSetChanged()
    }

    fun addItems(data: ArrayList<FavoriteList.FavoriteItem>) {
//        if (Looper.myLooper() != Looper.getMainLooper()) {
//            Handler(Looper.getMainLooper()).post { updateData(newData) }
//            return
//        }
        listData.addAll(data)
        notifyDataSetChanged()
//        notifyItemRangeInserted(startPos, data.size)
    }


    private class FootViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contentLoadingProgressBar: ContentLoadingProgressBar =
            itemView.findViewById<ContentLoadingProgressBar?>(
                R.id.pb_progress
            )
    }

    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById<TextView?>(R.id.tvContent)
        val tvTime: TextView = itemView.findViewById<TextView?>(R.id.tvTime)
        val expand: TextView = itemView.findViewById<TextView?>(R.id.expand)
        val topic: TextView? = itemView.findViewById<TextView?>(R.id.tvTopic)
    }
}