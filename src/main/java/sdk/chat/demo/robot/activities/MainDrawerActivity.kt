package sdk.chat.demo.robot.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.HistoryAdapter
import sdk.chat.demo.robot.adpter.HistoryItem
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.fragments.CozeChatFragment
import sdk.chat.demo.robot.handlers.CozeThreadHandler
import sdk.chat.demo.robot.ui.KeyboardDrawerHelper
import sdk.chat.ui.ChatSDKUI
import sdk.chat.ui.activities.MainActivity
import sdk.guru.common.RX
import kotlin.math.min


class MainDrawerActivity : MainActivity(), CozeChatFragment.DataCallback, View.OnClickListener {
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var sessions:List<Thread>
    private var currentSession:Thread? = null
    private lateinit var sessionAdapter : HistoryAdapter
    private val threadHander:CozeThreadHandler = ChatSDK.thread() as CozeThreadHandler
    private val chatTag ="tag_chat";


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        drawerLayout = findViewById(R.id.root)
        searchView = findViewById(R.id.searchView)
        findViewById<View>(R.id.btn_new_chat).setOnClickListener(this)
        findViewById<View>(R.id.menu_favorites).setOnClickListener(this)
        findViewById<View>(R.id.menu_reports).setOnClickListener(this)
        findViewById<View>(R.id.settings).setOnClickListener(this)

        KeyboardDrawerHelper.setup(drawerLayout)
        initViews()


        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        createSessionMenu()


//        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
//            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
//                Glide.with(this@MainDrawerActivity)
//                    .load(uri)
//                    .dontAnimate()
//                    .placeholder(placeholder)
//                    .into(imageView)
//            }
//
//            override fun cancel(imageView: ImageView) {
//                Glide.with(this@MainDrawerActivity).clear(imageView)
//            }
//        })

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

//        dm.add(
//            ChatSDK.events().sourceOnSingle()
//                .filter(NetworkEvent.filterType(EventType.ThreadsUpdated))
//                .subscribe(Consumer { networkEvent: NetworkEvent? ->
////                    loadData()
//                })
//        )
    }

    private fun toMenuItems(data: List<Thread>): ArrayList<HistoryItem> {
        sessions = data
        val sessionMenus: ArrayList<HistoryItem> = ArrayList<HistoryItem>()
        var lastTime: String? = null
        for (i in 0 until min(sessions.size,50)) {
            var session = sessions[i]
            var thisTime = DateLocalizationUtil.getFriendlyDate(this@MainDrawerActivity,session.creationDate)
            if(thisTime!=lastTime){
                lastTime = thisTime
                sessionMenus.add(HistoryItem.DateItem(lastTime))
            }
            var name = when {
                session.name.isNotEmpty() -> session.name
                session.messages.isNotEmpty() -> session.messages[0].text
                else -> "新会话"
            }
            sessionMenus.add(HistoryItem.SessionItem(name,session.id.toString()))
        }
        return sessionMenus
    }

    private fun createSessionMenu() {
        dm.add(
            threadHander.listOrNewSessions()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
                            val sessionMenus: ArrayList<HistoryItem> = toMenuItems(data)
                            sessionAdapter = HistoryAdapter(sessionMenus) { changed,clickedItem ->
                                toggleDrawer()
                                if(changed){
                                    setCurrentSession(clickedItem)
                                }
                            }
                            recyclerView.adapter = sessionAdapter
                            setCurrentSession(sessionAdapter.getSelectItem())
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

    private fun setCurrentSession(selected:HistoryItem.SessionItem?){
        currentSession = if(selected!=null){
            sessions.firstOrNull { selected.sessionId == it.id.toString() }
        }else{
            null
        }
        if(currentSession!=null){
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, CozeChatFragment(),chatTag).commit()
        }
    }

    override fun getCurrentData(): Thread? {
        return currentSession
    }

    override fun onClick(v: View?) {
        toggleDrawer()
        when (v?.id) {
            R.id.btn_new_chat -> {
                dm.add(
                    threadHander.newAndListSessions()
                        .observeOn(RX.main())
                        .subscribe(Consumer { sessions: List<Thread>? ->
                            if(sessions!=null){
                                val sessionMenus: ArrayList<HistoryItem> = toMenuItems(sessions)
                                sessionAdapter.updateAll(sessionMenus)
                                setCurrentSession(sessionAdapter.getSelectItem())
                            }else{
                                throw IllegalArgumentException("创建失败")
                            }
                        }, this@MainDrawerActivity)
                )
            }
            R.id.menu_favorites -> { Toast.makeText(this, "menu_favorites", Toast.LENGTH_SHORT).show() }
            R.id.menu_reports -> { Toast.makeText(this, "menu_reports", Toast.LENGTH_SHORT).show() }
            R.id.settings -> {
//                ChatSDK.ui().startProfileActivity(this@MainDrawerActivity, ChatSDK.currentUserID())
                ChatSDK.auth().logout().observeOn(RX.main()).doOnComplete(Action { ChatSDK.ui().startSplashScreenActivity(this@MainDrawerActivity) }).subscribe(this@MainDrawerActivity)
            }
        }
    }
}