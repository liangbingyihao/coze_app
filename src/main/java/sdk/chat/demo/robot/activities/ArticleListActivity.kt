package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Function
import sdk.chat.core.dao.Message
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.adpter.GenericMenuPopupWindow
import sdk.chat.demo.robot.adpter.SessionPopupAdapter
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.robot.adpter.data.ArticleSession
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.activities.BaseActivity
import sdk.guru.common.RX
import android.graphics.Color
import androidx.core.graphics.toColorInt


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private var articleAdapter: ArticleAdapter? = null;
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var sessionId: String = ""
    private lateinit var  tvTitle: TextView

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, topicId: String? = null) {
            val intent = Intent(context, ArticleListActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, topicId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_list)

        sessionId = intent.getStringExtra(EXTRA_INITIAL_DATA).toString()


        // 设置RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter { article -> /* 点击处理 */ }
        recyclerView.adapter = articleAdapter
        loadSessions()
        findViewById<View>(R.id.home).setOnClickListener(this)
        tvTitle = findViewById<View>(R.id.title) as TextView
        tvTitle.setOnClickListener(this)

//        // 添加分割线（可选）
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    private fun initMenuPopup(items:List<ArticleSession>) {
        var selectedPosition = items.indexOfFirst { it.id == sessionId }.let {
            if (it < 0) 0 else it
        }
        var menuPopupAdapter = SessionPopupAdapter(this, items, selectedPosition)
        var title = items[selectedPosition].title
        tvTitle.text = title
        menuPopup = GenericMenuPopupWindow(
            context = this,
            anchor = findViewById(R.id.home),
            adapter = menuPopupAdapter,
            onItemSelected = { item, position ->
                tvTitle.text = item.title
                sessionId = item.id
                menuPopup.setTitle(item.title)
                loadArticles()
            }
        )
        menuPopup.setTitle(title)
    }

    override fun getLayout(): Int {
        return R.layout.activity_article_list;
    }

    fun loadSessions() {
        dm.add(
            threadHandler.listSessions()
                .flatMap(Function { sessopms: MutableList<sdk.chat.core.dao.Thread> ->
                    val articleSessions = sessopms?.map { session ->
                        ArticleSession(
                            id = session.entityID,
                            title = session.name,
                        )
                    }
                    Single.just(articleSessions)
                } as Function<in MutableList<sdk.chat.core.dao.Thread>, SingleSource<List<ArticleSession>>>)
                .observeOn(RX.main())
                .subscribe(
                    { articleSessions ->
//                        menuPopup.updateMenuItems(articleSessions, 0)
                        initMenuPopup(articleSessions)
                        loadArticles()
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
            threadHandler.loadMessagesBySession(sessionId)
                .flatMap(Function { messages: List<Message> ->
                    val articleList = messages.map { message ->
                        Article(
                            id = message.id,
                            content = message.text,
                            time = DateLocalizationUtil.formatDay(message.date),
                            title = message.stringForKey("summary"),
                            colorTag = runCatching { message.stringForKey("colorTag").toColorInt() }
                                .getOrElse { exception ->
                                    "#FFFBE8".toColorInt()
                                }
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