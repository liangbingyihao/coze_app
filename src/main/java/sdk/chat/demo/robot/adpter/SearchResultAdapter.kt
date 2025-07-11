package sdk.chat.demo.robot.adpter

import android.content.Context
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.FavoriteList

class SearchResultAdapter(
    private val onItemClick: (FavoriteList.FavoriteItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData: MutableList<FavoriteList.FavoriteItem> =
        ArrayList<FavoriteList.FavoriteItem>()

    companion object {
        private const val TYPE_ITEM = 1
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
        if (position == listData.size) {
            return TYPE_FOOTER
        }
        return TYPE_ITEM
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == TYPE_FOOTER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_footer, parent, false)
            return FootViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
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
                        val item = listData[position]
                        holder.textView.text = Html.fromHtml(item.content, Html.FROM_HTML_MODE_LEGACY)
                        holder.tvTime.text = item.createdAt.replace("T", " ")
                        holder.itemView.setOnClickListener { onItemClick(item) }
                        holder.textView.setOnClickListener { onItemClick(item) }

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
    }
}