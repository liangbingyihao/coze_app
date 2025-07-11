package sdk.chat.demo.robot.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.ChatActivity
import sdk.chat.demo.robot.adpter.SearchResultAdapter
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.FavoriteList
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.handlers.FrequencyCounter
import sdk.chat.demo.robot.ui.LoadMoreSwipeRefreshLayout
import sdk.chat.ui.fragments.BaseFragment
import sdk.guru.common.RX

interface OnDataListener {
    fun getSearchKeyword(): String
}

class SearchResultFragment : BaseFragment() {
    private lateinit var swipeRefreshLayout: LoadMoreSwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: SearchResultAdapter
    private var currentPage = 1
    private var searchingKey = ""

    companion object {
        private const val ARG_QUERY_TYPE = "query_type"

        fun newInstance(source: String): SearchResultFragment {
            //"feed", "topic", "question","favorite"
            if (!arrayOf("feed", "topic", "question", "favorite").contains(source)) {
                throw Exception("error query source")
            }
            val args = Bundle().apply {
                putString(ARG_QUERY_TYPE, source)
            }
            return SearchResultFragment().apply { arguments = args }
        }
    }

    private var queryType: String = ""

    private var dataListener: OnDataListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataListener = context as? OnDataListener
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun initViews() {

    }

    override fun clearData() {

    }

    override fun reloadData() {

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        sharedPref = activity?.getSharedPreferences(
//            "ai_prompt", // 文件名（如不指定，则使用默认的 PreferenceManager.getDefaultSharedPreferences）
//            Context.MODE_PRIVATE // 仅当前应用可访问
//        )
        // 加载布局
        val view = inflater.inflate(R.layout.fragment_search_result, container, false)
        queryType = arguments?.getString(ARG_QUERY_TYPE, "") ?: ""
        setupRecyclerView(view)
        setupRefreshLayout(view)
//        searchMessage(currentPage)
        return view
    }

    private fun setupRecyclerView(root: View) {
        recyclerView = root.findViewById<RecyclerView?>(R.id.recyclerView)
        myAdapter = SearchResultAdapter(onItemClick = { item ->
            // 处理普通点击
            context?.let { ChatActivity.start(it, item.messageId) };
        })
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = myAdapter
    }

    private fun setupRefreshLayout(root: View) {
        swipeRefreshLayout = root.findViewById<LoadMoreSwipeRefreshLayout?>(R.id.swiperefreshlayout)
        swipeRefreshLayout.apply {
            // 下拉刷新监听
            setOnRefreshListener {
                currentPage = 1
                myAdapter.isLoading = false
                setLoadingMore(false)
                searchMessage(currentPage)
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
                        searchMessage(currentPage)
                    }
                }
            })
        }
    }

    fun searchMessage(page: Int) {
//        Toast.makeText(applicationContext, "page:${page}", Toast.LENGTH_SHORT).show()
        if (page == 1) {
            myAdapter.clear()
        }
        dm.add(
            GWApiManager.shared().listMessage(queryType, searchingKey, page, 20)
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
                            swipeRefreshLayout.setCanLoadMore(false)
                            Toast.makeText(
                                context,
                                "没有记录",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            swipeRefreshLayout.setCanLoadMore(true)
                        }
                        myAdapter.addItems(messages.items as ArrayList<FavoriteList.FavoriteItem>);
                    },
                    { error -> // onError
                        searchingKey = ""
                        myAdapter.isLoading = false
                        swipeRefreshLayout.setLoadingMore(false)
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            context,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun performSearch(force: Boolean) {
        var searchKey = dataListener?.getSearchKeyword()
        if (searchKey?.isNotEmpty() == true) {
            if (!force && searchingKey == searchKey && myAdapter.itemCount > 1) {
                return
            }
            FrequencyCounter.increment(context, searchKey)
            searchingKey = searchKey
            currentPage = 1
            swipeRefreshLayout.isRefreshing = true
            searchMessage(currentPage)
        }
    }

    override fun onResume() {
        super.onResume()
//        Toast.makeText(
//            context,
//            "前台: $queryType",
//            Toast.LENGTH_SHORT
//        ).show()
        performSearch(false)
    }
}