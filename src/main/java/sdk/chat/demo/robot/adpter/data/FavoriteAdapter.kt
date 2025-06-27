package sdk.chat.demo.robot.adpter.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.lassi.presentation.media.adapter.MediaAdapter
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.OnLoadMoreListener
import sdk.chat.demo.robot.api.model.FavoriteList.FavoriteItem


class FavoriteAdapter(
    private val mOnLoadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData: MutableList<FavoriteItem> = ArrayList<FavoriteItem>()

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == listData.size) {
//            if (position == listData.size && mOnLoadMoreListener.isAllScreen) {
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
                .inflate(R.layout.item_collection, parent, false)
            return MyViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (getItemViewType(position) != TYPE_FOOTER) {
            if (position < listData.size) {
                val viewHolder = holder as MyViewHolder
                var data: FavoriteItem = listData[position]
                viewHolder.textView.setText(data.content)
            }else{
                var a = 1;
            }
        } else {
            var a = 1;
        }
//        if (getItemViewType(position) === TYPE_FOOTER) {
//        } else {
//            val viewHolder = holder as MyViewHolder
//            viewHolder.textView.setText("第" + position + "行")
//        }
    }

    override fun getItemCount(): Int {
        return listData.size + 1;
    }

    fun addItem(d: FavoriteItem) {
        listData.add(d)
    }

    fun clear() {
        listData.clear()
    }

    fun setItems(data: ArrayList<FavoriteItem>) {
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    private class FootViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contentLoadingProgressBar: ContentLoadingProgressBar? = null
    }

    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById<TextView?>(R.id.tvContent)
    }
}