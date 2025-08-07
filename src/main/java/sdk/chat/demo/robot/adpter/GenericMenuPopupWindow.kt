package sdk.chat.demo.robot.adpter

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import sdk.chat.demo.pre.R
import kotlin.math.min
import androidx.core.graphics.drawable.toDrawable

class GenericMenuPopupWindow<T, VH : GenericMenuAdapter.ViewHolder<T>>(
    private val context: Context,
    private val anchor: View,
    private val adapter: GenericMenuAdapter<T, VH>,
    private val onItemSelected: (T?, Int) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var listView: ListView? = null
    private var tvTitle: TextView? = null
    private lateinit var menuNew: TextView
    private var menuTitle: String? = null
    private var anchorLocation: IntArray? = null
    private lateinit var blankTop: View
    private lateinit var blankBottom: View
    public var isEditModel=false

    fun editModel() {
        menuNew.visibility = View.VISIBLE
        blankTop.visibility = View.VISIBLE
        tvTitle?.visibility = View.GONE
        blankBottom.visibility = View.GONE
        isEditModel = true
    }

    fun viewModel() {
        menuNew.visibility = View.GONE
        blankTop.visibility = View.GONE
        tvTitle?.visibility = View.VISIBLE
        blankBottom.visibility = View.VISIBLE
        isEditModel = false
    }

    fun show(editModel: Boolean) {
        if (popupWindow?.isShowing == true) {
            dismiss()
            return
        }
        if (popupWindow == null) {
            initPopupWindow()
        }
        if (editModel) {
            editModel()
        } else {
            viewModel()
        }
        // 3. 获取anchor在屏幕中的位置
        if (anchorLocation == null) {
            anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)
        }
        // 4. 显示PopupWindow（精确覆盖）
        popupWindow?.showAtLocation(
            anchor,
            Gravity.NO_GRAVITY,
            anchorLocation?.get(0) ?: 0, // x坐标
            anchorLocation?.get(1) ?: 0  // y坐标
        )
//        popupWindow?.showAsDropDown(anchor, 0, 0, Gravity.START)
    }

    fun setTitle(title: String) {
        menuTitle = title
        tvTitle?.text = title
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }


    fun updateMenuItems(newItems: MutableList<T>, newSelectedPos: Int = -1) {
        adapter.updateData(newItems, newSelectedPos)
        adjustPopupHeight()
    }


    fun updateMenuItemSelected(newItems: T) {
        adapter.updateItemAt(adapter.getSelectedPosition(),newItems)
    }


    private fun initPopupWindow() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_window_layout, null)
        listView = popupView.findViewById(R.id.menu_listview)
        tvTitle = popupView.findViewById(R.id.menu_title)
        menuNew = popupView.findViewById(R.id.menu_new)
        blankTop = popupView.findViewById(R.id.blank_room_top)
        blankBottom = popupView.findViewById(R.id.blank_room_bottom)
        tvTitle?.text = menuTitle

        listView?.adapter = adapter
        listView?.setOnItemClickListener { _, _, position, _ ->
            if(!isEditModel){
                adapter.updateSelectedPosition(position)
            }
            onItemSelected(adapter.getItem(position), position)
            dismiss()
        }
        var b: View = popupView.findViewById(R.id.popup_container)
        b.setOnClickListener { v -> dismiss() }
        menuNew.setOnClickListener {
            onItemSelected(null, -1);
            dismiss()
        }

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