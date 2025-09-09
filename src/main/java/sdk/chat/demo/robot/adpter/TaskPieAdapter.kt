package sdk.chat.demo.robot.adpter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.TaskDetail
import sdk.chat.demo.robot.api.model.TaskProgress
import java.lang.ref.SoftReference

//data class TaskPieHolder(val completed: Boolean, val index: Int)

//enum class ItemType {
//    DONE, PENDING, DASHED_PIE_TEXT, PIE_TODAY,
//}

class TaskPieAdapter(
    private val pieContainer: ViewGroup,
    private val context: Context,
    private val onItemClick: ((Int) -> Unit)?,
) {
    private var pieSelected: SoftReference<View>? = null;
    private lateinit var taskToday: TaskDetail;

    @SuppressLint("SetTextI18n")
    fun setData(data: TaskDetail) {
        Log.e("taskPie","setData and removeAllViews")
        taskToday = data
        pieContainer.removeAllViews()
        for (index in 0..6) {
            val itemView: View
            if (index < taskToday.index) {
                itemView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pie_check, pieContainer, false)
            } else if (index == taskToday.index) {
                if (taskToday.isAllCompleted) {
                    itemView = LayoutInflater.from(context)
                        .inflate(R.layout.item_pie_check, pieContainer, false)
                } else {
                    itemView = LayoutInflater.from(context)
                        .inflate(R.layout.item_pie_text, pieContainer, false).apply {
                            var text = findViewById<TextView>(R.id.pieText)
                            text.text = (index + 1).toString()
                            text.setTextColor(context.getColor(R.color.item_text_selected))
                            findViewById<View>(R.id.bg).setBackgroundResource(R.drawable.pie_bg_pink)
                        }
                }
                itemView.tag = index
                setSelectView(itemView)
            } else {
                itemView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pie_text, pieContainer, false).apply {
                        findViewById<TextView>(R.id.pieText).text = (index + 1).toString()
                        findViewById<View>(R.id.bg).setBackgroundResource(R.drawable.pie_bg)
                    }
            }

            // 设置布局参数（平均分配宽度）
            itemView.layoutParams = LinearLayout.LayoutParams(
                0, // width设为0，weight生效
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f // weight=1，5个子View各占1/5
            )
            itemView.tag = index

            itemView.setOnClickListener { v ->
                setSelectView(v)
                var index = v.tag as Int
                onItemClick?.invoke(index)
            }

            Log.e("taskPie","setData and addView $index")
            pieContainer.addView(itemView)
            pieContainer.requestLayout()

        }
    }

    fun setSelectView(newView: View?) {
        var index = newView?.tag as Int
        var oldView: View? = pieSelected?.get()
        if (oldView != null) {
            var oldIndex = oldView.tag as Int
            if (index != oldIndex) {
                oldView.findViewById<View>(R.id.bg_selected)?.visibility = View.GONE
                if (oldIndex != taskToday.index) {
                    var v: TextView? = oldView.findViewById<TextView>(R.id.pieText)
                    v?.setTextColor(context.getColor(R.color.item_text_normal))
                }
            }
        }
        newView.findViewById<View>(R.id.bg_selected)?.visibility = View.VISIBLE
        newView.findViewById<TextView>(R.id.pieText)
            ?.setTextColor(context.getColor(R.color.item_text_selected))

        pieSelected = SoftReference(newView)
    }

    fun setSelectIndex(i:Int){
        Log.e("taskPie","setSelectIndex:$i")
        var v = pieContainer.findViewWithTag<View>(i)
        v?.performClick()
    }
}