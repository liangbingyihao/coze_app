package sdk.chat.demo.robot.adpter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.FavoriteList

class FavoriteAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData: MutableList<FavoriteList.FavoriteItem> =
        ArrayList<FavoriteList.FavoriteItem>()

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
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
        return if (position == listData.size) TYPE_FOOTER else TYPE_ITEM
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
                .inflate(R.layout.item_collection, parent, false)
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
                    Log.w("getItemViewType", "MyViewHolder,${position}")
                    if (position < listData.size) { // 关键修复点
                        val item = listData[position]
                        holder.textView.text = "${position},${item.content}"
                    } else {
                        holder.textView.text = "" // 处理异常情况
                    }
                }

                is FootViewHolder -> {
                    Log.w("getItemViewType", "FootViewHolder,${position}")
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
    }
}