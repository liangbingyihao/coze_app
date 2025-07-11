package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.gyf.immersionbar.ImmersionBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ImagePagerAdapter
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.getDateBefore
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.holder.DailyGWHolder
import sdk.chat.demo.robot.holder.ImageHolder
import sdk.chat.demo.robot.ui.listener.GWClickListener
import sdk.chat.ui.activities.BaseActivity

class ImageViewerActivity : BaseActivity(), View.OnClickListener {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var endDateStr: String? = null
    private var isLoading = false
    private lateinit var adapter: ImagePagerAdapter
    private var dateStr: String? = null
    private lateinit var imageHandler: GWClickListener<ImageHolder>

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, date: String? = null) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, date)
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).init()
        setContentView(layout)
        dateStr = intent.getStringExtra(EXTRA_INITIAL_DATA)
//        hideSystemBars() // 启动时立即隐藏
        findViewById<View>(R.id.back).setOnClickListener(this)
        findViewById<View>(R.id.btn_download).setOnClickListener(this)
        findViewById<View>(R.id.btn_share_image).setOnClickListener(this)
        if (dateStr == null || dateStr!!.isEmpty()) {
            findViewById<View>(R.id.conversations).setOnClickListener(this)
        } else {
            findViewById<View>(R.id.conversations).visibility = View.INVISIBLE
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        setAdapter()
        loadData()

//        // 手势滑动退出（可选）
        setupGestureExit()
        imageHandler = GWClickListener<ImageHolder>(this)
    }

    private fun setAdapter() {
        // 绑定 TabLayout 指示器
        adapter = ImagePagerAdapter(lifecycle)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "${position + 1}/${adapter.itemCount}"
        }.attach()

//        viewPager.setPageTransformer { page, position ->
//            page.translationX = -position * page.width // 关键：取负值实现反向
//        }
        // 预加载相邻页面（优化性能）
        viewPager.offscreenPageLimit = 1

// 3. 监听页面滑动，预加载下一页图片
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position < adapter.itemCount - 1 && position > 0) {
                    lifecycleScope.launch {
                        Glide.with(this@ImageViewerActivity)
                            .downloadOnly()
                            .load(adapter.getUrlAt(position - 2)?.backgroundUrl) // 预加载下一页
                            .preload()
                    }
                }

                if (dateStr == null && position <= 2 && !isLoading && !endDateStr!!.isEmpty()) {
                    loadNextPageData()
                }
            }
        })
    }

    private fun loadNextPageData() {
        isLoading = true
        dm.add(
            ImageApi.listImageDaily(endDateStr).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        isLoading = false
                        if (data != null && !data.isEmpty()) {
                            endDateStr = getDateBefore(data[data.size - 1].date, 1)
                            var imageUrls = data.reversed()
                            adapter.prependData(imageUrls)
//                            if (imageUrls.isNotEmpty()) {
//                                viewPager.setCurrentItem(oldPosition + imageUrls.size, false)
//                            }

                        } else {
                            throw IllegalArgumentException("获取数据失败")
                        }
                    },
                    this
                )
        )
    }

    private fun loadData() {
        dm.add(
            ImageApi.listImageDaily("")
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
                            if (dateStr != null && !dateStr!!.isEmpty()) {
                                var newData = listOf(data.first { it.date == dateStr })
                                adapter.replaceData(newData)
                            } else {
                                endDateStr = getDateBefore(data[data.size - 1].date, 1)
                                adapter.replaceData(data.reversed())
                                viewPager.setCurrentItem(adapter.itemCount - 1, false)
                            }
                        } else {
                            throw IllegalArgumentException("获取数据失败")
                        }
                    },
                    this
                )
        )
    }

    override fun getLayout(): Int {
        return R.layout.activity_image_viewer
    }

    private fun setupGestureExit() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (viewPager.adapter as? ImagePagerAdapter)?.let { adapter ->
                    adapter.notifyItemChanged(position) // 触发当前页面的生命周期检查
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPager.adapter = null // 防止内存泄漏
        Glide.get(this).clearMemory()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.back -> {
                finish()
            }

            R.id.btn_download,R.id.btn_share_image -> {
                val currentPosition = viewPager.currentItem
                var data = DailyGWHolder(GWThreadHandler.action_daily_gw, adapter.getUrlAt(currentPosition))
                imageHandler.onMessageViewClick(v, data)
            }

            R.id.conversations -> {
                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                var date = adapter.getUrlAt(viewPager.currentItem)?.date
                threadHandler.aiExplore.contextId
                threadHandler.sendExploreMessage(
                    "【每日恩语】-${date}",
                    threadHandler.aiExplore.message,
                    2,
                    date
                ).subscribe();
                finish()
            }
        }
    }

    override fun onError(e: Throwable) {
        if (ChatSDK.config().debug) {
            e.printStackTrace()
        }
        alert.onError(e)
        isLoading = false
    }

//    private fun save(view: View, share: Boolean) {
//        val currentPosition = viewPager.currentItem
//        var data = DailyGWHolder(GWThreadHandler.action_daily_gw, adapter.getUrlAt(currentPosition))
//        imageHandler.onMessageViewClick(view, data)
//        return
//
//
//        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: run {
//            ToastHelper.show(this, "RecyclerView not found")
//            return
//        }
//
//        val currentViewHolder =
//            recyclerView.findViewHolderForAdapterPosition(currentPosition) as? ImagePagerAdapter.ViewHolder
//                ?: run {
//                    ToastHelper.show(this, "View not ready at position $currentPosition")
//                    return
//                }
//
//        val currentView = currentViewHolder.itemView
//        currentView.findViewById<View>(R.id.footer).visibility = View.VISIBLE
//        currentView.post {
//            val disposable = PermissionRequestHandler
//                .requestWriteExternalStorage(this@ImageViewerActivity)
//                .andThen( // 权限请求成功后，执行后续操作
//                    Observable.fromCallable<Bitmap> {
//                        val bitmap = createBitmap(currentView.width, currentView.height)
//
//                        // 2. 将 View 绘制到 Bitmap
//                        val canvas = Canvas(bitmap)
//                        currentView.draw(canvas)
//
//                        currentView.post {
//                            currentView.findViewById<View>(R.id.footer).visibility =
//                                View.INVISIBLE
//                        }
//
//                        bitmap
//                    }
//                        .subscribeOn(Schedulers.io())
//                )
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                    { bitmap ->
//                        val bitmapURL = ImageSaveUtils.saveBitmapToGallery(
//                            context = this@ImageViewerActivity,
//                            bitmap = bitmap,
//                            filename = "img_${System.currentTimeMillis()}",
//                            format = Bitmap.CompressFormat.JPEG
//                        )
//                        bitmap.recycle()
//                        if (bitmapURL != null) {
//                            ToastHelper.show(this, getString(R.string.image_saved))
//                            if (share) {
//                                val shareIntent = Intent(Intent.ACTION_SEND)
//                                shareIntent.setType("image/*") // 或具体类型如 "image/jpeg"
//                                shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL)
//                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 临时权限
//
//                                startActivity(Intent.createChooser(shareIntent, "分享图片"))
//                            }
//                        } else {
//                            ToastHelper.show(this, getString(R.string.image_save_failed))
//                        }
//                    },
//                    this
//                )
//            dm.add(disposable)
//        }
//    }
//    fun Activity.showSystemUI() {
//        WindowCompat.setDecorFitsSystemWindows(window, true)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
//        } else {
//            @Suppress("DEPRECATION")
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//        }
//    }
//
//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        if (hasFocus) hideSystemBars() // 避免退出全屏后无法恢复
//    }
}