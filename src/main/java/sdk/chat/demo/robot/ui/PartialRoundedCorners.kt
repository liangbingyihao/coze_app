package sdk.chat.demo.robot.ui
import android.graphics.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class PartialRoundedCorners(
    private val topLeft: Float = 0f,
    private val topRight: Float = 0f,
    private val bottomLeft: Float = 0f,
    private val bottomRight: Float = 0f
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // 创建目标 Bitmap
        val result = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        result.setHasAlpha(true)

        // 创建 Canvas 和 Path
        val canvas = Canvas(result)
        val path = Path()
        val rect = RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat())

        // 定义圆角半径（8个值：每角X/Y半径）
        val radii = floatArrayOf(
            topLeft, topLeft,        // 左上角
            topRight, topRight,      // 右上角
            bottomRight, bottomRight,// 右下角
            bottomLeft, bottomLeft   // 左下角
        )

        // 添加圆角矩形路径
        path.addRoundRect(rect, radii, Path.Direction.CCW)
        canvas.clipPath(path)

        // 绘制原图
        canvas.drawBitmap(toTransform, 0f, 0f, null)
        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(
            "PartialRoundedCorners($topLeft,$topRight,$bottomLeft,$bottomRight)"
                .toByteArray()
        )
    }
}