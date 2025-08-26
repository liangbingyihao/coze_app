package sdk.chat.demo.robot.adpter

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.size
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.stfalcon.chatkit.commons.models.IMessage
import io.reactivex.Single
import org.pmw.tinylog.Logger
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.holder.ChatImageViewHolder
import sdk.chat.demo.robot.holder.ChatTextViewHolder
import sdk.chat.demo.robot.holder.ExploreViewHolder
import sdk.chat.demo.robot.holder.ImageHolder
import sdk.chat.demo.robot.holder.TextHolder
import sdk.chat.demo.robot.holder.TimeHolder
import sdk.guru.common.RX
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

    //    private var header: Any? = null
    private var footer: Any? = null
    private val viewClickListenersArray =
        SparseArray<OnMessageViewClickListener>()

    interface OnMessageViewClickListener {
        /**
         * Fires when message view is clicked.
         *
         * @param message clicked message.
         */
        fun onMessageViewClick(view: View?, message: IMessage?)
    }

    var header = false
        set(value) {
            if (field != value) {
                field = value
//                Handler(Looper.getMainLooper()).postDelayed({ notifyItemChanged(itemCount-1); },2)
            }
        }

    fun registerViewClickListener(
        viewId: Int,
        onMessageViewClickListener: OnMessageViewClickListener
    ) {
        viewClickListenersArray.append(viewId, onMessageViewClickListener)
    }

    fun bindListeners(holder: RecyclerView.ViewHolder, item: IMessage?) {
        for (i in 0..<viewClickListenersArray.size) {
            val key: Int = viewClickListenersArray.keyAt(i)
            val view: View? = holder.itemView.findViewById<View?>(key)
            view?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    viewClickListenersArray.get(key).onMessageViewClick(view, item)
                }
            })
        }
    }

    @SuppressLint("CheckResult")
    fun submitList(newList: List<IMessage>, onComplete: (() -> Unit)? = null) {
//        val diffResult = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
//        items.clear()
//        items.addAll(newList)
//        diffResult.dispatchUpdatesTo(this)
//        onComplete?.invoke()
        // 切换到后台线程计算Diff
        Single.fromCallable {
            DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        }.subscribeOn(RX.computation())
            .observeOn(RX.main())
            .subscribe({ diffResult ->
                items.clear()
                items.addAll(newList)
                diffResult.dispatchUpdatesTo(this@ChatAdapter)
//                notifyDataSetChanged()
                onComplete?.invoke()
            }, { error ->
                Log.e("ChatAdapter", "DiffUtil计算失败", error)
                // 降级方案：普通全量刷新
                items.clear()
                items.addAll(newList)
                notifyDataSetChanged()
                onComplete?.invoke()
            })
    }

    // 添加新消息（自动插入到头部）
    fun addNewMessage(item: IMessage, onComplete: (() -> Unit)? = null) {
        val newList = items.toMutableList().apply { add(0, item) }
        submitList(newList, onComplete)
    }

    /**
     * 添加多条新消息到列表头部（支持批量操作和DiffUtil优化）
     * @param newMessages 要添加的消息集合
     * @param onComplete 操作完成回调（可选，在主线程执行）
     */
    fun addNewMessage(newMessages: List<IMessage>, onComplete: (() -> Unit)? = null) {
        if (newMessages.isEmpty()) {
            onComplete?.invoke()
            return
        }

        val newList = ArrayList<IMessage>(items.size + newMessages.size).apply {
            addAll(newMessages) // 先添加新消息
            addAll(items)       // 追加旧数据
        }

        submitList(newList, onComplete)

    }

    // 批量添加历史消息（追加到尾部）
    fun addHistoryMessages(newItems: List<IMessage>, onComplete: (() -> Unit)? = null) {
        val newList = items.toMutableList().apply { addAll(newItems) }
        submitList(newList, onComplete)
    }


    fun delMessage(message: IMessage, onComplete: (() -> Unit)? = null) {
        // Create new list with items removed
        val deletePos = items.indexOfFirst { it.id == message.id }

        if (deletePos == -1) {
            Log.e("delmsg", "message.id:${message.id},del:-1,size:${items.size}")
            onComplete?.invoke()
            return
        }
        Log.e("delmsg", "message.id:${message.id},del:${deletePos}:${items[deletePos].id},size:${items.size}")
        val newList = ArrayList(items).apply { removeAt(deletePos + headerOffset()) }
        //status: 0: pending,1:idle
        submitList(newList, onComplete)
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
        if (TYPE_HEADER == viewType) {
            Logger.warn("onLoadCreateView:" + viewType)
        }
        return when (viewType) {
            TYPE_HEADER -> ExploreViewHolder(inflateView(R.layout.item_feed_header, parent))
            TYPE_TEXT -> ChatTextViewHolder<TextHolder>(
                inflateView(
                    R.layout.item_feed_text,
                    parent
                )
            )

            TYPE_IMAGE -> ChatImageViewHolder<ImageHolder>(
                inflateView(
                    R.layout.item_feed_daily_gw,
                    parent
                )
            )

            TYPE_TIME -> TimeViewHolder(inflateView(R.layout.item_date_header, parent))
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
                    (holder as ChatTextViewHolder<TextHolder>).onBind(item as TextHolder)
                    bindListeners(holder, item)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }

            is ChatImageViewHolder<*> -> {
                val item = getItem(position)
                try {
                    @Suppress("UNCHECKED_CAST")
                    (holder as ChatImageViewHolder<ImageHolder>).onBind(item as ImageHolder)
                    bindListeners(holder, item)
                } catch (e: ClassCastException) {
//                    holder.onError(e)
                }
            }

            is TimeViewHolder -> holder.bind(getItem(position) as TimeHolder)
            is ExploreViewHolder -> {
                holder.bind(header)
//                bindListeners(holder, null)
            }

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
    private fun hasHeader() = false
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

//    inner class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        var contentLoadingProgressBar: ContentLoadingProgressBar =
//            itemView.findViewById<ContentLoadingProgressBar?>(
//                R.id.pb_progress
//            )
//        val exploreView: Map<String, TextView> = mapOf(
//            "explore0" to itemView.findViewById<TextView>(R.id.explore1),
//            "explore1" to itemView.findViewById<TextView>(R.id.explore2),
//            "explore2" to itemView.findViewById<TextView>(R.id.explore3)
//        )
//
//        fun bind(header: Boolean) {
//            // 根据header类型处理
//            contentLoadingProgressBar.visibility = if (header) View.VISIBLE else View.INVISIBLE
//            bindListeners(this, null)
//        }
//    }

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