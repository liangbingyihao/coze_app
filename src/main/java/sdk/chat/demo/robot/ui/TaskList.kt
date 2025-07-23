package sdk.chat.demo.robot.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.TaskDetail

class TaskList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val cells = mutableListOf<View>()
    private var headerLabel: TextView
    private var contentContainer: LinearLayout

    // 设置按钮点击回调
    var onCellButtonClick: ((row: Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.lst_daily_task, this)

        headerLabel = findViewById(R.id.headerLabel)
        contentContainer = findViewById(R.id.contentContainer)
//        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.divider_horizontal)
//        showDividers = SHOW_DIVIDER_MIDDLE

        // 初始化3行内容
        repeat(3) { row ->
            val cell = LayoutInflater.from(context)
                .inflate(R.layout.item_daily_task, contentContainer, false)

            cell.findViewById<Button>(R.id.actionButton).setOnClickListener {
                onCellButtonClick?.invoke(row)
            }
            var resId = context.resources.getIdentifier(
                "task_name_$row",
                "string",
                context.packageName
            )
            cell.findViewById<TextView>(R.id.titleText).text = context.getString(resId)
            resId = context.resources.getIdentifier(
                "task_desc_$row",
                "string",
                context.packageName
            )
            cell.findViewById<TextView>(R.id.subtitleText).text = context.getString(resId)

            contentContainer.addView(cell)
            cells.add(cell)
        }
    }

    fun setTaskData(mode: Int, taskPending: TaskDetail? = null) {
        //mode:>0:已完成，=0：进行种，<0：未开始
        if (mode < 0) {
            visibility = VISIBLE
            for (index in 0..2){
                setCellAction(
                    index,
                    context.getString(R.string.task_action_p),
                )
            }
        } else if (taskPending != null && mode == 0 && !taskPending.isAllCompleted) {
            visibility = VISIBLE
            var status = taskPending.allTaskStatus
            for (index in 0..2){
                if(status[index]){
                    setCellAction(
                        index,
                        context.getString(R.string.task_action_d)
                    )
                }else{
                    setCellAction(
                        index,
                        context.getString(R.string.task_action_g),true
                    )
                }
            }
        }else{
            visibility = GONE
        }
    }

//    // 设置单元格数据
//
//    fun setCellData(
//        row: Int,
//        title: String,
//        subtitle: String,
//        buttonText: String = "Action",
//        isImportant: Boolean = false
//    ) {
//        require(row in 0..2) { "Row index must be 0-2" }
//
//        val cell = cells[row]
//        cell.findViewById<TextView>(R.id.titleText).text = title
//        cell.findViewById<TextView>(R.id.subtitleText).text = subtitle
////        cell.findViewById<Button>(R.id.actionButton).text = buttonText
//    }

    fun setCellAction(
        row: Int,
        buttonText: String = "Action",
        isImportant: Boolean = false
    ) {
        require(row in 0..2) { "Row index must be 0-2" }
        val cell = cells[row]
        var button: MaterialButton = cell.findViewById<MaterialButton>(R.id.actionButton)
        button.text = buttonText
        if (isImportant) {
            button.setTextColor(context.getColor(R.color.white))
            button.setBackgroundColor(context.getColor(R.color.item_text_selected))
        } else {
            button.setTextColor(context.getColor(R.color.item_text_selected))
            button.setBackgroundColor(context.getColor(R.color.button_pink))
        }
    }

}