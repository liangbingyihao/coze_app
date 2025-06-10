package sdk.chat.demo.robot.activities

import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ImagePagerAdapter

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // 模拟网络图片列表
        val imageUrls = listOf(
            "https://oss.tikvpn.in/daily/bdcb19f1158148d6a48b8fc2f4580d61.webp",
            "https://oss.tikvpn.in/daily/a2ce308b5dfc4238b191f5260666f9ba.webp",
            "https://oss.tikvpn.in/daily/1c1da8e53292425e9a0206f50824311b.webp"
        )

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
            }
        })
        // 手势滑动退出（可选）
        setupGestureExit()
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
}