package sdk.chat.demo.robot.adpter

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
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
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.holder.ChatImageViewHolder
import sdk.chat.demo.robot.holder.ChatTextViewHolder
import sdk.chat.demo.robot.holder.ExploreHolder
import sdk.chat.demo.robot.holder.ExploreViewHolder
import sdk.chat.demo.robot.holder.ImageHolder
import sdk.chat.demo.robot.holder.TextHolder
import sdk.chat.demo.robot.holder.TimeHolder
import sdk.chat.ui.chat.model.MessageHolder
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

//    init {
//        if (items.isEmpty()) {
//            items.add(ExploreHolder())
//        }
//    }

    //    private val exploreHolder = ExploreHolder()
    var aiExplore: AIExplore? = null
        set(value) {
            field = value
            Log.e("AIExplore", "set AIExplore:" + aiExplore?.message?.id)
            Handler(Looper.getMainLooper()).postDelayed(
                { notifyItemChanged(0); },
                2
            )
        }

    var header = false
        get() = field
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

    fun clear() {

        Log.e("AIExplore", "clear")
        if (items.isNotEmpty()) {
            items.clear()
            notifyDataSetChanged()
        }
    }

    @SuppressLint("CheckResult")
    private fun submitList(newList: List<IMessage>, onComplete: (() -> Unit)? = null) {
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
//                items.add(exploreHolder);
                items.addAll(newList)
                diffResult.dispatchUpdatesTo(this@ChatAdapter)
//                notifyDataSetChanged()
                onComplete?.invoke()
            }, { error ->
                Log.e("ChatAdapter", "DiffUtil计算失败", error)
                // 降级方案：普通全量刷新
                items.clear()
//                items.add(exploreHolder);
                items.addAll(newList)
                notifyDataSetChanged()
                onComplete?.invoke()
            })
    }

    // 添加新消息（自动插入到头部）
    fun addNewMessage(item: IMessage, onComplete: (() -> Unit)? = null) {
//        val newList = items.toMutableList().apply { add(1, item) }
//        submitList(newList, onComplete)
        Log.e("AIExplore", "addNewMessage:${item.id}")
        var msg = (item as? MessageHolder)?.message
        val oldHeader = if (itemCount > 0) {
            getItemViewType(0) == TYPE_HEADER
        } else {
            false // 如果 itemCount 为 0，直接返回 false
        }

        if (oldHeader) {
            items[0] = ExploreHolder(msg)
        } else {
            items.add(0, ExploreHolder(msg))
        }

        items.add(1, item)

        // 批量通知
        if (oldHeader) {
            notifyItemChanged(0)      // 头部内容变化
            notifyItemInserted(1)     // 新项插入
        } else {
            notifyItemRangeInserted(0, 2)  // 两个新项插入
        }
        onComplete?.invoke()
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
//        if (itemCount == 0) {
//            var msgHolder = newMessages[0]
//            var message = when (msgHolder) {
//                is TextHolder -> msgHolder.message
//                is ImageHolder -> msgHolder.message
//                else -> null
//            }
//            if(message!=null){
//                items.add(ExploreHolder(message))
//            }
//        }
        Log.e("AIExplore", "addNewMessage List")

        var msg = (newMessages[newMessages.lastIndex] as? MessageHolder)?.message
        val oldHeader = if (itemCount > 0) {
            getItemViewType(0) == TYPE_HEADER
        } else {
            false // 如果 itemCount 为 0，直接返回 false
        }

        if (oldHeader) {
            items[0] = ExploreHolder(msg)
        } else {
            items.add(0, ExploreHolder(msg))
        }

        items.addAll(1, newMessages)
//        notifyDataSetChanged()

        // 批量通知
        if (oldHeader) {
            notifyItemChanged(0)      // 头部内容变化
            notifyItemRangeInserted(1, newMessages.size)     // 新项插入
        } else {
            notifyItemRangeInserted(0, newMessages.size + 1)  // 两个新项插入
        }
        onComplete?.invoke()


//        if (itemCount == 0 && newMessages.isNotEmpty()) {
//            (newMessages[0] as? MessageHolder)?.message?.let { items.add(ExploreHolder(it)) }
//        }
//        items.addAll(newMessages)
//        notifyItemRangeInserted(0, newMessages.size)
//        items.apply {
//            if (size >= 2) {
//                val tail = subList(2, size)
//                subList(1, size).clear()
//                addAll(1, newMessages)
//                addAll(tail)
//            } else if (size == 1) {
//                addAll(1, newMessages)
//            } else {
//                items.add(ExploreHolder())
//                addAll(0, newMessages)
//            }
//        }
//        notifyDataSetChanged()

//        val newList = ArrayList<IMessage>(items.size + newMessages.size).apply {
//            // 1. 添加旧数据的第一条（如果存在,这是探索...）
//            if (items.isNotEmpty()) {
//                add(items[0])
//            }
//
//            // 2. 追加所有新消息
//            addAll(newMessages)
//
//            // 3. 追加旧数据第二条及之后的数据（如果存在）
//            if (items.size > 1) {
//                addAll(items.subList(1, items.size))
//            }
//        }
//
//        submitList(newList, onComplete)

    }

    // 批量添加历史消息（追加到尾部）
    fun addHistoryMessages(newItems: List<IMessage>, onComplete: (() -> Unit)? = null) {
        Log.e("AIExplore", "addHistoryMessages:${newItems.size}")
//        val newList = items.toMutableList().apply { addAll(newItems) }
//        submitList(newList, onComplete)
        var s = itemCount
        if (itemCount == 0 && newItems.isNotEmpty()) {
            (newItems[0] as? MessageHolder)?.message?.let { items.add(ExploreHolder(it)) }
        }
        items.addAll(newItems)
        notifyItemRangeInserted(s, newItems.size + 1)
        onComplete?.invoke()
    }


    fun delMessage(item: IMessage, onComplete: (() -> Unit)? = null) {
        // Create new list with items removed
        val deletePos = items.indexOfFirst { it.id == item.id }

        if (deletePos == -1) {
//            Log.e("delmsg", "message.id:${message.id},del:-1,size:${items.size}")
            onComplete?.invoke()
            return
        }

        Log.e("AIExplore", "delMessage:${item.id},deletePos:${deletePos}")
//        var newExploreHolder: ExploreHolder? = null
//        if (itemCount > deletePos) {
//            var msg = (items[deletePos + 1] as? MessageHolder)?.message
//            newExploreHolder = ExploreHolder(msg)
//        }

//        var msg = (item as? MessageHolder)?.message
        items.removeAt(deletePos)
        if (deletePos == 1) {
            val oldExploreMsg = (items[0] as? ExploreHolder)?.message
            if (oldExploreMsg != null && oldExploreMsg.entityID == item.id) {
                Log.e("AIExplore", "delMessage:${item.id},deletePos:${deletePos}, and del explore")
                items.removeAt(0)
                notifyItemRangeRemoved(0,2)
                onComplete?.invoke()
                return
            }
        }
        notifyItemRemoved(deletePos)
        onComplete?.invoke()

//        if (delExplore) {
//            items.removeAt(deletePos)
//            items.removeAt(0)
//            notifyItemChanged(0)      // 头部内容变化
//            notifyItemRangeInserted(1, newMessages.size)     // 新项插入
//        } else {
//            notifyItemRangeInserted(0, newMessages.size + 1)  // 两个新项插入
//        }
//
//        notifyItemRemoved(deletePos)
//        onComplete?.invoke()
//        val newList = ArrayList(items).apply { removeAt(deletePos) }
//        //status: 0: pending,1:idle
//        submitList(newList, onComplete)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            else -> when (getItem(position)) {
                is ExploreHolder -> TYPE_HEADER
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

//        Log.e("AIExplore", "onBindViewHolder:" + position+","+holder)
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
                val item = getItem(position)
                holder.bind(header, item as ExploreHolder)
//                bindListeners(holder, null)
            }

            is FooterViewHolder -> holder.bind(footer)
        }
    }

    override fun getItemCount(): Int = items.size

    /* 内部工具方法 */
    private fun inflateView(layoutId: Int, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    private fun getItem(position: Int): IMessage {
        return items[position]
    }

//    private fun isHeaderPosition(pos: Int) = hasHeader() && pos == 0
//    private fun isFooterPosition(pos: Int) = hasFooter() && pos == itemCount - 1
//    private fun hasHeader() = false
//    private fun hasFooter() = footer != null
//    private fun headerOffset() = if (hasHeader()) 1 else 0
//    private fun footerOffset() = if (hasFooter()) 1 else 0
//    private fun getInsertPosition() = if (hasHeader()) 1 else 0


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