package sdk.chat.demo.robot.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import materialsearchview.MaterialSearchView
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.SessionAdapter
import sdk.chat.demo.robot.audio.AsrHelper
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.CustomDivider
import sdk.chat.demo.robot.ui.HighlightOverlayView
import sdk.chat.demo.robot.ui.hasShownGuideOverlay
import sdk.chat.demo.robot.ui.listener.GWClickListener
import sdk.chat.ui.activities.MainActivity
import sdk.guru.common.RX
import java.util.concurrent.TimeUnit


class MainDrawerActivity : MainActivity(), View.OnClickListener, GWClickListener.TTSSpeaker {
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var vHomeMenu: View
    private lateinit var vTaskMenu: View
    private lateinit var vRedDotTask: View
    private lateinit var vDgwMenu: TextView
    private lateinit var vErrorHint: TextView

    //    private lateinit var sessions: List<Thread>
    private var highlightOverlay: HighlightOverlayView? = null
    private lateinit var sessionAdapter: SessionAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var hasShownGuide = false
//    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 加载菜单资源
        menuInflater.inflate(R.menu.nav_menu, menu)
//        IconicsMenuInflaterUtil.inflate(
//            menuInflater,
//            this,
//            R.menu.menu_main,
//            menu,
//            true
//        )
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

//        val daoCore: DaoCore = ChatSDK.db().getDaoCore()
//        if (daoCore==null||daoCore.getDaoSession() == null) {
//            LogHelper.reportExportEvent("app.init", "getDaoSession == null: "+LogHelper.logStr, null)
//        }


        var isInitialized = (application as MainApp).isInitialized
        Logger.error{"MainDrawerActivity.onCreate,isInitialized:${isInitialized}"}
        drawerLayout = findViewById(R.id.root_container)
        highlightOverlay = findViewById(R.id.overlay)
        findViewById<View>(R.id.menu_favorites).setOnClickListener(this)
        vDgwMenu = findViewById<TextView>(R.id.menu_gw_daily)
        vDgwMenu.setOnClickListener(this)
        findViewById<View>(R.id.menu_search).setOnClickListener(this)
        findViewById<View>(R.id.menu_setting).setOnClickListener(this)
        vErrorHint = findViewById<View>(R.id.error_hint) as TextView
        vHomeMenu = findViewById<View>(R.id.menu_home)
        vHomeMenu.setOnClickListener(this)
        vTaskMenu = findViewById<View>(R.id.menu_task)
        vTaskMenu.setOnClickListener(this)
        vRedDotTask = findViewById<View>(R.id.red_dot2)

//        KeyboardDrawerHelper.setup(drawerLayout)
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset > 0.3) {
                    hideKeyboard()
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                if (toReloadSessions) {
                    threadHandler.triggerNetworkSync()
                }
                recyclerView.scrollToPosition(0);

            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
//        initViews()

        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        listSessions()


        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.ThreadsUpdated)).subscribe(Consumer {
                    listSessions()
                })
        )

        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.HideDrawer)).subscribe(Consumer {
                    drawerLayout.closeDrawers()
                })
        )

        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.NetworkStateChanged))
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
//                    ToastHelper.show(
//                        this@MainDrawerActivity,
//                        "networkEvent:${networkEvent?.isOnline}"
//                    )
                    if (networkEvent != null) {
                        if (!networkEvent.isOnline) {
                            vErrorHint.visibility = View.VISIBLE
                            vErrorHint.setText(R.string.network_error)
                        } else {
                            vErrorHint.visibility = View.GONE
                        }
                    }

                })
        )

        //        dm.add(ChatSDK.events().sourceOnMain()
