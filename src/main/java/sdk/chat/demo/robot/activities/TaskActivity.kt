package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.TaskPieAdapter
import sdk.chat.demo.robot.api.model.TaskDetail
import sdk.chat.demo.robot.api.model.TaskProgress
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.ui.TaskList
import sdk.chat.ui.activities.BaseActivity

class TaskActivity : BaseActivity(), View.OnClickListener {
    private lateinit var pieContainer: ViewGroup
    private lateinit var taskContainer: TaskList
    private lateinit var taskDetail: TaskDetail
    private lateinit var taskProcess: TaskProgress
    private lateinit var taskPieAdapter: TaskPieAdapter
    private lateinit var storyContainer: View
    private lateinit var tvStory: TextView
    private lateinit var tvStoryTitle: TextView
    private lateinit var tvStoryName: TextView

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, date: String? = null) {
            val intent = Intent(context, TaskActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, date)
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        ImmersionBar.with(this)
            .titleBar(findViewById<View>(R.id.title_bar))
            .init()
        findViewById<View>(R.id.back).setOnClickListener(this)
        pieContainer = findViewById<LinearLayout>(R.id.pieContainer)
        storyContainer = findViewById<LinearLayout>(R.id.storyContainer)
        tvStory = findViewById<TextView>(R.id.story)
        tvStoryTitle = findViewById<TextView>(R.id.storyTitle)
        tvStoryName = findViewById<TextView>(R.id.storyName)
        taskPieAdapter = TaskPieAdapter(
            pieContainer, this@TaskActivity,
            onItemClick = { i ->
                // 处理长按
//                Toast.makeText(this, "i: $i", Toast.LENGTH_SHORT).show()
                taskContainer.setTaskData(taskDetail.index - i, taskDetail)
                setStoryData(i)
            })
        taskContainer = findViewById<TaskList>(R.id.taskContainer)
        taskContainer.onCellButtonClick = { row ->
            taskDetail.completeTaskByIndex(row)
            DailyTaskHandler.setTaskDetail(taskDetail)
        }

    }

    override fun getLayout(): Int {
        return R.layout.activity_task
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.back -> {
                finish()
            }

        }
    }

    override fun onError(e: Throwable) {
        if (ChatSDK.config().debug) {
            e.printStackTrace()
        }
        alert.onError(e)
    }

    private fun loadData() {
        dm.add(
            DailyTaskHandler.getTaskProgress()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
                            taskProcess = data
                            taskDetail = taskProcess.taskDetail
                            initTaskPie()
                        } else {
                            throw IllegalArgumentException("获取数据失败")
                        }
                    },
                    this
                )
        )
    }


    private fun initTaskPie() {
        tvStoryName.text = taskProcess.storyName
        taskPieAdapter.setData(taskDetail)
        taskContainer.setTaskData(0, taskDetail)
        setStoryData(taskDetail.index)
    }

    private fun setStoryData(index: Int) {
        if (index > taskDetail.index) {
            storyContainer.visibility = View.GONE
        } else if (index == taskDetail.index && !taskDetail.isAllCompleted) {
            storyContainer.visibility = View.GONE
        } else {
            storyContainer.visibility = View.VISIBLE
            tvStory.text = taskProcess.chapters[index].content
            tvStoryTitle.text = taskProcess.chapters[index].title
        }
    }
}