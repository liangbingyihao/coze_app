package sdk.chat.demo.robot.adpter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.pre.R;
import androidx.recyclerview.widget.ListAdapter

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
}

class ArticleAdapter(
    private val onItemClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.ViewHolder>(ArticleDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = getItem(position)  // 使用 getItem() 而非直接访问列表
        holder.tvTime.text = article.time
        holder.tvTitle.text = article.title
        holder.tvContent.text = article.content

        holder.itemView.setOnClickListener { onItemClick(article) }

//        val timeLineView = holder.itemView.findViewById<View>(R.id.timeLine)
//        timeLineView.layoutParams.height = if (isLastItem) {
//            // 最后一项只显示到圆点
//            dpToPx(24f)
//        } else {
//            ViewGroup.LayoutParams.MATCH_PARENT
//        }
    }

//    override fun getItemCount() = articles.size
}