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