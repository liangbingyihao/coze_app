package sdk.chat.demo.robot.activities

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import sdk.chat.demo.robot.adpter.SessionAdapter
import sdk.chat.demo.robot.adpter.HistoryItem
import sdk.chat.demo.robot.dialog.DialogEditSingle
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.CustomDivider
import sdk.chat.demo.robot.ui.listener.GWClickListener
import sdk.chat.ui.activities.MainActivity
import sdk.guru.common.RX
import java.util.Locale
import kotlin.math.min


class MainDrawerActivity : MainActivity(), View.OnClickListener, GWClickListener.TTSSpeaker {
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var sessions: List<Thread>
    private var currentSession: Thread? = null
    private lateinit var sessionAdapter: SessionAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var textToSpeech: TextToSpeech? = null
    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>

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
        findViewById<View>(R.id.menu_gw_daily).setOnClickListener(this)
        findViewById<View>(R.id.menu_search).setOnClickListener(this)
        findViewById<View>(R.id.debug).setOnClickListener(this)
        findViewById<View>(R.id.menu_home).setOnClickListener(this)
        findViewById<View>(R.id.menu_task).setOnClickListener(this)

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
        createSessionMenu()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()


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

        // 检查 TTS 是否可用
        // 注册 ActivityResultLauncher
        ttsCheckLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    initTTS() // TTS 可用，初始化
                } else {
                    // 提示用户安装 TTS 数据
                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    startActivity(installIntent)
                }
            }

        // 检查 TTS 数据
        val checkIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
//        ttsCheckLauncher.launch(checkIntent)


        if (checkIntent.resolveActivity(packageManager) != null) {
            // 确认有 TTS 引擎后再启动
            ttsCheckLauncher.launch(checkIntent)
        } else {
            // 设备完全无 TTS 支持时的处理
//            handleNoTtsEngine()
            Toast.makeText(this, "暂不支持语音播放", Toast.LENGTH_SHORT).show()

//            AlertDialog.Builder(this)
//                .setTitle("需要语音支持")
//                .setMessage("您的设备缺少语音合成引擎，是否安装 Google TTS？")
//                .setPositiveButton("安装") { _, _ ->
//                    safeInstallTtsEngine()
//                }
//                .setNegativeButton("取消") { _, _ ->
//                    Toast.makeText(this, "部分功能将不可用", Toast.LENGTH_SHORT).show()
//                }
//                .show()
        }
    }

    private fun safeInstallTtsEngine() {
        val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 再次检查安装 Intent 是否可用
        if (installIntent.resolveActivity(packageManager) != null) {
            startActivity(installIntent)
        } else {
            // 连安装入口都没有的极端情况（如国产 ROM）
            Toast.makeText(
                this,
                "您的设备不支持语音功能",
                Toast.LENGTH_LONG
            ).show()
        }
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
                            sessionAdapter = SessionAdapter(sessionMenus, { changed, clickedItem ->
//                                toggleDrawer()
//                                if (changed) {
//                                    setCurrentSession(clickedItem)
                                ArticleListActivity.start(
                                    this@MainDrawerActivity,
                                    clickedItem.sessionId
                                )
                            }, { clickedItem ->
                                var item: HistoryItem.SessionItem? = sessionAdapter.getSelectItem()
                                if (item != null && !dialogEditSingle.isShowing) {
                                    dialogEditSingle.show()
                                    dialogEditSingle.setEditDefault(item.title)
                                }
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

    private fun initTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
                } else {
//                    speek("你好, Android TTS")
//                    textToSpeech.speak("Hello, Android TTS", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Toast.makeText(this@MainDrawerActivity, "onStart", Toast.LENGTH_SHORT).show()
            }

            override fun onDone(utteranceId: String?) {

                //通知老的播放按键恢复一下
                threadHandler.setPlayingMsg(null);
                Toast.makeText(this@MainDrawerActivity, "onDone", Toast.LENGTH_SHORT).show()
            }

            override fun onError(utteranceId: String?) {
                threadHandler.setPlayingMsg(null);
                Toast.makeText(this@MainDrawerActivity, "playError", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun speek(text: String, msgId: String) {
        var rawText = text.replace("*", "").replace("<br>|</br>|<br/>".toRegex(), "")
        textToSpeech?.language = LanguageUtils.getTextLanguage(rawText)
        textToSpeech?.speak(rawText, TextToSpeech.QUEUE_FLUSH, null, msgId)
    }

    override fun getCurrentUtteranceId(): String? {
        return currentUtteranceId;
    }

    override fun stop() {
        if (textToSpeech != null && textToSpeech!!.isSpeaking) {
            textToSpeech!!.stop()
        }
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

    private val dialogEditSingle by lazy {
        DialogEditSingle(this) { inputText ->
            // 处理发送逻辑
            var item: HistoryItem.SessionItem? = sessionAdapter.getSelectItem()
            if (item != null) {
                Toast.makeText(this, "${item.title}: $inputText", Toast.LENGTH_SHORT).show()
                dm.add(
                    threadHandler.setSessionName(item.sessionId.toLong(), inputText)
                        .observeOn(RX.main()).subscribe(
                            { result ->
                                if (!result) {
                                    Toast.makeText(
                                        this@MainDrawerActivity,
                                        getString(R.string.failed_and_retry),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            { error -> // onError
                                Toast.makeText(
                                    this@MainDrawerActivity,
                                    "${getString(R.string.failed_and_retry)} ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                )
            }
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
        if (threadHandler.isCustomPrompt) {
            toolbar?.title = "自定义提示语中"
        } else {
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

//    private fun setCurrentSession(selected: HistoryItem.SessionItem?) {
//        currentSession = if (selected != null) {
//            sessions.firstOrNull { selected.sessionId == it.id.toString() }
//        } else {
//            null
//        }
//        if (currentSession != null) {
//            val fragment: GWChatFragment? =
//                supportFragmentManager.findFragmentByTag(chatTag) as? GWChatFragment;
//            if (fragment == null) {
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()
//            } else {
////                fragment.onNewIntent(currentSession);
//            }
//            getToolbar()?.title = currentSession!!.name
//        }
//    }


    override fun onClick(v: View?) {
        if(v?.id!=R.id.menu_task){
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
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        ImageViewerActivity::class.java
                    )
                )
            }

            R.id.debug -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SpeechToTextActivity::class.java
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
        if (textToSpeech != null) {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
    }
}