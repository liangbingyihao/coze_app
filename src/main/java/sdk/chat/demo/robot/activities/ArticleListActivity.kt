package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function;
import sdk.chat.core.dao.Message
import sdk.chat.core.dao.Thread
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.adpter.GenericMenuPopupWindow
import sdk.chat.demo.robot.adpter.HistoryItem
import sdk.chat.demo.robot.adpter.SessionPopupAdapter
import sdk.chat.demo.robot.adpter.SessionSpinnerAdapter
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.robot.adpter.data.ArticleSession
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.activities.BaseActivity
import sdk.guru.common.RX


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private var articleAdapter: ArticleAdapter? = null;
    private var sessionAdapter: SessionSpinnerAdapter? = null;
    private lateinit var menuPopupAdapter: SessionPopupAdapter
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var selectedPosition = 0

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, topicId: Long? = null) {
            val intent = Intent(context, ArticleListActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, topicId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_list)
//        // 1. 设置 ActionBar 返回键
//        setSupportActionBar(findViewById(R.id.toolbar))
//        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 显示返回箭头
//        supportActionBar?.setDisplayShowTitleEnabled(false);

        val topicId = intent.getLongExtra(EXTRA_INITIAL_DATA, 0)


        // 设置RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter { article -> /* 点击处理 */ }
        recyclerView.adapter = articleAdapter
        loadArticles()
//        initMenuPopup()
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.title).setOnClickListener(this)

        // 添加分割线（可选）
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


//        // 设置自定义Toolbar
//        val toolbar: Toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)


//        // 获取Spinner并设置适配器
//        val spinner: androidx.appcompat.widget.AppCompatSpinner =
//            toolbar.findViewById(R.id.spinner_topic)
////        val adapter = ArrayAdapter.createFromResource(
////            this,
////            R.array.topics_array, android.R.layout.simple_spinner_item
////        )
////        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////        spinner.setAdapter(adapter)
//        // 配置 Spinner 适配器
//        sessionAdapter = SessionSpinnerAdapter(
//            this,
//            R.layout.item_session_spinner,
//        );
//        spinner.adapter = sessionAdapter;
//
//        spinner.post {
//            val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                val windowMetrics = windowManager.currentWindowMetrics
//                windowMetrics.bounds.width()
//            } else {
//                val displayMetrics = DisplayMetrics()
//                @Suppress("DEPRECATION")
//                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
//                displayMetrics.widthPixels
//            }
//
//            spinner.dropDownWidth = screenWidth
//            spinner.dropDownHorizontalOffset = 50;
//        }
//
//
//        // 监听选项切换
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
//                sessionAdapter?.setSelectedPosition(pos);
//                val session: ArticleSession? = sessionAdapter?.getItem(pos)
//                Toast.makeText(
//                    this@ArticleListActivity,
//                    "切换主题: ${session?.title}",
//                    Toast.LENGTH_SHORT
//                ).show()
//                // 这里加载对应主题的数据
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
        loadSessions()
    }

    private fun initMenuPopup(items:List<ArticleSession>) {
        menuPopupAdapter = SessionPopupAdapter(this, items, selectedPosition)
        menuPopup = GenericMenuPopupWindow(
            context = this,
            anchor = findViewById(R.id.home),
            adapter = menuPopupAdapter,
            onItemSelected = { item, position ->
                selectedPosition = position
//                updateSpinnerView(item)
                // 执行菜单项对应的操作
                Toast.makeText(
                    this@ArticleListActivity,
                    "切换主题: ${item?.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    override fun getLayout(): Int {
        return R.layout.activity_article_list;
    }

    fun loadSessions() {
        dm.add(
            threadHandler.listSessions()
                .flatMap(Function { sessopms: MutableList<sdk.chat.core.dao.Thread?>? ->
                    val articleSessions = sessopms?.map { session ->
                        ArticleSession(
                            id = (session?.id ?: "").toString(),
                            title = session?.name ?: "hello",
                        )
                    }
                    Single.just(articleSessions)
                } as Function<in MutableList<sdk.chat.core.dao.Thread>, SingleSource<List<ArticleSession>>>)
                .observeOn(RX.main())
                .subscribe(
                    { articleSessions ->
//                        menuPopup.updateMenuItems(articleSessions, 0)
                        initMenuPopup(articleSessions)
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun loadArticles() {
        dm.add(
            threadHandler.loadMessagesEarlier(0, true)
                .flatMap(Function { messages: List<Message?>? ->
                    val articleList = messages?.map { message ->
                        Article(
                            id = message?.id ?: 0,
                            content = message?.text ?: "hello",
                            time = DateLocalizationUtil.formatDay(message?.date),
                            title = "abcd",
                        )
                    }
                    Single.just(articleList)
                } as Function<List<Message?>?, SingleSource<List<Article?>?>?>)
                .observeOn(RX.main())
                .subscribe(
                    { messages ->
                        articleAdapter?.submitList(messages)
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }


//    // 3. 创建下拉菜单 Spinner
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_article_list, menu)
//
//        // 获取 Spinner
//        val spinnerItem = menu.findItem(R.id.spinner_topic)
//        val spinner = spinnerItem.actionView as Spinner
//
//        // 配置 Spinner 适配器
//        spinner.adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_spinner_dropdown_item,
//            topics
//        )
//
//        // 监听选项切换
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
//                val selectedTopic = topics[pos]
//                Toast.makeText(this@ArticleListActivity, "切换主题: $selectedTopic", Toast.LENGTH_SHORT).show()
//                // 这里加载对应主题的数据
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
//
//        return true
//    }

//    // 4. 处理返回键点击
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> { // 返回键 ID
//                finish()
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

            R.id.title -> {
                menuPopup.show()
            }

        }
    }

}