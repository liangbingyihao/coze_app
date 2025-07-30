package sdk.chat.demo.robot.extensions

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sdk.chat.demo.pre.R

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
    title: String?,
    message: String,
    positiveAction: () -> Unit
) {
    val dialog = MaterialAlertDialogBuilder(context)
//            .setTitle(title)
        .setMessage(message)
        .setPositiveButton(context.getString(R.string.confirm)) { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        }
        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
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

fun Activity.disableSwipeBack() {
}