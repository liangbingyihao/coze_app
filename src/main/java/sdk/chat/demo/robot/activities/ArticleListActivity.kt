package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Function
import sdk.chat.core.dao.Message
import sdk.chat.core.dao.Thread
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.adpter.GenericMenuPopupWindow
import sdk.chat.demo.robot.adpter.SessionPopupAdapter
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.robot.adpter.data.ArticleSession
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.PopupMenuHelper
import sdk.chat.ui.activities.BaseActivity
import sdk.guru.common.RX


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private lateinit var articleAdapter: ArticleAdapter;
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var sessionId: String = ""
    private lateinit var tvTitle: TextView
    private lateinit var vEdSummaryContainer: View
    private lateinit var vEdSummary: EditText
    private lateinit var vEmptyContainer: View
    private lateinit var vLineDash: View
    private lateinit var recyclerView: RecyclerView
    private var isEditingSummary = false

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
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(
            onItemClick = { article ->
                // 处理普通点击
                Toast.makeText(this, "点击了: ${article.title}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { article ->
                // 处理编辑点击
//                Toast.makeText(this, "编辑: ${article.title}", Toast.LENGTH_SHORT).show()
                isEditingSummary = true
                vEdSummary.setText(article.title)
                vEdSummaryContainer.visibility = View.VISIBLE
                showKeyboard(vEdSummary)
            },
            onLongClick = { v, article ->
                // 处理长按
//                Toast.makeText(this, "长按: ${article.title}", Toast.LENGTH_SHORT).show()
                showPopupMenu(v, article)
                true
            })
        recyclerView.adapter = articleAdapter
        loadSessions()
        findViewById<View>(R.id.home).setOnClickListener(this)
        tvTitle = findViewById<View>(R.id.title) as TextView
        tvTitle.setOnClickListener(this)
        findViewById<View>(R.id.edSummaryExit).setOnClickListener(this)
        findViewById<View>(R.id.edSummaryConfirm).setOnClickListener(this)

        vEmptyContainer = findViewById<View>(R.id.empty_container)
        vLineDash = findViewById<View>(R.id.dash_line)

    }

    private fun setEmptyRecord(isEmpty: Boolean) {
        if (isEmpty) {
            vEmptyContainer.visibility = View.VISIBLE
            vLineDash.visibility = View.INVISIBLE
        } else {
            vEmptyContainer.visibility = View.INVISIBLE
            vLineDash.visibility = View.VISIBLE
        }
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
                if (item != null) {
                    if (menuPopup.isEditModel) {
                        changeTopic(item.id.toLong())
                    } else {
                        tvTitle.text = item.title
                        sessionId = item.id
                        menuPopup.setTitle(item.title)
                        loadArticles()
                    }
                } else {
                    isEditingSummary = false
                    vEdSummary.setText("")
                    vEdSummaryContainer.visibility = View.VISIBLE
                    showKeyboard(vEdSummary)
                }
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
                .flatMap(Function { sessions: MutableList<Thread> ->
                    val articleSessions = sessions.map { session ->
                        ArticleSession(
                            id = session.entityID,
                            title = session.name,
                        )
                    }
                    Single.just(articleSessions)
                } as Function<in MutableList<Thread>, SingleSource<List<ArticleSession>>>)
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
                        val aiFeedback: MessageDetail? = GWMsgHandler.getAiFeedback(message)
                        Article(
                            id = message.entityID,
                            content = message.text,
                            day = thisDay,
                            time = thisTime,
                            title = aiFeedback?.summary ?: message.stringForKey("summary"),
                            colorTag = runCatching {
                                aiFeedback?.feedback?.colorTag?.toColorInt()
                                    ?: message.stringForKey("colorTag").toColorInt()
                            }
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
                        articleAdapter.submitList(messages)
                        if (messages != null && !messages.isEmpty()) {
                            setEmptyRecord(false)
                        } else {
                            setEmptyRecord(true)
                        }
                        recyclerView.scrollToPosition(0);
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
                menuPopup.show(false)
            }

            R.id.edSummaryExit -> {
                vEdSummary.requestFocus()
                hideKeyboard()
                vEdSummaryContainer.visibility = View.GONE
            }

            R.id.edSummaryConfirm -> {
                if (isEditingSummary) {
                    editSummary()
                } else {
                    newTopic()
                }
            }

        }
    }

    // 在Activity/Fragment中使用
    fun showPopupMenu(anchorView: View, selectedArticle: Article) {
        // 根据选中项目动态创建菜单项
        PopupMenuHelper(
            context = this,
            anchorView = anchorView,
            onItemSelected = { v ->
                when (v.id) {
                    R.id.delArticle -> {
                        changeTopic(-1)
                    }

                    R.id.changeTopic -> {
                        menuPopup.show(true)
                    }
                }
            },
        ).show()
    }

    fun changeTopic(topicId: Long) {
        val msgId = articleAdapter.selectId
        if (msgId == null) {
            return
        }
        dm.add(
            threadHandler.setSession(msgId, topicId)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (topicId <= 0) {
                            if (articleAdapter.itemCount == 2) {
                                setEmptyRecord(true)
                            }
                            articleAdapter.deleteById(msgId)
                        } else {
                            sessionId = topicId.toString()
                            loadSessions()
                        }
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "修改失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun editSummary() {
        val newSummary = vEdSummary.text.toString()
        val msgId = articleAdapter.selectId
        dm.add(
            threadHandler.setSummary(msgId, newSummary)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (result) {
                            articleAdapter.updateSummaryById(msgId, newSummary)
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


    fun newTopic() {
        val newSummary = vEdSummary.text.toString()
        val msgId = articleAdapter.selectId
        dm.add(
            threadHandler.newSession(msgId, newSummary)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (result > 0) {
                            sessionId = result.toString()
                            loadSessions()
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

    fun showKeyboard(view: View?) {
        if (view == null) return

        val context = view.getContext()
        view.requestFocus()

        view.post(Runnable {
            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+推荐方式
                    view.getWindowInsetsController()!!.show(WindowInsets.Type.ime())
                } else {
                    // 传统方式
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        })
    }
}