//                .filter(NetworkEvent.filterRoleUpdated(thread, ChatSDK.currentUser()))
//                .subscribe(networkEvent -> {
//                    showOrHideTextInputView();
//                }));

        if (!hasShownGuideOverlay(this@MainDrawerActivity)) {
            dm.add(
                ChatSDK.events().prioritySourceOnSingle()
                    .filter(
                        NetworkEvent.filterType(
                            EventType.MessageUpdated
                        )
                    )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { networkEvent: NetworkEvent? ->
                        highlightOverlay?.handleFirst(
                            this@MainDrawerActivity,
                            networkEvent?.message
                        )
                    }, this)
            )

            dm.add(
                threadHandler.welcomeMsg
                    .delay(2, TimeUnit.SECONDS)
                    .subscribeOn(RX.io())
                    .observeOn(RX.main())
                    .subscribe(
                        { data ->
                            if (data != null) {
                                highlightOverlay?.handleFirst(
                                    this@MainDrawerActivity,
                                    data
                                )
                            }
                        },
                        { error ->
                        }
                    )
            )
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()

        requestPermissions();
        TTSHelper.initTTS(this@MainDrawerActivity)
        AsrHelper.initAsrEngine()
        checkTaskDetail()
    }

    private fun checkTaskDetail() {
        hasShownGuide = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("has_shown_guide", false)
        if (hasShownGuide) {
            val today: String = DateLocalizationUtil.formatDayAgo(0)
            var showDate =
                getSharedPreferences("app_prefs", MODE_PRIVATE).getString("shown_gw_date", "")
            if (today != showDate) {
                ImageViewerActivity.start(this@MainDrawerActivity);
            }

            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putString("shown_gw_date", today)
                }
        } else {
            // 保存已经显示过引导页的状态
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putBoolean("has_shown_guide", true)
                }
        }


    }


//    private fun toMenuItems(data: List<Thread>): ArrayList<HistoryItem> {
//        sessions = data
//        val sessionMenus: ArrayList<HistoryItem> = ArrayList<HistoryItem>()
//        var lastTime: String? = null
//        toReloadSessions = false
//        for (i in 0 until min(sessions.size, 100)) {
//            var session = sessions[i]
////            var thisTime =
////                DateLocalizationUtil.getFriendlyDate(this@MainDrawerActivity, session.creationDate)
////            if (thisTime != lastTime) {
////                lastTime = thisTime
////                sessionMenus.add(HistoryItem.DateItem(lastTime))
////            }
//            var name = when {
//                session.name.isNotEmpty() -> session.name
//                session.messages.isNotEmpty() -> session.messages[0].text
//                else -> "新会话"
//            }
////            name = session.entityID + "," + name + "," + session.type.toString();
//            if (!toReloadSessions && "新会话" == name) {
//                toReloadSessions = true
//            }
//            sessionMenus.add(HistoryItem.SessionItem(name, session.entityID))
//        }
//        return sessionMenus
//    }

    private fun listSessions() {
        dm.add(
            threadHandler.listSessions()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
//                            val sessionMenus: ArrayList<HistoryItem> = toMenuItems(data)
                            sessionAdapter = SessionAdapter(data, { changed, clickedItem ->
//                                toggleDrawer()
//                                if (changed) {
//                                    setCurrentSession(clickedItem)
                                ArticleListActivity.start(
                                    this@MainDrawerActivity,
                                    clickedItem.id
                                )
                            }, { clickedItem ->
//                                var item: ArticleSession? = sessionAdapter.getSelectItem()
//                                if (item != null && !dialogEditSingle.isShowing) {
//                                    dialogEditSingle.show()
//                                    dialogEditSingle.setEditDefault(item.title)
//                                }
                            })
                            recyclerView.adapter = sessionAdapter
                            if (recyclerView.itemDecorationCount == 0) {
                                recyclerView.addItemDecoration(
                                    CustomDivider(
                                        thickness = 1.dpToPx(this@MainDrawerActivity),  // 扩展函数转换 dp 到 px
                                        colorResId = R.color.gray_divider,
                                        insetStart = 12.dpToPx(this@MainDrawerActivity),
                                        insetEnd = 12.dpToPx(this@MainDrawerActivity)
                                    )
                                )
                            }
                        } else {
                            throw IllegalArgumentException("创建会话失败")
                        }
                    },
                    this
                )
        )

    }


    override fun speek(text: String, msgId: String) {
        TTSHelper.speek(text, msgId)
    }

    override fun getCurrentUtteranceId(): String? {
        return currentUtteranceId;
    }

    override fun stop() {
        TTSHelper.stop()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                toggleDrawer()
                true
            }

            R.id.action_record -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SpeechToTextActivity::class.java
                    )
                )
                true
            }

            R.id.action_prompt -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SettingPromptActivity::class.java
                    )
                )
                true
            }

            R.id.action_share -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        TaskActivity::class.java
                    )
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

