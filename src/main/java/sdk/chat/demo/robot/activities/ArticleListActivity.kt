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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import sdk.chat.core.dao.Message
import sdk.chat.core.dao.Thread
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.adpter.GenericMenuPopupWindow
import sdk.chat.demo.robot.adpter.SessionPopupAdapter
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.robot.adpter.data.ArticleSession
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.showMaterialConfirmationDialog
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.LoadMoreSwipeRefreshLayout
import sdk.chat.demo.robot.ui.PopupMenuHelper
import sdk.chat.ui.activities.BaseActivity
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.RX


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private lateinit var articleAdapter: ArticleAdapter;
    private lateinit var swipeRefreshLayout: LoadMoreSwipeRefreshLayout
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var sessionId: String = ""
    private lateinit var tvTitle: TextView
    private lateinit var vEdSummaryContainer: View
    private lateinit var bConversations: View
    private lateinit var vEdSummary: EditText
    private lateinit var vEmptyContainer: View
    private lateinit var vLineDash: View
    private lateinit var recyclerView: RecyclerView
    private var isEditingSummary = false
    private var currentPage = 1

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
        setupRecyclerView()
        setupRefreshLayout()

        loadSessions()
        findViewById<View>(R.id.home).setOnClickListener(this)
        tvTitle = findViewById<View>(R.id.title) as TextView
        tvTitle.setOnClickListener(this)
        findViewById<View>(R.id.edSummaryExit).setOnClickListener(this)
        findViewById<View>(R.id.edSummaryConfirm).setOnClickListener(this)
        findViewById<View>(R.id.more_menus).setOnClickListener(this)
        bConversations = findViewById<View>(R.id.conversations)
        bConversations.setOnClickListener(this)
        findViewById<View>(R.id.conversations1).setOnClickListener(this)

        vEmptyContainer = findViewById<View>(R.id.empty_container)
        vLineDash = findViewById<View>(R.id.dash_line)

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.ThreadRemoved))
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    finish()
                })
        )
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById<RecyclerView?>(R.id.recyclerView)
        articleAdapter = ArticleAdapter(
            onItemClick = { article ->
                // 处理普通点击
//                Toast.makeText(this, "点击了: ${article.localId}，${article.title}", Toast.LENGTH_SHORT).show()
                ChatActivity.start(ArticleListActivity@ this, article.id);
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
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupRefreshLayout() {
        swipeRefreshLayout = findViewById<LoadMoreSwipeRefreshLayout?>(R.id.swiperefreshlayout)
        swipeRefreshLayout.apply {
            // 下拉刷新监听
            setOnRefreshListener {
                articleAdapter.isLoading = false
                setLoadingMore(false)
                loadArticles()
            }

            // 绑定RecyclerView
            setupWithRecyclerView(recyclerView)

//            // 上拉加载监听
//            setOnLoadMoreListener(object : LoadMoreSwipeRefreshLayout.OnLoadMoreListener {
//                override fun onLoadMore() {
//                    if (!articleAdapter.isLoading) {
//                        articleAdapter.isLoading = true
//                        setLoadingMore(true)
//                        loadArticles(++currentPage)
//                    }
//                }
//            })
            setCanLoadMore(false)
        }
    }

    private fun setEmptyRecord(isEmpty: Boolean) {
        if (isEmpty) {
            vEmptyContainer.visibility = View.VISIBLE
            vLineDash.visibility = View.INVISIBLE
            bConversations.visibility = View.INVISIBLE
        } else {
            vEmptyContainer.visibility = View.INVISIBLE
            vLineDash.visibility = View.VISIBLE
            bConversations.visibility = View.VISIBLE
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
        return 0;
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
                            localId = message.id,
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
                        swipeRefreshLayout.isRefreshing = false
                        articleAdapter.submitList(messages)
                        if (messages != null && !messages.isEmpty()) {
                            setEmptyRecord(false)
                        } else {
                            setEmptyRecord(true)
                        }
                        recyclerView.scrollToPosition(0);
                    },
                    { error -> // onError
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            this@ArticleListActivity,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

//    fun loadArticlesFromServer(page: Int) {
//        currentPage = page
//        if(page==1){
//            articleAdapter.clearAll()
//        }
////        Toast.makeText(applicationContext, "page:${page}", Toast.LENGTH_SHORT).show()
//        dm.add(
//            GWApiManager.shared().listMessage(sessionId, page, 20)
//                .flatMap(Function { messages: List<MessageDetail> ->
//                    var lastDay = "";
//                    val articleList = messages.map { message ->
//                        val parts = message.createdAt.split("T")
//                        var thisDay = ""
//                        var thisTime = ""
//                        if (parts.size == 2) {
//                            thisDay = parts[0];
//                            thisTime = parts[1]
//                        }
//
//                        var showDay = thisDay != lastDay
//                        lastDay = thisDay
////                        val aiFeedback: MessageDetail? = GWMsgHandler.getAiFeedback(message)
//
//                        Article(
//                            id = message.id,
//                            content = message.content,
//                            day = thisDay,
//                            time = thisTime,
//                            title = message.summary,
//                            colorTag = message.feedback?.colorTag?.toColorInt()
//                                ?: "#FFFBE8".toColorInt(),
//                            showDay = showDay
//                        )
//                    }
//                    Single.just(articleList)
//                } as Function<List<MessageDetail?>?, SingleSource<List<Article>>>)
//                .observeOn(RX.main())
//                .subscribe(
//                    { messages ->
//                        articleAdapter.isLoading = false
//                        if (page == 1) {
//                            articleAdapter.clearAll()
//                            articleAdapter.appendItems(messages,Runnable {
//                                recyclerView.scrollToPosition(0);
//                            });
//                            swipeRefreshLayout.isRefreshing = false
//                            if (messages != null && !messages.isEmpty()) {
//                                setEmptyRecord(false)
//                            } else {
//                                setEmptyRecord(true)
//                            }
//                        }else{
//                            articleAdapter.appendItems(messages,null);
//                            swipeRefreshLayout.setLoadingMore(false)
//                        }
//                        if (messages == null || messages.isEmpty()) {
//                            swipeRefreshLayout.setCanLoadMore(false)
//                        } else {
//                            swipeRefreshLayout.setCanLoadMore(true)
//                        }
//                    },
//                    { error -> // onError
//                        articleAdapter.isLoading = false
//                        swipeRefreshLayout.setLoadingMore(false)
//                        swipeRefreshLayout.isRefreshing = false
//                        Toast.makeText(
//                            this@ArticleListActivity,
//                            "加载失败: ${error.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    })
//        )
//    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home, R.id.conversations, R.id.conversations1 -> {
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

//            R.id.conversations -> {
//                finish()
//            }

            R.id.more_menus -> {
                PopupMenuHelper(
                    context = this,
                    anchorView = v,
                    onItemSelected = { v ->
                        when (v.id) {
                            R.id.delTopic -> {
                                showMaterialConfirmationDialog(
                                    this@ArticleListActivity,
                                    "",
                                    getString(R.string.delete_confirm),
                                    positiveAction = {
                                        val disposable = threadHandler.deleteSession(sessionId)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                Consumer { newState: Boolean? ->
                                                    ToastHelper.show(
                                                        this@ArticleListActivity,
                                                        getString(R.string.success)
                                                    )
                                                    finish()
                                                },
                                                Consumer { error: Throwable? ->
                                                    ToastHelper.show(
                                                        this@ArticleListActivity,
                                                        error?.message
                                                    );
                                                })
                                        dm.add(disposable)
                                    })
                            }

                        }
                    },
                    menuResId = R.layout.menu_article_topic,
                    clickableResIds = intArrayOf(
                        R.id.delTopic,
                    )
                ).show()
            }

        }
    }

//    fun showMaterialConfirmationDialog(
//        context: Context,
//        title: String?,
//        message: String,
//        positiveAction: () -> Unit
//    ) {
//        val dialog = MaterialAlertDialogBuilder(context)
////            .setTitle(title)
//            .setMessage(message)
//            .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
//                positiveAction()
//                dialog.dismiss()
//            }
//            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
//                dialog.dismiss()
//            }
//            .setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_background))
//            .create()
//
//        dialog.setOnShowListener {
//            // 获取按钮并自定义
//            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
////                setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
//                setTextColor(ContextCompat.getColor(context, R.color.item_text_selected))
//            }
//
//            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
//                setTextColor(ContextCompat.getColor(context, R.color.item_text_normal))
//            }
//        }
//
//        dialog.show()
//    }

    // 在Activity/Fragment中使用
    fun showPopupMenu(anchorView: View, selectedArticle: Article) {
        // 根据选中项目动态创建菜单项
        PopupMenuHelper(
            context = this,
            anchorView = anchorView,
            onItemSelected = { v ->
                when (v.id) {
                    R.id.delArticle -> {
                        showMaterialConfirmationDialog(
                            this@ArticleListActivity,
                            "",
                            getString(R.string.delete_confirm),
                            positiveAction = {
                                changeTopic(-1)
                            })
                    }

                    R.id.changeTopic -> {
                        menuPopup.show(true)
                    }
                }
            },
            menuResId = R.layout.menu_article_popup,
            clickableResIds = intArrayOf(
                R.id.delArticle,
                R.id.changeTopic,
            )
        ).show()
    }

    fun changeTopic(topicId: Long) {
        val msgId = articleAdapter.selectId
        if (msgId == null) {
            return
        }
        dm.add(
            threadHandler.setMsgSession(msgId, topicId)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (topicId <= 0) {
                            if (articleAdapter.itemCount == 2) {
                                setEmptyRecord(true)
                            }
                            articleAdapter.deleteById(msgId)
                        } else {
//                            sessionId = topicId.toString()
                            loadSessions()
                            start(this@ArticleListActivity,topicId.toString())
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
            threadHandler.newMsgSession(msgId, newSummary)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (result > 0) {
//                            sessionId = result.toString()
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