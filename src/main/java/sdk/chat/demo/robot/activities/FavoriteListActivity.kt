package sdk.chat.demo.robot.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.OnLoadMoreListener
import sdk.chat.demo.robot.adpter.data.FavoriteAdapter
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.FavoriteList
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.LoadMoreSwipeRefreshLayout
import sdk.chat.ui.activities.BaseActivity
import sdk.guru.common.RX

class FavoriteListActivity : BaseActivity(), View.OnClickListener {
    private lateinit var myAdapter: FavoriteAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var swipeRefreshLayout: LoadMoreSwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private var currentPage = 1

    //    private var handler: Handler? = null
    private var count = 0
    private var mOnLoadMoreListener: OnLoadMoreListener? = null
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler

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
        myAdapter = FavoriteAdapter()
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
                myAdapter.clear()
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
        Toast.makeText(applicationContext, "page:${page}", Toast.LENGTH_SHORT).show()
        dm.add(
            GWApiManager.shared().listFavorite(page, 3)
                .observeOn(RX.main())
                .subscribe(
                    { messages ->
                        myAdapter.isLoading = false
                        if (page == 1) {
                            myAdapter.clear()
//                            recyclerView.scrollToPosition(0);
                            swipeRefreshLayout.isRefreshing = false
                        } else {
                            swipeRefreshLayout.setLoadingMore(false)
                        }
                        if (messages.items == null || messages.items.isEmpty()) {
                            swipeRefreshLayout.setCanLoadMore(false)
                        }else{
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
}