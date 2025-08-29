package sdk.chat.demo.robot.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import sdk.chat.core.dao.Message
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.FavoriteAdapter
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.FavoriteList
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.LoadMoreSwipeRefreshLayout
import sdk.chat.demo.robot.ui.PopupMenuHelper
import sdk.chat.ui.activities.BaseActivity
import sdk.chat.ui.utils.ToastHelper
import sdk.guru.common.RX

class FavoriteListActivity : BaseActivity(), View.OnClickListener {
    private lateinit var myAdapter: FavoriteAdapter
    private lateinit var swipeRefreshLayout: LoadMoreSwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private var currentPage = 1

    //    private var handler: Handler? = null
//    private var count = 0
//    private var mOnLoadMoreListener: OnLoadMoreListener? = null
//    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_list)
        findViewById<View?>(R.id.home).setOnClickListener(this)
        setupRecyclerView()
        setupRefreshLayout()
        listFavorite(currentPage)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById<RecyclerView?>(R.id.recyclerview)
        myAdapter = FavoriteAdapter(
            onItemClick = { item ->
                // 处理普通点击
                ChatActivity.start(FavoriteListActivity@ this, item.messageId);
            },
            onLongClick = { v, article ->
                // 处理长按
//                Toast.makeText(this, "长按: ${article.title}", Toast.LENGTH_SHORT).show()
                showPopupMenu(v, article)
                true
            })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = myAdapter
    }

    private fun setupRefreshLayout() {
        swipeRefreshLayout = findViewById<LoadMoreSwipeRefreshLayout?>(R.id.swiperefreshlayout)
        swipeRefreshLayout.apply {
            // 下拉刷新监听
            setOnRefreshListener {
                currentPage = 1
                myAdapter.isLoading = false
                setLoadingMore(false)
                listFavorite(currentPage)
            }

            // 绑定RecyclerView
            setupWithRecyclerView(recyclerView)

            // 上拉加载监听
            setOnLoadMoreListener(object : LoadMoreSwipeRefreshLayout.OnLoadMoreListener {
                override fun onLoadMore() {
                    if (!myAdapter.isLoading) {
                        myAdapter.isLoading = true
                        setLoadingMore(true)
                        currentPage++
                        listFavorite(currentPage)
                    }
                }
            })
        }
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.home) {
            finish();
        }
    }

    fun listFavorite(page: Int) {
//        Toast.makeText(applicationContext, "page:${page}", Toast.LENGTH_SHORT).show()
        if (page == 1) {
            myAdapter.clear()
        }
        dm.add(
            GWApiManager.shared().listFavorite(page, 20)
                .observeOn(RX.main())
                .subscribe(
                    { messages ->
                        myAdapter.isLoading = false
                        if (page == 1) {
//                            recyclerView.scrollToPosition(0);
                            swipeRefreshLayout.isRefreshing = false
                        } else {
                            swipeRefreshLayout.setLoadingMore(false)
                        }
                        if (messages.items == null || messages.items.isEmpty()) {
                            ToastHelper.show(this@FavoriteListActivity,R.string.error_no_data)
                            swipeRefreshLayout.setCanLoadMore(false)
                        } else {
                            swipeRefreshLayout.setCanLoadMore(true)
                        }
                        myAdapter.addItems(messages.items as ArrayList<FavoriteList.FavoriteItem>);
                    },
                    { error -> // onError
                        myAdapter.isLoading = false
                        swipeRefreshLayout.setLoadingMore(false)
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            this@FavoriteListActivity,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    override fun onDestroy() {
        swipeRefreshLayout.setOnRefreshListener(null)
        swipeRefreshLayout.setOnLoadMoreListener(null)
        super.onDestroy()
    }

    fun showPopupMenu(anchorView: View, item: FavoriteList.FavoriteItem) {
        // 根据选中项目动态创建菜单项
        PopupMenuHelper(
            context = this,
            anchorView = anchorView,
            onItemSelected = { v ->
                when (v.id) {
                    R.id.del -> {
                        val threadHandler = ChatSDK.thread() as GWThreadHandler

                        var message: Message = ChatSDK.db().fetchEntityWithEntityID<Message?>(
                            item.messageId,
                            Message::class.java
                        );
                        val r: Single<Int?> =
                            if (item.contentType == GWApiManager.contentTypeAI) threadHandler.toggleAiLikeState(
                                message
                            ) else threadHandler.toggleContentLikeState(
                                message
                            )
                        val disposable: Disposable = r.observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                Consumer { newState: Int? ->
                                    if (newState == 0) {
                                        ToastHelper.show(
                                            this@FavoriteListActivity,
                                            getString(R.string.unsaved)
                                        )
                                        myAdapter.delItem(item)
                                    } else if (newState == 1) {
                                        ToastHelper.show(
                                            this@FavoriteListActivity,
                                            getString(R.string.saved)
                                        )
                                    }
                                },
                                this
                            )
                        dm.add(disposable)

                    }

                    R.id.copy -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("恩语", item.content)
                        clipboard.setPrimaryClip(clip)
                        ToastHelper.show(
                            MainApp.getContext(),
                            MainApp.getContext().getString(R.string.copied)
                        )
                    }

                    R.id.share -> {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.setType("text/plain") // 或具体类型如 "image/jpeg"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, item.content)
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "恩语之声") // 临时权限

                        startActivity(Intent.createChooser(shareIntent, "分享到"))
                    }
                }
            },
            menuResId = R.layout.menu_favorite_popup,
            clickableResIds = intArrayOf(
                R.id.copy,
                R.id.share,
                R.id.del
            )
        ).show()
    }
}