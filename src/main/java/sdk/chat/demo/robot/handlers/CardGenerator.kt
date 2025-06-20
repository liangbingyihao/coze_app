package sdk.chat.demo.robot.handlers;

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import sdk.chat.demo.MainApp
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import sdk.chat.demo.pre.R
import com.bumptech.glide.request.target.CustomTarget
import kotlinx.coroutines.suspendCancellableCoroutine
import sdk.chat.demo.robot.api.model.ImageDaily
import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class CardGenerator private constructor() {

    // 单例模式
    companion object {
        @Volatile
        private var instance: CardGenerator? = null

        fun getInstance(): CardGenerator {
            return instance ?: synchronized(this) {
                instance ?: CardGenerator().also { instance = it }
            }
        }
    }

    // 线程池（避免频繁创建线程）
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 内存缓存（缓存生成的卡片）
    private val memoryCache = LruCache<String, Bitmap>(10 * 1024 * 1024) // 10MB

    // 获取缓存目录
    private val cacheDir by lazy {
        File(MainApp.getContext().cacheDir, "card_cache").apply { mkdirs() }
    }

    /**
     * 获取缓存图片的本地Uri（兼容Android 7+ FileProvider）
     * @param key 缓存键（同生成时使用的key）
     * @return 返回 content:// 或 file:// Uri
     */
    fun getCachedCardUri(key: String): Uri? {
        val file = getCachedCardFile(key) ?: return null
        return if (file.exists()) {
            // 适配Android 7+ FileProvider
            FileProvider.getUriForFile(
                MainApp.getContext(),
                "${MainApp.getContext().packageName}.provider",
                file
            )
        } else {
            null
        }
    }

    /**
     * 从磁盘缓存获取卡片
     */
    fun getCachedCardFile(key: String): File? {
        val file = File(cacheDir, "${key.hashCode()}.jpg")
        return if (file.exists()) file else null
    }

    /**
     * 保存卡片到磁盘缓存
     */
    private fun saveCardToCache(key: String, bitmap: Bitmap): Boolean {
        return try {
            val file = File(cacheDir, "${key.hashCode()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }


    /**
     * 清理缓存
     */
    fun clearCache() {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
    }


    fun generateScriptureCard(
        context: Context,
        imageBitmap: Bitmap,
        text: String,
        maxWidth: Int = getScreenWidth(context),
        textColor: Int = Color.BLACK,
        textSize: Float = (getScreenWidth(context) * 0.04f).coerceAtLeast(12f), // 自适应文字大小
        typeface: Typeface = Typeface.DEFAULT_BOLD,
        padding: Float = 80f,
        lineSpacingMultiplier: Float = 1.0f,
        lineSpacingExtra: Float = 40f,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        textBackgroundColor: Int = 0xFFFAFAFA.toInt(),
        textBackgroundCornerRadius: Float = 0f,
        minHeight: Int = getScreenHeight(context),
        maxHeight: Int = getScreenHeight(context) * 2 // 限制最大高度为屏幕2倍
    ): Bitmap {
        // 1. 计算图片缩放比例（限制宽度 ≤ 屏幕宽度）
        val screenWidth = maxWidth
        val imageWidth = imageBitmap.width
        val imageHeight = imageBitmap.height

        val scale = screenWidth.toFloat() / imageWidth // 强制适配屏幕宽度
        val scaledWidth = (imageWidth * scale).toInt()
        val scaledHeight = (imageHeight * scale).toInt()

        // 2. 缩放图片（高质量）
        val scaledBitmap = imageBitmap.scale(scaledWidth, scaledHeight)


        // 3. 设置自适应文字样式
        val textPaint = TextPaint().apply {
            color = textColor
            this.textSize = textSize
            this.typeface = typeface
            isAntiAlias = true
        }

        // 4. 计算文本布局（宽度 = 图片宽度 - 2*padding）
        val availableTextWidth = (scaledWidth - 2 * padding).coerceAtLeast(0F)
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, availableTextWidth.toInt())
            .setAlignment(alignment)
            .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            .setIncludePad(true)
            .build()

        val textHeight = staticLayout.height.toFloat()

        // 5. 计算最终高度（图片高度 + 文字高度 + 2*padding，并限制范围）
        val contentHeight = scaledHeight + textHeight + 3 * padding
        val finalHeight = contentHeight
//            .coerceAtLeast(minHeight.toFloat())
            .coerceAtMost(maxHeight.toFloat())
            .toInt()

        // 6. 创建输出Bitmap（宽度=缩放后图片宽度）
        val outputBitmap = createBitmap(scaledWidth, finalHeight).apply {
            if (!isMutable) {
                return generateScriptureCard(
                    context,
                    scaledBitmap.copy(Bitmap.Config.ARGB_8888, true),
                    text,
                    maxWidth,
                    textColor,
                    textSize,
                    typeface,
                    padding,
                    lineSpacingMultiplier,
                    lineSpacingExtra,
                    alignment,
                    textBackgroundColor,
                    textBackgroundCornerRadius,
                    minHeight,
                    maxHeight
                )
            }
        }

        // 7. 绘制内容
        Canvas(outputBitmap).apply {
            // 绘制图片（居中）
            drawBitmap(scaledBitmap, 0f, 0f, null)

            // 绘制文字背景（圆角矩形）
            if (textBackgroundColor != Color.TRANSPARENT) {
                val backgroundPaint = Paint().apply {
                    color = textBackgroundColor
                    isAntiAlias = textBackgroundCornerRadius > 0
                }
//                val backgroundRect = RectF(
//                    padding,
//                    scaledHeight + padding,
//                    scaledWidth - padding,
//                    finalHeight - padding
//                )
                val backgroundRect = RectF(
                    0F,
                    scaledHeight.toFloat(),
                    scaledWidth.toFloat(),
                    finalHeight.toFloat()
                )
                if (textBackgroundCornerRadius > 0) {
                    drawRoundRect(
                        backgroundRect,
                        textBackgroundCornerRadius,
                        textBackgroundCornerRadius,
                        backgroundPaint
                    )
                } else {
                    drawRect(backgroundRect, backgroundPaint)
                }
            }

            // 绘制文本（垂直居中）
//            val textTop = scaledHeight + padding +
//                    ((finalHeight - scaledHeight - 2 * padding - textHeight) / 2).coerceAtLeast(0f)
            save()
            translate(padding, scaledHeight + padding)
            staticLayout.draw(this)
            restore()
        }

        return outputBitmap
    }

    // 获取屏幕宽度
    private fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    // 获取屏幕高度
    private fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }


    fun captureView(
        view: View,
        callback: (Result<Bitmap>) -> Unit
    ) {
        executor.execute {
            try {
                // 1. 创建 Bitmap
                val bitmap = createBitmap(view.width, view.height)

                // 2. 将 View 绘制到 Bitmap
                val canvas = Canvas(bitmap)
                view.draw(canvas)

                // 切换到主线程返回结果
                mainHandler.post {
                    callback(Result.success(bitmap))
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(Result.failure(e))
                }
            }
        }
    }

    /**
     * 生成卡片图片（异步）
     * @param imageBitmap 上半部分图片
     * @param text 下半部分文本
     * @param callback 结果回调（在主线程执行）
     */
    fun generateCardAsync(
        context: Context,
        imageBitmap: Bitmap,
        text: String,
        callback: (Result<Bitmap>) -> Unit
    ) {
        executor.execute {
            try {
                // 在子线程执行生成操作
                val resultBitmap = generateScriptureCard(context, imageBitmap, text)

                // 切换到主线程返回结果
                mainHandler.post {
                    callback(Result.success(resultBitmap))
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(Result.failure(e))
                }
            }
        }
    }


    fun getCacheBitmap(cacheKey: String): Bitmap? {
        memoryCache.get(cacheKey)?.let {
            return it
        }
        getCachedCardFile(cacheKey)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let {
                memoryCache.put(cacheKey, it)
                return it
            }
        }
        return null
    }

    /**
     * 从网络图片URL生成卡片（异步）
     * @param imageUrl 网络图片地址
     * @param text 卡片文本
     * @param callback 结果回调（主线程执行）
     */
    fun generateBibleCard(
        context: Context,
        resId: Int,
        imageDetail: ImageDaily,
        onSuccess: (Bitmap) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        var cacheKey: String? = null
        var imageUrl: String? = null
        assert(resId == R.layout.view_popup_image_bible || resId == R.layout.item_image_gw)
        if (resId == R.layout.item_image_gw) {
            cacheKey = "image_daily_gw_${imageDetail.date}"
            imageUrl = imageDetail.backgroundUrl
        } else {
            cacheKey = "${imageDetail.backgroundUrl}|${imageDetail.scripture}"
            imageUrl = imageDetail.backgroundUrl
        }
//        // 1. 检查内存缓存
        memoryCache.get(cacheKey)?.let {
            if (!it.isRecycled) {
                onSuccess(it)
                return
            }
        }
//
//        // 2. 检查磁盘缓存
//        getCachedCardFile(cacheKey)?.let { file ->
//            BitmapFactory.decodeFile(file.absolutePath)?.let {
//                memoryCache.put(cacheKey, it)
//                onSuccess(Pair(cacheKey, it))
//                return
//            }
//        }


        // 使用Glide加载网络图片
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    // 生成卡片

                    CoroutineScope(Dispatchers.Main).launch {
                        try {

//                            val deferredBitmaps = listOf(
//                                async { loadBitmapWithGlide(context, imageUrl) },
//                                async { loadBitmapWithGlide(context, "https://api-test.kolacdn.xyz") },
//                            )
//                            val bitmaps = deferredBitmaps.awaitAll()

                            val bitmap = captureLayoutAsync(
                                context,
                                resId,
                                imageDetail,
                                resource,
                            )
                            if (bitmap != null) {
                                memoryCache.put(cacheKey, bitmap)
//                                saveCardToCache(cacheKey, bitmap)
                                onSuccess(bitmap)
                            } else {
                                onFailure(Throwable("生成失败"))
                            }
                        } catch (e: Exception) {
                            onFailure(e)
                        }
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    val error = IOException("图片加载失败: $imageUrl")
                    onFailure(error)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清理资源时的操作（可选）
                }
            })
    }

    suspend fun loadBitmapWithGlide(context: Context, url: String): Bitmap =
        suspendCancellableCoroutine { continuation ->
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    continuation.resume(resource) { cause ->
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    continuation.resumeWithException(
                        IOException("Image load failed: $url")
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    continuation.cancel()
                }
            }

            // 启动 Glide 请求
            Glide.with(context)
                .asBitmap()
                .load(url)
                .into(target)

            // 关键修复：添加 onCancellation 处理
            continuation.invokeOnCancellation {
                Glide.with(context).clear(target) // 协程取消时清理 Glide 请求
            }
        }

    @SuppressLint("SetTextI18n")
    suspend fun captureLayoutAsync(
        context: Context,
        layoutResId: Int,
        imageDetail: ImageDaily,
        bitmaps: Bitmap,
    ): Bitmap? = withContext(Dispatchers.Main) {
        val view = LayoutInflater.from(context).inflate(layoutResId, null, false).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }


        val img = view.findViewById<ImageView>(R.id.photoView)
        img.setImageBitmap(bitmaps)


        try {
            view.findViewById<TextView>(R.id.bible).apply {
                text = imageDetail.scripture + imageDetail.reference?.let { "\n($it)" }.orEmpty()
            }
            var viewContent: View = view.findViewById<View>(R.id.content)
            if (layoutResId == R.layout.item_image_gw) {
                view.findViewById<TextView>(R.id.day).apply {
                    text = imageDetail.date.substring(8)
                }
                view.findViewById<TextView>(R.id.month).apply {
                    text = imageDetail.date.substring(0, 7)
                }
                view.findViewById<View>(R.id.footer).visibility = View.VISIBLE
                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(
                        getScreenWidth(context),
                        View.MeasureSpec.EXACTLY
                    )
                val heightSpec = View.MeasureSpec.makeMeasureSpec(
                    getScreenHeight(context),
                    View.MeasureSpec.EXACTLY
                )
                viewContent.measure(widthSpec, heightSpec)
            } else {
                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(
                        getScreenWidth(context),
                        View.MeasureSpec.EXACTLY
                    )
                val heightSpec = View.MeasureSpec.makeMeasureSpec(
                    getScreenHeight(context),
                    View.MeasureSpec.AT_MOST
                )
                viewContent.measure(widthSpec, heightSpec)
            }


            viewContent.layout(0, 0, viewContent.measuredWidth, viewContent.measuredHeight)

            createBitmap(
                viewContent.measuredWidth.coerceAtLeast(1),
                viewContent.measuredHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).apply {
                Canvas(this).run { viewContent.draw(this) }
            }.compressToSafeSize()
        } catch (e: Exception) {
            null
        }
    }

    private fun Bitmap.compressToSafeSize(maxSizeKB: Int = 500): Bitmap {
        if (this.allocationByteCount / 1024 <= maxSizeKB) return this
        return try {
            val outputStream = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
        } catch (e: Exception) {
            this
        }
    }

    /**
     * 释放资源（在Activity销毁时调用）
     */
    fun release() {
        executor.shutdown()
    }
}