package sdk.chat.demo.robot.activities

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import materialsearchview.MaterialSearchView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.CozeChatFragment
import sdk.chat.ui.ChatSDKUI
import sdk.chat.ui.activities.MainActivity
import android.view.inputmethod.InputMethodManager
import sdk.chat.demo.robot.ui.KeyboardDrawerHelper

sealed class HistoryItem  {
    data class DateItem(val date: String) : HistoryItem()
    data class SessionItem(val title: String, val sessionId: String) : HistoryItem()
}


class MainDrawerActivity : MainActivity() {
//    open lateinit var slider: MaterialDrawerSliderView
    open lateinit var drawerLayout: DrawerLayout
    open lateinit var searchView: MaterialSearchView
    private lateinit var recyclerView: RecyclerView
//    private lateinit var navView: NavigationView


    class HistoryAdapter(private val items: List<HistoryItem>,
                         private val onItemClick: (HistoryItem.SessionItem) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var selectedPosition = -1

        companion object {
            private const val TYPE_DATE = 0
            private const val TYPE_ARTICLE = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is HistoryItem.DateItem -> TYPE_DATE
                is HistoryItem.SessionItem -> TYPE_ARTICLE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_DATE -> DateViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_history_date, parent, false)
                )
                else -> ArticleViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_history_session, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is HistoryItem.DateItem -> (holder as DateViewHolder).bind(item)
                is HistoryItem.SessionItem -> (holder as ArticleViewHolder).bind(item,position)
            }
        }

        override fun getItemCount() = items.size

        inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val dateText: TextView = view.findViewById(R.id.date_text)

            fun bind(item: HistoryItem.DateItem) {
                dateText.text = item.date
            }
        }

        inner class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val titleText: TextView = view.findViewById(R.id.title_text)

            fun bind(item: HistoryItem.SessionItem, position: Int) {
                titleText.text = item.title
                itemView.isSelected = position == selectedPosition
                itemView.setOnClickListener {
                    val clickedPosition = absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?: return@setOnClickListener
                    // 更新选中状态
                    val previous = selectedPosition
                    selectedPosition = clickedPosition

                    // 只刷新必要的项目
                    listOfNotNull(previous.takeIf { it != -1 }, clickedPosition)
                        .distinct()
                        .forEach { notifyItemChanged(it) }

                    onItemClick(item)
                }
            }
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        drawerLayout = findViewById(R.id.root)
        searchView = findViewById(R.id.searchView)

        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = createMenuAdapter()
        KeyboardDrawerHelper.setup(drawerLayout)

        initViews()

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, CozeChatFragment())
            .commit()


        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                Glide.with(this@MainDrawerActivity)
                    .load(uri)
                    .dontAnimate()
                    .placeholder(placeholder)
                    .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(this@MainDrawerActivity).clear(imageView)
            }
        })

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

    private fun createMenuAdapter(): RecyclerView.Adapter<*> {
        val historyItems = listOf(
            HistoryItem.DateItem("今天"),
            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
            HistoryItem.DateItem("昨天"),
            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
            HistoryItem.DateItem("今天"),
            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
            HistoryItem.DateItem("昨天"),
            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
            HistoryItem.SessionItem("MVVM架构解析", "https://example.com/4"),
            HistoryItem.DateItem("今天"),
            HistoryItem.SessionItem("Android开发最佳实践", "https://example.com/1"),
            HistoryItem.SessionItem("Kotlin协程详解", "https://example.com/2"),
            HistoryItem.DateItem("昨天"),
            HistoryItem.SessionItem("Jetpack Compose入门", "https://example.com/3"),
        )

        return HistoryAdapter(historyItems){ clickedItem ->
            // 打开文章详情等操作
            Toast.makeText(this, clickedItem.sessionId, Toast.LENGTH_SHORT).show()
        }
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
//        if (root.isDrawerOpen(slider)) {
//            root.closeDrawer(slider)
//        } else {
//            root.openDrawer(slider)
//        }
        if(drawerLayout.isOpen){
            drawerLayout.closeDrawers()
        }else{
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
}