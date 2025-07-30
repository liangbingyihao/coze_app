package sdk.chat.demo.robot.adpter
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.github.chrisbanes.photoview.PhotoView
import sdk.chat.demo.pre.R
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.load.engine.DiskCacheStrategy
import sdk.chat.demo.robot.api.model.ImageDaily

class ImagePagerAdapter(
    private val lifecycle: Lifecycle
) : ListAdapter<ImageDaily, ImagePagerAdapter.ViewHolder>(DiffCallback()) {

    private class DiffCallback : DiffUtil.ItemCallback<ImageDaily>() {
        override fun areItemsTheSame(oldItem: ImageDaily, newItem: ImageDaily): Boolean {
            return oldItem.date == newItem.date // 根据实际业务逻辑调整（例如比较URL或ID）
        }

        override fun areContentsTheSame(oldItem: ImageDaily, newItem: ImageDaily): Boolean {
            return oldItem.date == newItem.date // 默认直接比较内容
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: ImageView = view.findViewById<ImageView>(R.id.photoView).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val content: View = view.findViewById<View>(R.id.bible_container);
        val day: TextView = view.findViewById<TextView>(R.id.day);
        val month: TextView = view.findViewById<TextView>(R.id.month);
        val bible: TextView = view.findViewById<TextView>(R.id.bible);
        val reference: TextView = view.findViewById<TextView>(R.id.reference);
        val footer: View = view.findViewById<View>(R.id.footer);
//        val photoView: PhotoView = view.findViewById<PhotoView>(R.id.photoView)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_gw, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.photoView.context
        holder.content.visibility= View.INVISIBLE
        holder.footer.visibility= View.INVISIBLE
        val item:ImageDaily  = getItem(position)
        var url = item.backgroundUrl
        Glide.with(context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.mipmap.ic_placeholder) // 占位图
            .error(R.mipmap.ic_placeholder) // 错误图
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                @SuppressLint("SetTextI18n")
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // 仅在 Lifecycle 活跃时更新 UI
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        holder.content.visibility= View.VISIBLE
                        holder.day.text = item.date.substring(8)
                        holder.month.text = item.date.substring(0,7)
                        holder.bible.text = item.scripture+"\n("+item.reference+")"
                        holder.photoView.setImageDrawable(resource)
                        return false
                    }
                    return false
                }
            })
            .into(holder.photoView)
    }

    private fun adjustImageScale(photoView: PhotoView, drawable: Drawable?, position: Int) {
        drawable ?: return
        with(photoView) {
            val run = Runnable {
                val drawableWidth = drawable.intrinsicWidth.toFloat()
                val drawableHeight = drawable.intrinsicHeight.toFloat()
                val viewWidth = photoView.width.toFloat()
                val viewHeight = photoView.height.toFloat()
                setScale(viewHeight / drawableHeight, 0f, viewHeight, true)
//                notifyItemChanged(position)
            }
            post(run)
        }
//        photoView.post {
//            val drawableWidth = drawable.intrinsicWidth.toFloat()
//            val drawableHeight = drawable.intrinsicHeight.toFloat()
//            val viewWidth = photoView.width.toFloat()
//            val viewHeight = photoView.height.toFloat()
//
//            // 1. 计算缩放比例（宽度撑满）
////            val scale = viewWidth / drawableWidth
//            val scale = 2F
//
//            // 2. 设置初始缩放比例（关键改动）
//            photoView.setScale(scale, 0f, 0f, false)
//
//            // 3. 计算缩放后的高度
//            val scaledHeight = drawableHeight * scale
//
//            // 4. 如果图片高度不足，垂直居中
//            if (scaledHeight < viewHeight) {
//                val offsetY = (viewHeight - scaledHeight) / 2
//                photoView.setDisplayMatrix(Matrix().apply {
//                    postTranslate(0f, offsetY)
//                })
//            }
//        }
    }

    // 在列表头部插入数据
    public fun prependData(newData: List<ImageDaily>) {
        val newList = newData + currentList // 新数据在前
        submitList(newList) // 自动触发DiffUtil
    }

    public fun replaceData(newData: List<ImageDaily>) {
        submitList(newData)
    }

    fun getUrlAt(position: Int): ImageDaily? {
        return if (position in 0 until itemCount) getItem(position) else null
    }

}