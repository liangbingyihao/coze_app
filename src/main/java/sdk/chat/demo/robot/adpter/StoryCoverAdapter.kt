package sdk.chat.demo.robot.adpter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.api.model.Story

class StoryCoverAdapter() : ListAdapter<Story, StoryCoverAdapter.ViewHolder>(DiffCallback()) {

    private class DiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem.id == newItem.id // 根据实际业务逻辑调整（例如比较URL或ID）
        }

        override fun areContentsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem.progressImage == newItem.progressImage // 默认直接比较内容
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: ImageView = view.findViewById<ImageView>(R.id.photoView).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
//        val content: View = view.findViewById<View>(R.id.bible_container);
//        val day: TextView = view.findViewById<TextView>(R.id.day);
//        val month: TextView = view.findViewById<TextView>(R.id.month);
//        val bible: TextView = view.findViewById<TextView>(R.id.bible);
//        val reference: TextView = view.findViewById<TextView>(R.id.reference);
//        val footer: View = view.findViewById<View>(R.id.footer);
////        val photoView: PhotoView = view.findViewById<PhotoView>(R.id.photoView)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_cover, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.photoView.context
        val item: Story = getItem(position)
        var url = item.progressImage

        Glide.with(context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.mipmap.ic_placeholder_task) // 占位图
            .error(R.mipmap.ic_placeholder_task) // 错误图
            .into(holder.photoView)
    }

    public fun replaceData(newData: List<Story>) {
        submitList(newData)
    }

    fun getItemAt(position: Int): Story? {
        return if (position in 0 until itemCount) getItem(position) else null
    }

}