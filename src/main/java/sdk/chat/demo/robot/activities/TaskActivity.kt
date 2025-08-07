package sdk.chat.demo.robot.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.gyf.immersionbar.ImmersionBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.TaskPieAdapter
import sdk.chat.demo.robot.api.model.TaskDetail
import sdk.chat.demo.robot.api.model.TaskProgress
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
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
    private lateinit var imTaskImage: ImageView

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
        imTaskImage = findViewById<ImageView>(R.id.fullscreenImageView)
        storyContainer = findViewById<LinearLayout>(R.id.storyContainer)
        tvStory = findViewById<TextView>(R.id.story)
        tvStoryTitle = findViewById<TextView>(R.id.storyTitle)
        tvStoryName = findViewById<TextView>(R.id.storyName)
        findViewById<View>(R.id.calendar_enter).setOnClickListener(this)
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
            if (taskContainer.mode == 0) {
                if (row == 0) {
                    ImageViewerActivity.start(this@TaskActivity);
                    finish()
                } else if (row == 1) {
                    val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                    var date = formatDayAgo(0);
//                threadHandler.aiExplore.contextId
                    threadHandler.sendExploreMessage(
                        "【每日恩语】-${date}",
                        threadHandler.aiExplore.message,
                        GWThreadHandler.action_daily_gw_pray,
                        date
                    ).subscribe();
                    finish()
                } else {
                    ChatSDK.events().source()
                        .accept(NetworkEvent.messageInputPrompt(""))
                    finish()
                }
            }
//            DailyTaskHandler.setTaskDetail(taskDetail)
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

            R.id.calendar_enter -> {
                TaskCalendarActivity.start(this@TaskActivity)
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

        Glide.with(this@TaskActivity)
            .load(taskProcess.progressImage)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.mipmap.ic_placeholder_task) // 占位图
            .error(R.mipmap.ic_placeholder_task) // 错误图
            .into(imTaskImage)
    }

    private fun setStoryData(index: Int) {
        if (index > taskDetail.index) {
            storyContainer.visibility = View.GONE
        } else if (index == taskDetail.index && !taskDetail.isAllCompleted) {
            storyContainer.visibility = View.GONE
        } else {
            storyContainer.visibility = View.VISIBLE
//            tvStory.text = taskProcess.chapters[index].content
//            tvStoryTitle.text = taskProcess.chapters[index].title
//            var html = "<br/><div style=\"text-align:center\"><b><h3><font color='#333333'>第一天：光的创造</font></h3></b></div>\n" +
//                    "\n" +
//                    "<p><strong><font color='#333333'>起初，世界一片混沌，黑暗笼罩。上帝说：“要有光！” 光便出现了。上帝将光与暗分开，称光为昼，暗为夜。这一天的创造，是秩序的开端，光代表着希望与启示，打破了无尽的黑暗，为后续的创造奠定基础，让世界有了时间的雏形 —— 昼夜交替，开启了万物生长的可能。</font></strong>\n" +
//                    "</p>\n" +
//                    "\n" +
//                    "<div style=\"text-align:center\"><span style=\"color:#817b7b;\"><b><h4>神7天创造的规律</h4></b></span></div>\n" +
//                    "<span style=\"color:#817b7b;\">\n" +
//                    "    1.从宏观到微观：\n" +
//                    "神先创造了宏观的宇宙框架，如第一天创造光，分开昼夜；第二天创造天空；第三天创造陆地和海洋，以及植物。之后才创造微观的生物，如第四天创造日月星辰管理时间，第五天创造水中的鱼和空中的鸟，第六天创造地上的走兽和人类。\n" +
//                    "  <br/>\n" +
//                    "  2.从简单到复杂：\n" +
//                    "创造的事物从简单的无机物逐渐发展到复杂的有机物和有生命的物体。先有光、天空、陆地等简单的元素，然后有植物、动物，最后创造出具有神形象的人类。\n" +
//                    "  <br/>\n" +
//                    "  3.有休息和祝福：\n" +
//                    "神在第七天歇了他一切的工，安息了，并赐福给第七日，定为圣日。这体现了神创造的节奏，也教导我们要劳逸结合，尊重安息的时间。\n" +
//                    "  <br/>\n" +
//                    "</span>"
            tvStory.text = HtmlCompat.fromHtml(taskProcess.chapters[index].content, HtmlCompat.FROM_HTML_MODE_LEGACY);
        }
    }
}