package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
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
import androidx.core.graphics.toColorInt


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private var articleAdapter: ArticleAdapter? = null;
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var sessionId: String = ""
    private lateinit var tvTitle: TextView
    private lateinit var vEdSummaryContainer: View
    private lateinit var vEdSummary: EditText

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


        vEdSummaryContainer = findViewById(R.id.edSummaryContainer)
        vEdSummary = findViewById(R.id.edSummary)

        // 设置RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(
            onItemClick = { article ->
                // 处理普通点击
                Toast.makeText(this, "点击了: ${article.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { article ->
                // 处理编辑点击
                Toast.makeText(this, "编辑: ${article.title}", Toast.LENGTH_SHORT).show()
                vEdSummary.setText(article.title)
                vEdSummaryContainer.visibility = View.VISIBLE
                showKeyboard()
                vEdSummary.requestFocus()
            },
            onLongClick = { article ->
                // 处理长按
                Toast.makeText(this, "长按: ${article.title}", Toast.LENGTH_SHORT).show()
                true
            })
        recyclerView.adapter = articleAdapter
        loadSessions()
        findViewById<View>(R.id.home).setOnClickListener(this)
        tvTitle = findViewById<View>(R.id.title) as TextView
        tvTitle.setOnClickListener(this)
        findViewById<View>(R.id.edSummaryExit).setOnClickListener(this)
        findViewById<View>(R.id.edSummaryConfirm).setOnClickListener(this)

    }

    private fun initMenuPopup(items: List<ArticleSession>) {
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
                    var lastDay = "";
                    val articleList = messages.map { message ->
                        var dateStr = DateLocalizationUtil.dateStr(message.date)
                        val parts = dateStr.split(" ")
                        var thisDay = ""
                        var thisTime = ""
                        if (parts.size == 2) {
                            thisDay = parts[0];
                            thisTime = parts[1]
                        }

                        var showDay = thisDay != lastDay
                        lastDay = thisDay
                        Article(
                            id = message.entityID,
                            content = message.text + message.entityID,
                            day = thisDay,
                            time = thisTime,
                            title = message.stringForKey("summary"),
                            colorTag = runCatching { message.stringForKey("colorTag").toColorInt() }
                                .getOrElse { exception ->
                                    "#FFFBE8".toColorInt()
                                },
                            showDay = showDay
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

            R.id.edSummaryExit -> {
                vEdSummary.requestFocus()
                hideKeyboard()
                vEdSummaryContainer.visibility = View.GONE
            }

            R.id.edSummaryConfirm -> {
                val newSummary = vEdSummary.text.toString()
                val msgId= articleAdapter?.selectId
                dm.add(
                    threadHandler.setSummary(msgId, newSummary)
                        .observeOn(RX.main())
                        .subscribe(
                            { result ->
                                if (result) {
                                    articleAdapter?.updateSummaryId(msgId, newSummary)
                                }
                                vEdSummary.requestFocus()
                                hideKeyboard()
                                vEdSummaryContainer.visibility = View.GONE
                            },
                            { error -> // onError
                                vEdSummary.requestFocus()
                                hideKeyboard()
                                vEdSummaryContainer.visibility = View.GONE
                                Toast.makeText(
                                    this@ArticleListActivity,
                                    "修改失败: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                )
            }

        }
    }

}