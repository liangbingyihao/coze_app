package sdk.chat.demo.robot.extensions

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sdk.chat.demo.pre.R
import androidx.recyclerview.widget.RecyclerView

fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

fun callMethodByName(obj: Any, methodName: String, vararg args: Any?): Any? {
    try {
        // 获取参数类型数组
        val paramTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()

        // 获取方法对象
        val method = obj.javaClass.getMethod(methodName, *paramTypes)

        // 调用方法
        return method.invoke(obj, *args)
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException("Failed to call method '$methodName'", e)
    }
}

fun showMaterialConfirmationDialog(
    context: Context,
    message: String,
    txtPositive: String?,
    txtNegative: String?,
    positiveAction: () -> Unit
) {
    val dialog = MaterialAlertDialogBuilder(context)
//            .setTitle(title)
        .setMessage(message)
        .setPositiveButton(txtPositive?:context.getString(R.string.confirm)) { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        }
        .setNegativeButton(txtNegative?:context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        .setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_background))
        .create()

    dialog.setOnShowListener {
        // 获取按钮并自定义
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
//                setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
            setTextColor(ContextCompat.getColor(context, R.color.item_text_selected))
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.item_text_normal))
        }
    }

    dialog.show()
}

fun findTopmostVisibleViewByResId(recyclerView: RecyclerView, resId: Int): View? {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
    val lastVisiblePos = layoutManager.findLastVisibleItemPosition()

    var topmostView: View? = null
    var minY = Int.MAX_VALUE // 记录最小的Y坐标（最靠近顶部）

    for (i in firstVisiblePos..lastVisiblePos) {
        val holder = recyclerView.findViewHolderForAdapterPosition(i)
        val targetView = holder?.itemView?.findViewById<View>(resId)
        if (targetView != null && targetView.isShown) {
            val location = IntArray(2)
            targetView.getLocationOnScreen(location) // 获取屏幕坐标
            if (location[1] < minY) { // Y坐标更小表示更靠近顶部
                minY = location[1]
                topmostView = targetView
            }
        }
    }
    return topmostView
}

fun Activity.disableSwipeBack() {
}