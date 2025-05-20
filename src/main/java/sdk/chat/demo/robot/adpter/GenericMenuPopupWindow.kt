package sdk.chat.demo.robot.adpter

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupWindow
import sdk.chat.demo.pre.R
import kotlin.math.min
import androidx.core.graphics.drawable.toDrawable

class GenericMenuPopupWindow<T, VH : GenericMenuAdapter.ViewHolder<T>>(
    private val context: Context,
    private val anchor: View,
    private val adapter: GenericMenuAdapter<T, VH>,
    private val onItemSelected: (T, Int) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var listView: ListView? = null

    fun show() {
        if (popupWindow?.isShowing == true) {
            dismiss()
            return
        }

        initPopupWindow()
        popupWindow?.showAsDropDown(anchor, 0, 0, Gravity.START)
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }


    fun updateMenuItems(newItems: List<T>, newSelectedPos: Int = -1) {
        adapter.updateData(newItems, newSelectedPos)
        adjustPopupHeight()
    }

    private fun initPopupWindow() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_window_layout, null)
        listView = popupView.findViewById(R.id.menu_listview)

        listView?.adapter = adapter
        listView?.setOnItemClickListener { _, _, position, _ ->
            adapter.updateSelectedPosition(position)
            onItemSelected(adapter.getItem(position), position)
            dismiss()
        }
        var b:View = popupView.findViewById(R.id.blank_room)
        b.setOnClickListener { v->dismiss() }

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
//            animationStyle = R.style.PopupWindowAnimation
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = true
        }

        adjustPopupHeight()
    }

    private fun adjustPopupHeight() {
        return
        listView?.post {
            val maxHeight = (context.resources.displayMetrics.heightPixels * 0.6).toInt()
            var totalHeight = 0
            var width = context.resources.displayMetrics.widthPixels
            for (i in 0 until adapter.count) {
                val itemView = adapter.getView(i, null, listView!!)
                itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                totalHeight += itemView.measuredHeight
            }

            val params = listView?.layoutParams
            params?.height = min(totalHeight, maxHeight)
            listView?.layoutParams = params
            popupWindow?.update(width, params?.height ?: maxHeight)
        }
    }
}