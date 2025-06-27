package sdk.chat.demo.robot.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.OnLoadMoreListener
import sdk.chat.demo.robot.adpter.data.FavoriteAdapter
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.model.FavoriteList
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.activities.BaseActivity
import sdk.guru.common.RX

class FavoriteListActivity : BaseActivity(), View.OnClickListener {
    private lateinit var myAdapter: FavoriteAdapter
    private var layoutManager: LinearLayoutManager? = null
    private lateinit var refreshLayout: SwipeRefreshLayout
    private var recyclerView: RecyclerView? = null

    //    private var handler: Handler? = null
    private var count = 0
    private var mOnLoadMoreListener: OnLoadMoreListener? = null
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_list)

        init()
    }

    private fun init() {
        mOnLoadMoreListener = object : OnLoadMoreListener() {
            override fun onLoading(countItem: Int, lastItem: Int) {
                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                    override fun run() {
                        getData("loadMore")
                    }
                }, 3000)
            }
        }
        myAdapter = FavoriteAdapter(mOnLoadMoreListener!!)
        findViewById<View?>(R.id.home).setOnClickListener(this)
        layoutManager = LinearLayoutManager(this)

        refreshLayout = findViewById<SwipeRefreshLayout?>(R.id.swiperefreshlayout)
        recyclerView = findViewById<RecyclerView?>(R.id.recyclerview)

        recyclerView!!.setLayoutManager(layoutManager)
        recyclerView!!.setItemAnimator(DefaultItemAnimator())
        recyclerView!!.setAdapter(myAdapter)

        //设置下拉时圆圈的颜色（可以尤多种颜色拼成）
        refreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_orange_light
        )
        //设置下拉时圆圈的背景颜色（这里设置成白色）
        refreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white)

        refreshLayout.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                getData("refresh")
            }
        })
        recyclerView!!.addOnScrollListener(mOnLoadMoreListener!!)

        getData("reset")
    }


    private fun getData(type: String?) {
        if ("reset" == type) {
            listFavorite()
//        } else if ("refresh" == type) {
//            myAdapter.clear()
//            count = 0
//            for (i in 0..12) {
//                count += 1
//                myAdapter.addItem(count)
//            }
        } else if ("loadMore" == type) {
            var i=1;
//            for (i in 0..2) {
//                count += 1
//                myAdapter.addItem(count)
//            }
        }

//        myAdapter.notifyDataSetChanged()
        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false)
        }
        if ("refresh" == type) {
            Toast.makeText(getApplicationContext(), "刷新完毕", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplicationContext(), "加载完毕", Toast.LENGTH_SHORT).show()
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

    fun listFavorite() {
        dm.add(
            GWApiManager.shared().listFavorite(1, 10)
//                .flatMap(Function { messages: FavoriteList ->
//                    var lastDay = "";
//                    val articleList = messages.map { message ->
//                        var dateStr = DateLocalizationUtil.dateStr(message.date)
//                        val parts = dateStr.split(" ")
//                        var thisDay = ""
//                        var thisTime = ""
//                        if (parts.size == 2) {
//                            thisDay = parts[0];
//                            thisTime = parts[1]
//                        }
//
//                        var showDay = thisDay != lastDay
//                        lastDay = thisDay
//                        val aiFeedback: MessageDetail? = GWMsgHandler.getAiFeedback(message)
//                        Article(
//                            id = message.entityID,
//                            content = message.text,
//                            day = thisDay,
//                            time = thisTime,
//                            title = aiFeedback?.summary ?: message.stringForKey("summary"),
//                            colorTag = runCatching {
//                                aiFeedback?.feedback?.colorTag?.toColorInt()
//                                    ?: message.stringForKey("colorTag").toColorInt()
//                            }
//                                .getOrElse { exception ->
//                                    "#FFFBE8".toColorInt()
//                                },
//                            showDay = showDay
//                        )
//                    }
//                    Single.just(articleList)
//                } as Function<List<Message?>?, SingleSource<List<Article?>?>?>)
                .observeOn(RX.main())
                .subscribe(
                    { messages ->
                        myAdapter.setItems(messages.items as ArrayList<FavoriteList.FavoriteItem>)
                        recyclerView?.scrollToPosition(0);
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@FavoriteListActivity,
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }
}