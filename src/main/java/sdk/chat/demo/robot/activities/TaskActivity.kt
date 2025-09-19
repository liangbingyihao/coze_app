package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.gyf.immersionbar.ImmersionBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.StoryCoverAdapter
import sdk.chat.demo.robot.adpter.TaskPieAdapter
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.api.model.Story
import sdk.chat.demo.robot.api.model.TaskProgress
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.extensions.LogHelper
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.TaskList
import sdk.chat.ui.activities.BaseActivity
import sdk.chat.ui.utils.ToastHelper

class TaskActivity : BaseActivity(), View.OnClickListener {
    private lateinit var pieContainer: ViewGroup
    private lateinit var taskContainer: TaskList

    //    private lateinit var taskDetail: TaskDetail
//    private lateinit var taskProcess: TaskProgress
    private lateinit var taskPieAdapter: TaskPieAdapter
    private lateinit var storyContainer: View
    private lateinit var tvStory: WebView

    //    private lateinit var tvStoryTitle: TextView
    private lateinit var tvStoryName: TextView

    //    private lateinit var imTaskImage: ImageView
    private lateinit var viewPager: ViewPager2
    private lateinit var storyAdapter: StoryCoverAdapter
    private lateinit var loading: View

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
        loading = findViewById<View>(R.id.loading)
        loading.visibility = View.GONE
//        imTaskImage = findViewById<ImageView>(R.id.fullscreenImageView)
        storyContainer = findViewById<LinearLayout>(R.id.storyContainer)
        tvStory = findViewById<WebView>(R.id.story)
        configureWebView()
//        tvStoryTitle = findViewById<TextView>(R.id.storyTitle)
        tvStoryName = findViewById<TextView>(R.id.storyName)
        findViewById<View>(R.id.calendar_enter).setOnClickListener(this)
        taskPieAdapter = TaskPieAdapter(
            pieContainer, this@TaskActivity,
            onItemClick = { i ->
                var story = storyAdapter.getItemAt(viewPager.currentItem)
                if (story != null && story.taskProcess != null) {
                    var taskDetail = story.taskProcess.taskDetail
                    taskContainer.setTaskData(taskDetail.index - i, taskDetail)
                    setStoryData(story.taskProcess, i)
                } else {
                    ToastHelper.show(this, "i: $i is null")
                }
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
                        null,
                        AIExplore.ExploreItem.action_daily_gw_pray,
                        date
                    ).subscribe();
                    finish()
                } else {
                    ChatSDK.events().source()
                        .accept(NetworkEvent.messageInputPrompt("", ""))
                    finish()
                }
            }
//            DailyTaskHandler.setTaskDetail(taskDetail)
        }
        setAdapter()
    }

    private fun setAdapter() {
        // 绑定 TabLayout 指示器
        viewPager = findViewById(R.id.viewPager)
        storyAdapter = StoryCoverAdapter()
        viewPager.adapter = storyAdapter

//        viewPager.setPageTransformer { page, position ->
//            page.translationX = -position * page.width // 关键：取负值实现反向
//        }
        // 预加载相邻页面（优化性能）
        viewPager.offscreenPageLimit = 1

// 3. 监听页面滑动，预加载下一页图片
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                var story = storyAdapter.getItemAt(position)
                if (story != null) {
                    if (story.taskProcess == null) {
                        viewPager.isUserInputEnabled = false
                        loadStoryDetail(story)
                    } else {
                        Log.e("taskPie", "setData2 and removeAllViews")
                        pieContainer.post({
                            initTaskPie(story.taskProcess)
                        })
                    }
                }
                Log.e("onPageSelected", position.toString());
                if (position < storyAdapter.itemCount - 1 && position > 0) {
                    lifecycleScope.launch {
                        Glide.with(this@TaskActivity)
                            .downloadOnly()
                            .load(storyAdapter.getItemAt(position - 2)?.progressImage) // 预加载下一页
                            .preload()
                    }
                }
            }
        })
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

    private fun loadStoryDetail(story: Story) {
        dm.add(
            DailyTaskHandler.getStoryDetail(story.id)
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        viewPager.isUserInputEnabled = true
                        if (data != null) {
                            story.taskProcess = data
                            pieContainer.post({
                                initTaskPie(data)
                            })
                        } else {
                            throw IllegalArgumentException("获取数据失败")
                        }
                    },
                    Consumer { error: Throwable? ->
                        viewPager.isUserInputEnabled = true
                        ToastHelper.show(this@TaskActivity, error?.message)
                        if (error != null) {
                            FirebaseCrashlytics.getInstance().recordException(error);
                        }
                    }
                )
        )
    }

    private fun loadData() {
        dm.add(
            DailyTaskHandler.getTaskProgress()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
//                            taskProcess = data
//                            taskDetail = taskProcess.taskDetail

                            var thisStory =
                                Story("0", data.storyName, data.progressImage)
                            thisStory.taskProcess = data
                            data.collections.add(thisStory)
                            storyAdapter.replaceData(data.collections)
                            viewPager.setCurrentItem(storyAdapter.itemCount - 1, false)
//                            initTaskPie(data)
                        } else {
                            throw IllegalArgumentException("获取数据失败")
                        }
                    },
                    Consumer { error: Throwable? ->
                        ToastHelper.show(this@TaskActivity, error?.message)
                        if (error != null) {
                            FirebaseCrashlytics.getInstance().recordException(error);
                        }
                    }
                )
        )
    }


    private fun initTaskPie(taskProgressSelected: TaskProgress) {
        tvStoryName.text = getString(R.string.grace_journey, taskProgressSelected.storyName)
        taskPieAdapter.setData(taskProgressSelected.taskDetail)
//        taskContainer.setTaskData(0, taskProgressSelected.taskDetail)
//        setStoryData(taskProgressSelected, taskProgressSelected.taskDetail.index)
//        setStoryData(taskProgressSelected, 0)
        if (taskProgressSelected.unlocked == (taskProgressSelected.total)) {
            taskPieAdapter.setSelectIndex(0)
        } else {
            taskPieAdapter.setSelectIndex(taskProgressSelected.taskDetail.index)
        }

    }

    private fun setStoryData(taskProgressSelected: TaskProgress, index: Int) {
        var taskDetail = taskProgressSelected.taskDetail
        if (index > taskDetail.index) {
            storyContainer.visibility = View.GONE
        } else if (index == taskDetail.index && !taskDetail.isAllCompleted) {
            storyContainer.visibility = View.GONE
        } else if (taskProgressSelected.chapters.size > index) {
            storyContainer.visibility = View.VISIBLE
            tvStory.loadDataWithBaseURL(
                null,
                taskProgressSelected.chapters[index].content,
                "text/html",
                "UTF-8",
                null
            );

        }
    }


    private fun configureWebView() {
        val settings: WebSettings = tvStory.getSettings()
        settings.setJavaScriptEnabled(true)
        settings.setDomStorageEnabled(true)
        settings.setLoadWithOverviewMode(true)
        settings.setUseWideViewPort(true)

        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE)

//        tvStory.setWebViewClient(object : WebViewClient() {
//            public override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                progressBar.setVisibility(View.VISIBLE)
//            }
//
//            public override fun onPageFinished(view: WebView?, url: String?) {
//                progressBar.setVisibility(View.GONE)
//            }
//        })

//        tvStory.addJavascriptInterface(WebAppInterface(this), "Android")
    }
}