//    private val dialogEditSingle by lazy {
//        DialogEditSingle(this) { inputText ->
//            // 处理发送逻辑
//            var item: HistoryItem.SessionItem? = sessionAdapter.getSelectItem()
//            if (item != null) {
//                Toast.makeText(this, "${item.title}: $inputText", Toast.LENGTH_SHORT).show()
//                dm.add(
//                    threadHandler.setSessionName(item.sessionId.toLong(), inputText)
//                        .observeOn(RX.main()).subscribe(
//                            { result ->
//                                if (!result) {
//                                    Toast.makeText(
//                                        this@MainDrawerActivity,
//                                        getString(R.string.failed_and_retry),
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                            },
//                            { error -> // onError
//                                Toast.makeText(
//                                    this@MainDrawerActivity,
//                                    "${getString(R.string.failed_and_retry)} ${error.message}",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            })
//                )
//            }
//        }
//    }

    fun toggleDrawer() {
        if (drawerLayout.isOpen) {
            drawerLayout.closeDrawers()
        } else {
            hideKeyboard()
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }


    override fun onResume() {
        super.onResume()
        if (!ChatSDK.connectionStateMonitor().isOnline()) {
            vErrorHint.visibility = View.VISIBLE
            vErrorHint.setText(R.string.network_error)
        } else {
            vErrorHint.visibility = View.GONE
        }
        if (threadHandler.isCustomPrompt) {
            toolbar?.title = "自定义提示语中"
        } else {
            toolbar?.title = getString(R.string.app_name)
        }
        setRedDotView()
        threadHandler.reloadTimeoutMsg()
    }

    override fun getLayout(): Int {
        return R.layout.activity_main_coze_drawer;
    }

    override fun searchEnabled(): Boolean {
        return false
    }

    override fun search(text: String?) {

    }

    override fun searchView(): MaterialSearchView {
        return searchView
    }

    override fun reloadData() {

    }

    override fun clearData() {

    }

    override fun updateLocalNotificationsForTab() {

    }

    // 显示引导层
    fun showTutorialOverlay(targetView: View) {
//        highlightOverlay?.setHighlightMode(this@MainDrawerActivity,guideDrawer)
//        val rootView = findViewById<ViewGroup>(R.id.content_container) // 获取根布局
//        val overlay = HighlightOverlayView(this).apply {
//            layoutParams = FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//        }
//        var r:RecyclerView = findViewById<View>(R.id.chatView).findViewById<RecyclerView>(R.id.recyclerview)
//        var m: View? = findTopmostVisibleViewByResId(r,R.id.btn_pic)
//        if(m!=null){
//            highlightOverlay?.setHighlightMode(this@MainDrawerActivity,guideDrawer) // 设置高亮区域
//        }
//        rootView.addView(overlay) // 添加到根布局
    }

    override fun onClick(v: View?) {
        if (v?.id != R.id.menu_task) {
            toggleDrawer()
        }
        when (v?.id) {
            R.id.menu_home -> {
                true
            }

            R.id.menu_search -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SearchActivity::class.java
                    )
                )
            }

            R.id.menu_favorites -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        FavoriteListActivity::class.java
                    )
                )
            }

            R.id.menu_gw_daily -> {
                hasShownGuide = true
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        ImageViewerActivity::class.java
                    )
                )
            }

            R.id.menu_setting -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SettingsActivity::class.java
                    )
                )
            }


            R.id.menu_task -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        TaskActivity::class.java
                    )
                )
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TTSHelper.clear()
    }

    fun setRedDotView() {
        setGwdRedDotView()
        setTaskRedDotView()
//
//        val redDot2: View = findViewById<View>(R.id.red_dot2)
//        vTaskMenu.post({
//            // 获取菜单图标的宽高
//            val menuWidth: Int = vTaskMenu.width
//
//            // 创建布局参数
//            val params: FrameLayout.LayoutParams =
//                redDot2.layoutParams as FrameLayout.LayoutParams
//
//
//            // 设置红点位置（菜单图标右上角）
//            params.gravity = Gravity.START or Gravity.TOP
//            params.leftMargin =
//                vTaskMenu.left + menuWidth - vTaskMenu.paddingRight - redDot2.width / 2
//            params.topMargin = vTaskMenu.top + vTaskMenu.paddingTop - redDot2.height / 2
//            redDot2.setLayoutParams(params)
//        })


    }

    fun setTaskRedDotView() {

        dm.add(
            DailyTaskHandler.getTaskProgress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null && !data.taskDetail.isAllUserTaskCompleted) {
                            // 获取菜单图标的宽高
                            val menuWidth: Int = vTaskMenu.width

                            // 创建布局参数
                            val params: FrameLayout.LayoutParams =
                                vRedDotTask.layoutParams as FrameLayout.LayoutParams


                            // 设置红点位置（菜单图标右上角）
                            params.gravity = Gravity.START or Gravity.TOP
                            params.leftMargin =
                                vTaskMenu.left + menuWidth - vTaskMenu.paddingRight - vRedDotTask.width / 2
                            params.topMargin =
                                vTaskMenu.top + vTaskMenu.paddingTop - vRedDotTask.height / 2
                            vRedDotTask.setLayoutParams(params)
                        } else {
                            vRedDotTask.visibility = View.GONE
                        }
                    },
                    Consumer { error: Throwable? ->
                        vRedDotTask.visibility = View.GONE
                    }
                )
        )
    }

    fun setGwdRedDotView() {
        // 获取红点视图
        val redDot: View = findViewById<View>(R.id.red_dot)
        val redDot3: View = findViewById<View>(R.id.red_dot3)
        if (!hasShownGuide) {
            vDgwMenu.post({
                val drawables: Array<Drawable?> = vDgwMenu.getCompoundDrawables()
                val leftDrawable: Drawable? = drawables[0]
                if (leftDrawable != null) {

                    // 创建布局参数
                    val params: FrameLayout.LayoutParams =
                        redDot3.layoutParams as FrameLayout.LayoutParams


                    // 设置红点位置（菜单图标右上角）
                    params.gravity = Gravity.START or Gravity.TOP
                    params.leftMargin =
                        vDgwMenu.left + vDgwMenu.paddingLeft + leftDrawable.intrinsicWidth - redDot3.width / 2
                    params.topMargin = vDgwMenu.top + vDgwMenu.paddingTop - redDot3.height / 2
                    redDot3.setLayoutParams(params)
                }

            })
// 在视图布局完成后调整位置
            vHomeMenu.post({
                // 获取菜单图标的宽高
                val menuWidth: Int = vHomeMenu.width


                // 创建布局参数
                val params: FrameLayout.LayoutParams =
                    redDot.layoutParams as FrameLayout.LayoutParams


                // 设置红点位置（菜单图标右上角）
                params.gravity = Gravity.START or Gravity.TOP
                params.leftMargin =
                    vHomeMenu.left + menuWidth - vHomeMenu.paddingRight - redDot.width / 2
                params.topMargin = vHomeMenu.top + vHomeMenu.paddingTop - redDot.height / 2
                redDot.setLayoutParams(params)
            })

        } else {
            redDot.visibility = View.GONE
            redDot3.visibility = View.GONE
        }
    }
}