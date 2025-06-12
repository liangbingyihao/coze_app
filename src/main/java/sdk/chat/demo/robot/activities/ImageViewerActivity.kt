package sdk.chat.demo.robot.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ImagePagerAdapter
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.ui.activities.BaseActivity

class ImageViewerActivity : BaseActivity(), View.OnClickListener {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var imageUrls: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(layout)
//        hideSystemBars() // 启动时立即隐藏
        findViewById<View>(R.id.back).setOnClickListener(this)
        findViewById<View>(R.id.download).setOnClickListener(this)
        findViewById<View>(R.id.share).setOnClickListener(this)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        loadData()

        // 手势滑动退出（可选）
        setupGestureExit()
    }

    private fun setAdapter() {
        // 模拟网络图片列表
//        val imageUrls = listOf(
//            "https://oss.tikvpn.in/daily/bdcb19f1158148d6a48b8fc2f4580d61.webp",
//            "https://oss.tikvpn.in/daily/a2ce308b5dfc4238b191f5260666f9ba.webp",
//            "https://oss.tikvpn.in/daily/1c1da8e53292425e9a0206f50824311b.webp"
//        )

        // 设置适配器（传递当前 Lifecycle）
        viewPager.adapter = ImagePagerAdapter(imageUrls, lifecycle)

        // 绑定 TabLayout 指示器
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "${position + 1}/${imageUrls.size}"
        }.attach()

        // 预加载相邻页面（优化性能）
        viewPager.offscreenPageLimit = 1

// 3. 监听页面滑动，预加载下一页图片
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position < imageUrls.size - 1) {
                    lifecycleScope.launch {
                        Glide.with(this@ImageViewerActivity)
                            .downloadOnly()
                            .load(imageUrls[position + 1]) // 预加载下一页
                            .preload()
                    }
                }
//                if (position == 0) {
//                    // 滑到头部假项时，无动画跳转到倒数第二项（真实最后一项）
//                    viewPager.setCurrentItem(imageUrls.size, false)
//                } else if (position == imageUrls.size) {
//                    viewPager.setCurrentItem(0, false)
//                }
            }
        })
    }

    private fun loadData() {
        dm.add(
            ImageApi.listImageDaily(0, 0).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
                            imageUrls = data.imgs.map { it.url }.toTypedArray().toList()
                            setAdapter()
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

            R.id.download -> {
                Toast.makeText(this, "待实现", Toast.LENGTH_SHORT).show()
            }

            R.id.share -> {
                Toast.makeText(this, "待实现", Toast.LENGTH_SHORT).show()
            }
        }
    }

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