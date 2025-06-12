package sdk.chat.demo.robot.adpter

import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.github.chrisbanes.photoview.PhotoView
import sdk.chat.demo.pre.R

class ImagePagerAdapter(
    private val imageUrls: List<String>,
    private val lifecycle: Lifecycle
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        val holder = ViewHolder(view)
        holder.photoView.setAllowParentInterceptOnEdge(true)
        holder.photoView.isZoomable = false
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

//
        val context = holder.photoView.context
        Glide.with(context)
            .load(imageUrls[position])
            .placeholder(R.drawable.icn_200_image_message_placeholder) // 占位图
            .error(R.drawable.icn_200_image_message_error) // 错误图
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // 仅在 Lifecycle 活跃时更新 UI
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
//                        holder.photoView.setImageDrawable(resource)
                        adjustImageScale(holder.photoView, resource,position)
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

    override fun getItemCount(): Int = imageUrls.size
}