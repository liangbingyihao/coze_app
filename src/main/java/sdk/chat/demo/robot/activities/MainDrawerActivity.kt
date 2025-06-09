package sdk.chat.demo.robot.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import materialsearchview.MaterialSearchView
import sdk.chat.core.dao.Thread
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.HistoryAdapter
import sdk.chat.demo.robot.adpter.HistoryItem
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.ChatSDKUI
import sdk.chat.ui.activities.MainActivity
import sdk.guru.common.RX
import kotlin.math.min


class MainDrawerActivity : MainActivity(), GWChatFragment.DataCallback, View.OnClickListener {
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var sessions: List<Thread>
    private var currentSession: Thread? = null
    private lateinit var sessionAdapter: HistoryAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var pickImageLauncher: ActivityResultLauncher<Intent?>? = null

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

        drawerLayout = findViewById(R.id.root)
//        searchView = findViewById(R.id.btn_search)
        findViewById<View>(R.id.menu_favorites).setOnClickListener(this)
        findViewById<View>(R.id.settings).setOnClickListener(this)

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
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
//        initViews()

        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        createSessionMenu()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()


        // Handle Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        supportActionBar!!.setHomeAsUpIndicator(
            ChatSDKUI.icons().get(
                this,
                ChatSDKUI.icons().drawer,
                ChatSDKUI.icons().actionBarIconColor
            )
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.ThreadsUpdated)).subscribe(Consumer {
                    createSessionMenu()
                    // Refresh the read count

//            dm.add(privateTabName().subscribe(Consumer {
//                slider.updateName(privateThreadItem.identifier, it)
//            }))
                })
        )

        requestPermissions();

        // 注册从相册选择图片的launcher
        pickImageLauncher = registerForActivityResult(
            StartActivityForResult(),
            { result ->
                if (result.getResultCode() === RESULT_OK) {
                    val data: Intent? = result.getData()
                    if (data != null) {
                        val imageUri: Uri? = data.getData()
                        // 处理选择的图片
                        if (imageUri != null) {
                            // 或者分享图片
                            val shareIntent = Intent(Intent.ACTION_SEND)


                            // 设置分享类型为图片和文本
                            shareIntent.setType("image/*")


                            // 添加图片URI
                            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)


                            // 添加文本内容（包含下载链接）
                            val shareText =
                                "看看这个有趣的图片！下载我们的应用: " + "https://play.google.com/store/apps/details?id=com.color.colorvpn&hl=en"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)


                            // 创建选择器，避免直接显示Intent.createChooser的标题
                            val chooserIntent = Intent.createChooser(shareIntent, "分享到")


                            // 验证是否有应用可以处理此Intent
                            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(chooserIntent)
                            } else {
                                Toast.makeText(this, "没有找到可用的分享应用", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            })
    }

    private fun toMenuItems(data: List<Thread>): ArrayList<HistoryItem> {
        sessions = data
        val sessionMenus: ArrayList<HistoryItem> = ArrayList<HistoryItem>()
        var lastTime: String? = null
        toReloadSessions = false
        for (i in 0 until min(sessions.size, 50)) {
            var session = sessions[i]
//            var thisTime =
//                DateLocalizationUtil.getFriendlyDate(this@MainDrawerActivity, session.creationDate)
//            if (thisTime != lastTime) {
//                lastTime = thisTime
//                sessionMenus.add(HistoryItem.DateItem(lastTime))
//            }
            var name = when {
                session.name.isNotEmpty() -> session.name
                session.messages.isNotEmpty() -> session.messages[0].text
                else -> "新会话"
            }
//            name = session.entityID + "," + name + "," + session.type.toString();
            if (!toReloadSessions && "新会话" == name) {
                toReloadSessions = true
            }
            sessionMenus.add(HistoryItem.SessionItem(name, session.entityID))
        }
        return sessionMenus
    }

    private fun createSessionMenu() {
        dm.add(
            threadHandler.listOrNewSessions()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
                            val sessionMenus: ArrayList<HistoryItem> = toMenuItems(data)
                            sessionAdapter = HistoryAdapter(sessionMenus) { changed, clickedItem ->
                                toggleDrawer()
//                                if (changed) {
//                                    setCurrentSession(clickedItem)
                                    ArticleListActivity.start(
                                        this@MainDrawerActivity,
                                        clickedItem.sessionId
                                    )
//                                }
                            }
                            recyclerView.adapter = sessionAdapter
//                            setCurrentSession(sessionAdapter.getSelectItem())
                        } else {
                            throw IllegalArgumentException("创建会话失败")
                        }
                    },
                    this
                )
        )

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
                val intent = Intent(Intent.ACTION_PICK)
                intent.setType("image/*")
                pickImageLauncher!!.launch(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

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
        if(threadHandler.isCustomPrompt){
            toolbar?.title = "自定义提示语中"
        }else{
            toolbar?.title = getString(R.string.app_name)
        }
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

    private fun setCurrentSession(selected: HistoryItem.SessionItem?) {
        currentSession = if (selected != null) {
            sessions.firstOrNull { selected.sessionId == it.id.toString() }
        } else {
            null
        }
        if (currentSession != null) {
            val fragment: GWChatFragment? =
                supportFragmentManager.findFragmentByTag(chatTag) as? GWChatFragment;
            if (fragment == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()
            } else {
//                fragment.onNewIntent(currentSession);
            }
            getToolbar()?.title = currentSession!!.name
        }
    }

    override fun getCurrentData(): Thread? {
        return currentSession
    }

    override fun onClick(v: View?) {
        toggleDrawer()
        when (v?.id) {
//            R.id.btn_new_chat -> {
//                dm.add(
//                    threadHandler.newAndListSessions()
//                        .observeOn(RX.main())
//                        .subscribe(Consumer { sessions: List<Thread>? ->
//                            if (sessions != null) {
//                                val sessionMenus: ArrayList<HistoryItem> = toMenuItems(sessions)
//                                sessionAdapter.updateAll(sessionMenus)
//                                setCurrentSession(sessionAdapter.getSelectItem())
//                            } else {
//                                throw IllegalArgumentException("创建失败")
//                            }
//                        }, this@MainDrawerActivity)
//                )
//            }

            R.id.menu_favorites -> {
                Toast.makeText(this, "menu_favorites", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        CollectionListActivity::class.java
                    )
                )
            }


            R.id.settings -> {
//                ChatSDK.ui().startProfileActivity(this@MainDrawerActivity, ChatSDK.currentUserID())
                ChatSDK.auth().logout().observeOn(RX.main()).doOnComplete(Action {
                    ChatSDK.ui().startSplashScreenActivity(this@MainDrawerActivity)
                }).subscribe(this@MainDrawerActivity)
            }
        }
    }

}