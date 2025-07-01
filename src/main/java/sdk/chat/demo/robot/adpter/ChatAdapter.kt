package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.stfalcon.chatkit.commons.models.IMessage
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.holder.ChatImageViewHolder
import sdk.chat.demo.robot.holder.ChatTextViewHolder
import sdk.chat.demo.robot.holder.ImageHolder
import sdk.chat.demo.robot.holder.TextHolder
import sdk.chat.demo.robot.holder.TimeHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageDiffCallback(
    private val oldList: List<IMessage>,
    private val newList: List<IMessage>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        val oldItem = oldList[oldPos]
        val newItem = newList[newPos]

        return when {
            // 相同类型的消息才比较ID
            oldItem is TextHolder && newItem is TextHolder ->
                oldItem.id == newItem.id
            oldItem is ImageHolder && newItem is ImageHolder ->
                oldItem.id == newItem.id
            oldItem is TimeHolder && newItem is TimeHolder ->
                oldItem.createdAt == newItem.createdAt // 时间分隔条用时间戳对比
            else -> false
        }
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos] // 使用data class的equals方法
    }

//    @Nullable
//    override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
//        // 可选：实现精细化的局部更新
//        val oldItem = oldList[oldPos]
//        val newItem = newList[newPos]
//
//        return when {
//            oldItem is MessageItem.TextMessage && newItem is MessageItem.TextMessage -> {
//                if (oldItem.content != newItem.content) {
//                    mapOf("content" to newItem.content) // 只更新文本内容
//                } else null
//            }
//            else -> null
//        }
//    }
}

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TEXT = 1
        private const val TYPE_IMAGE = 2
        private const val TYPE_TIME = 3
        private const val TYPE_FOOTER = 4
    }

    private val items = mutableListOf<IMessage>()
    private var header: Any? = null
    private var footer: Any? = null

    fun submitList(newList: List<IMessage>) {
        val diffResult = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    // 添加新消息（自动插入到头部）
    fun addNewMessage(item: IMessage) {
        val newList = items.toMutableList().apply { add(0, item) }
        submitList(newList)
    }

    // 批量添加历史消息（追加到尾部）
    fun addHistoryMessages(newItems: List<IMessage>) {
        val newList = items.toMutableList().apply { addAll(newItems) }
        submitList(newList)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isHeaderPosition(position) -> TYPE_HEADER
            isFooterPosition(position) -> TYPE_FOOTER
            else -> when (getItem(position)) {
                is TextHolder -> TYPE_TEXT
                is ImageHolder -> TYPE_IMAGE
                is TimeHolder -> TYPE_TIME
                else -> throw IllegalStateException("Unknown message type")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflateView(R.layout.item_list_footer, parent))
            TYPE_TEXT -> ChatTextViewHolder<TextHolder>(inflateView(R.layout.item_feed_text, parent))
            TYPE_IMAGE -> ChatImageViewHolder<ImageHolder>(inflateView(R.layout.item_feed_bible_pic, parent))
            TYPE_TIME -> TimeViewHolder(inflateView( R.layout.item_date_header, parent))
            TYPE_FOOTER -> FooterViewHolder(inflateView(R.layout.item_list_footer, parent))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

//    // 修改onBindViewHolder以支持局部更新
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
//        if (payloads.isNotEmpty()) {
//            when (holder) {
//                is TextViewHolder -> {
//                    val content = payloads.find { it is Map<*,*> }
//                        ?.let { (it as Map<*, *>)["content"] as? String }
//                    content?.let { holder.updateContent(it) }
//                }
//                else -> super.onBindViewHolder(holder, position, payloads)
//            }
//        } else {
//            super.onBindViewHolder(holder, position, payloads)
//        }
//    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChatTextViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as ChatTextViewHolder<TextHolder>).bind(item as TextHolder)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }
            is ChatImageViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as ChatImageViewHolder<ImageHolder>).bind(item as ImageHolder)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }
            is TimeViewHolder -> holder.bind(getItem(position) as TimeHolder)
            is HeaderViewHolder -> holder.bind(header)
            is FooterViewHolder -> holder.bind(footer)
        }
    }

    override fun getItemCount(): Int = items.size + headerOffset() + footerOffset()

    /* 内部工具方法 */
    private fun inflateView(layoutId: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    private fun getItem(position: Int): IMessage {
        return items[position - headerOffset()]
    }

    private fun isHeaderPosition(pos: Int) = hasHeader() && pos == 0
    private fun isFooterPosition(pos: Int) = hasFooter() && pos == itemCount - 1
    private fun hasHeader() = header != null
    private fun hasFooter() = footer != null
    private fun headerOffset() = if (hasHeader()) 1 else 0
    private fun footerOffset() = if (hasFooter()) 1 else 0
    private fun getInsertPosition() = if (hasHeader()) 1 else 0


    inner class TimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(divider: TimeHolder) {
            itemView.findViewById<TextView>(R.id.messageText).text =
                SimpleDateFormat("MM月dd日", Locale.CHINA)
                    .format(divider.createdAt)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Any?) {
            // 根据header类型处理
        }
    }

    inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Any?) {
            // 根据footer类型处理
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }
}