package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.DiffUtil
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.pre.R;
import androidx.recyclerview.widget.ListAdapter

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem

    // 可选：获取变更内容（用于部分更新）
    override fun getChangePayload(oldItem: Article, newItem: Article): Any? {
        return if (oldItem.title != newItem.title) {
            ArticleAdapter.SummaryUpdatePayload(newItem.title)
        } else {
            null
        }
    }
}

class ArticleAdapter(
    private val onItemClick: (Article) -> Unit,
    private val onEditClick: (Article) -> Unit,
    private val onLongClick: (View, Article) -> Boolean,
) : ListAdapter<Article, RecyclerView.ViewHolder>(ArticleDiffCallback()) {

    private var _selectId: String? = null;

    val selectId: String?
        get() = _selectId

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
        return if (position == currentList.size) TYPE_FOOTER else TYPE_ITEM
    }

    override fun getItemCount(): Int = currentList.size + 1

    fun clearAll() {
        submitList(emptyList()) // 清空并自动通知DiffUtil更新
    }

    fun appendItems(newItems: List<Article>, commitCallback: Runnable?) {
        val updatedList = currentList.toMutableList().apply {
            addAll(newItems)
        }
        submitList(updatedList, commitCallback)
    }

    fun updateSummaryById(id: String?, newSummary: String): Boolean {

        if (id == null) return false

        // 创建新列表（不可变操作）
        val newList = currentList.map { article ->
            if (article.id == id) {
                article.copy(title = newSummary)
            } else {
                article
            }
        }

        // 查找需要更新的位置
        val position = currentList.indexOfFirst { it.id == id }
        if (position == -1) return false

        // 提交更新（使用payload优化）
        submitList(newList) {
            // 提交完成后的回调
            val payload = SummaryUpdatePayload(newSummary)
            notifyItemChanged(position, payload)
        }

        return true
    }

    fun deleteById(id: String?): Boolean {

        if (id == null) return false
        val newList = currentList.toMutableList()
        newList.removeAll { it.id == id }
        submitList(newList)
        return true
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCardView: View = itemView.findViewById(R.id.cardView)
        val editTitle: View = itemView.findViewById(R.id.editTitle)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val tvContentMask: View = itemView.findViewById(R.id.tvTimeMask)
        val tvDashedLine: View = itemView.findViewById(R.id.vDashedLine)

        fun bind(article: Article) {
            tvDay.text = article.day
            tvTime.text = article.time
            tvTitle.text = article.title
            tvContent.text = article.content
//            tvCardView.setBackgroundColor(article.colorTag)
            itemView.setOnClickListener { onItemClick(article) }
            itemView.setOnLongClickListener { v -> _selectId = article.id;onLongClick(v, article) }
            editTitle.setOnClickListener { _selectId = article.id;onEditClick(article) }

            if (!article.showDay) {
                tvDay.visibility = View.INVISIBLE
                tvContentMask.visibility = View.INVISIBLE
            } else {
                tvDay.visibility = View.VISIBLE
                tvContentMask.visibility = View.VISIBLE
            }

            if (article.isFirstDay) {
                tvDashedLine.visibility = View.GONE
            } else {
                tvDashedLine.visibility = View.VISIBLE
            }
        }

        fun updateSummary(newSummary: String) {
            tvTitle.text = newSummary
        }
    }

    inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var contentLoadingProgressBar: ContentLoadingProgressBar =
            itemView.findViewById<ContentLoadingProgressBar?>(
                R.id.pb_progress
            )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOOTER -> FooterViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_footer, parent, false)
            )

            else -> ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_article, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder && position < currentList.size) {
            holder.bind(getItem(position))
        } else if (holder is FooterViewHolder) {
            holder.contentLoadingProgressBar.visibility =
                if (isLoading) View.VISIBLE else View.INVISIBLE
        }
    }

    // 带 Payload 的绑定方法
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val article = getItem(position)
            when (holder) {
                is ViewHolder -> {
                    payloads.forEach { payload ->
                        when (payload) {
                            is SummaryUpdatePayload -> holder.updateSummary(payload.newSummary)
                        }
                    }
                }

                is FooterViewHolder -> {
                    // 处理特色文章的部分更新
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    internal data class SummaryUpdatePayload(val newSummary: String)
//    override fun getItemCount() = articles.size
}