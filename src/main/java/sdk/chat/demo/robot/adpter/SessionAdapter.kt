package sdk.chat.demo.robot.adpter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.handlers.GWThreadHandler.headTopic

sealed class HistoryItem {
    data class DateItem(val date: String) : HistoryItem()
    data class SessionItem(var title: String, val sessionId: String) : HistoryItem()
}


class SessionAdapter(
    private val items: MutableList<HistoryItem> = mutableListOf(),
    private val onItemClick: (Boolean, HistoryItem.SessionItem) -> Unit,
    private val onItemEdit: (HistoryItem.SessionItem) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //    private var selectedPosition = if(items.isNotEmpty()) 1 else -1
    private var selectedPosition = -1

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_ARTICLE = 1
        private const val TYPE_HEADER = 2
        private var cornerSize = 10f.dpToPx(MainApp.getContext())
    }


    fun updateAll(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        selectedPosition = if (items.isNotEmpty()) 1 else -1
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

    fun setSelectItemName(name: String) {
        if (selectedPosition in items.indices) {
            val item: HistoryItem.SessionItem = items[selectedPosition] as HistoryItem.SessionItem
            item.title = name
            notifyItemChanged(selectedPosition)
        }
    }

    override fun getItemViewType(position: Int): Int {
        var item = items[position]
        if (item is HistoryItem.DateItem) {
            return TYPE_DATE
        }
        if (item is HistoryItem.SessionItem) {
            if (position == 0 && (item.title == headTopic)) {
                return TYPE_HEADER
            }
        }
        return TYPE_ARTICLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> DateViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_date, parent, false)
            )

            TYPE_HEADER -> ArticleViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_head_session, parent, false)
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
            is HistoryItem.SessionItem -> (holder as ArticleViewHolder).bind(item, position)
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
//        private val editTopic: View? = view.findViewById(R.id.edit_topic)

        fun bind(item: HistoryItem.SessionItem, position: Int) {
            titleText.text = item.title
            if (position == 0) {
                titleText.background = GradientDrawable().apply {
                    // 设置四角半径（顺序：左上,右上,右下,左下）
                    cornerRadii = floatArrayOf(
                        cornerSize, cornerSize,
                        cornerSize, cornerSize,
                        cornerSize, cornerSize,
                        cornerSize, cornerSize,
                    )
                    setColor(Color.WHITE)
                }
            } else if (position == 1 && (items[0] as HistoryItem.SessionItem).title == headTopic) {
                if (items.size > 2) {
                    itemView.background = GradientDrawable().apply {
                        // 设置四角半径（顺序：左上,右上,右下,左下）
                        cornerRadii = floatArrayOf(
                            cornerSize, cornerSize,
                            cornerSize, cornerSize,
                            0f, 0f,
                            0f, 0f,
                        )
                        setColor(Color.WHITE)
                    }
                } else {
                    itemView.background = GradientDrawable().apply {
                        // 设置四角半径（顺序：左上,右上,右下,左下）
                        cornerRadii = floatArrayOf(
                            cornerSize, cornerSize,
                            cornerSize, cornerSize,
                            cornerSize, cornerSize,
                            cornerSize, cornerSize,
                        )
                        setColor(Color.WHITE)
                    }
                }
            } else if (position == items.size - 1) {
                val bg = GradientDrawable().apply {
                    // 设置四角半径（顺序：左上,右上,右下,左下）
                    cornerRadii = floatArrayOf(
                        0f, 0f,
                        0f, 0f,
                        cornerSize, cornerSize,
                        cornerSize, cornerSize,
                    )
                    setColor(Color.WHITE)
                }
                itemView.background = bg
            } else {
                itemView.setBackgroundColor(Color.WHITE)
            }
//            itemView.isSelected = position == selectedPosition
            itemView.setOnClickListener {
                val clickedPosition =
                    absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?: return@setOnClickListener
                // 更新选中状态
                val previous = selectedPosition
                selectedPosition = clickedPosition

                // 只刷新必要的项目
                listOfNotNull(previous.takeIf { it != -1 }, clickedPosition)
                    .distinct()
                    .forEach { notifyItemChanged(it) }

                onItemClick(previous != selectedPosition, item)
            }

//            editTopic?.setOnClickListener {
//                val clickedPosition =
//                    absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
//                        ?: return@setOnClickListener
//                // 更新选中状态
//                selectedPosition = clickedPosition
//
//                onItemEdit(item)
//            }
        }
    }
}


