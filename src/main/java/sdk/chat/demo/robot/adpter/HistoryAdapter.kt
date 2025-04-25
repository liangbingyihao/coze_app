package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R

sealed class HistoryItem  {
    data class DateItem(val date: String) : HistoryItem()
    data class SessionItem(val title: String, val sessionId: String) : HistoryItem()
}


class HistoryAdapter(private val items: MutableList<HistoryItem> = mutableListOf(),
                     private val onItemClick: (Boolean,HistoryItem.SessionItem) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var selectedPosition = if(items.isNotEmpty()) 1 else -1

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_ARTICLE = 1
    }


    fun updateAll(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        selectedPosition = if(items.isNotEmpty()) 1 else -1
        notifyDataSetChanged() // 触发全局刷新
    }

    fun getSelectItem(): HistoryItem.SessionItem? {
        return if (selectedPosition in items.indices) {
            when (val item = items[selectedPosition]) {
                is HistoryItem.SessionItem -> item
                else -> null // 如果选中的是DateItem则返回null
            }
        } else {
            null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.DateItem -> TYPE_DATE
            is HistoryItem.SessionItem -> TYPE_ARTICLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> DateViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_date, parent, false)
            )
            else -> ArticleViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_session, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.DateItem -> (holder as DateViewHolder).bind(item)
            is HistoryItem.SessionItem -> (holder as ArticleViewHolder).bind(item,position)
        }
    }

    override fun getItemCount() = items.size

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.date_text)

        fun bind(item: HistoryItem.DateItem) {
            dateText.text = item.date
        }
    }

    inner class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleText: TextView = view.findViewById(R.id.title_text)

        fun bind(item: HistoryItem.SessionItem, position: Int) {
            titleText.text = item.title
            itemView.isSelected = position == selectedPosition
            itemView.setOnClickListener {
                val clickedPosition = absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener
                // 更新选中状态
                val previous = selectedPosition
                selectedPosition = clickedPosition

                // 只刷新必要的项目
                listOfNotNull(previous.takeIf { it != -1 }, clickedPosition)
                    .distinct()
                    .forEach { notifyItemChanged(it) }

                onItemClick(previous!=selectedPosition,item)
            }
        }
    }
}


