import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.core.content.FileProvider
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import java.io.IOException
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import sdk.chat.demo.MainApp
import java.io.File
import java.io.FileOutputStream

class CardGenerator private constructor() {

    // 单例模式
    companion object {
        @Volatile private var instance: CardGenerator? = null

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
    private fun getCachedCardFile(key: String): File? {
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
                val resultBitmap = generateCard(context, imageBitmap, text)

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

    /**
     * 同步生成卡片（需自行处理线程）
     */
    fun generateCard(
        context: Context,
        imageBitmap: Bitmap,
        text: String
    ): Bitmap {
        // 1. 计算文本高度
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val padding = 40f

        // 2. 创建输出Bitmap
        val outputBitmap = createBitmap(
            imageBitmap.width,
            imageBitmap.height + textBounds.height() + padding.toInt()
        ).apply {
            if (!isMutable) {
                // 如果Bitmap不可变，创建可变副本
                return generateCard(context, imageBitmap.copy(Bitmap.Config.ARGB_8888, true), text)
            }
        }

        // 3. 绘制内容
        Canvas(outputBitmap).apply {
            // 绘制图片（上半部分）
            drawBitmap(imageBitmap, 0f, 0f, null)

            // 绘制文本（下半部分）
            val textX = (width - textPaint.measureText(text)) / 2 // 水平居中
            val textY = imageBitmap.height - padding - textBounds.height()
            drawText(text, textX, textY, textPaint)
        }

        return outputBitmap
    }

    fun getCacheBitmap(imageUrl: String,text: String): Bitmap? {
        val cacheKey = "$imageUrl|$text"
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
    fun generateCardFromUrl(
        context: Context,
        imageUrl: String,
        text: String,
        callback: (Result<Bitmap>) -> Unit
    ) {
        // 1. 检查内存缓存
        val cacheKey = "$imageUrl|$text"
        memoryCache.get(cacheKey)?.let {
            callback(Result.success(it))
            return
        }

        // 2. 检查磁盘缓存
        getCachedCardFile(cacheKey)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let {
                memoryCache.put(cacheKey, it)
                callback(Result.success(it))
                return
            }
        }

        // 使用Glide加载网络图片
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                )
                {
                    // 生成卡片
                    generateCardAsync(context, resource, text) { result ->
                        result.onSuccess { bitmap ->
                            // 存入缓存
                            memoryCache.put(cacheKey, bitmap)
                            saveCardToCache(cacheKey, bitmap)
                            callback(Result.success(bitmap))
                        }.onFailure {
                            callback(Result.failure(it))
                        }
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    val error = IOException("图片加载失败: $imageUrl")
                    callback(Result.failure(error))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清理资源时的操作（可选）
                }
            })
    }
    /**
     * 释放资源（在Activity销毁时调用）
     */
    fun release() {
        executor.shutdown()
    }
}