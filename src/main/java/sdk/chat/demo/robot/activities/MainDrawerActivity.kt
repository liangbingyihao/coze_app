package sdk.chat.demo.robot.activities

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import materialsearchview.MaterialSearchView
import sdk.chat.core.dao.Thread
import sdk.chat.core.interfaces.ThreadType
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.HistoryAdapter
import sdk.chat.demo.robot.adpter.HistoryItem
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.fragments.CozeChatFragment
import sdk.chat.demo.robot.ui.KeyboardDrawerHelper
import sdk.chat.ui.ChatSDKUI
import sdk.chat.ui.activities.MainActivity
import kotlin.math.min

class MainDrawerActivity : MainActivity(), CozeChatFragment.DataCallback {
    //    open lateinit var slider: MaterialDrawerSliderView
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var sessions:List<Thread>
    private lateinit var sessionAdapter : HistoryAdapter
//    private lateinit var navView: NavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        drawerLayout = findViewById(R.id.root)
        searchView = findViewById(R.id.searchView)

        KeyboardDrawerHelper.setup(drawerLayout)

        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        createSessionMenu()

        initViews()

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, CozeChatFragment(),"tag_chat")
            .commit()


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


    }

    private fun createSessionMenu() {
        sessions = ChatSDK.thread().getThreads(ThreadType.Private)
        val sessionMenus: ArrayList<HistoryItem> = ArrayList<HistoryItem>()
        var lastTime: String? = null
        for (i in 0 until min(sessions.size,50)) {
            var session = sessions[i]
            var thisTime = DateLocalizationUtil.getFriendlyDate(this@MainDrawerActivity,session.creationDate)
            if(thisTime!=lastTime){
                lastTime = thisTime
                sessionMenus.add(HistoryItem.DateItem(lastTime))
            }
            sessionMenus.add(HistoryItem.SessionItem(if (session.messages.isNotEmpty()) session.messages[0].text else "新会话",
                session.id.toString()))
        }
        sessionAdapter = HistoryAdapter(sessionMenus) { clickedItem ->
            // 打开文章详情等操作
            Toast.makeText(this, clickedItem.sessionId, Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = sessionAdapter
    }

//    private fun createMenuAdapter(): RecyclerView.Adapter<*> {
//        val historyItems = listOf(
//            HistoryItem.DateItem("今天"),
//            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
//            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
//            HistoryItem.DateItem("昨天"),
//            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
//            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
//            HistoryItem.DateItem("今天"),
//            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
//            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
//            HistoryItem.DateItem("昨天"),
//            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
//            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
//            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
//            HistoryItem.DateItem("今天"),
//            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
//            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
//            HistoryItem.DateItem("昨天"),
//            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
//        )
//
//        return HistoryAdapter(historyItems) { clickedItem ->
//            // 打开文章详情等操作
//            Toast.makeText(this, clickedItem.sessionId, Toast.LENGTH_SHORT).show()
//        }
//    }


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
            // 1. 获取当前焦点视图
            val view = currentFocus
            // 2. 如果有焦点视图且输入法开启
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                // 3. 延迟打开抽屉确保键盘完全收起
                drawerLayout.postDelayed({
                    drawerLayout.openDrawer(GravityCompat.START)
                }, 100) // 100ms延迟足够键盘动画完成
            } else {
                // 4. 直接打开抽屉
                drawerLayout.openDrawer(GravityCompat.START)
            }
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

    override fun getCurrentData(): Thread? {
        var session:HistoryItem.SessionItem? = sessionAdapter.getSelectItem()
        return if(session!=null){
            sessions.firstOrNull { session.sessionId == it.id.toString() }
        }else{
            null
        }
    }

    override fun onDataUpdated(thread: Thread?) {
        TODO("Not yet implemented")
    }
}