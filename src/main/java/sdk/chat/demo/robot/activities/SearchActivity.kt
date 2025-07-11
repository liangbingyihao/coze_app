package sdk.chat.demo.robot.activities

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.SearchTextAdapter
import sdk.chat.demo.robot.adpter.data.SearchText
import sdk.chat.demo.robot.fragments.OnDataListener
import sdk.chat.demo.robot.fragments.SearchResultFragment
import sdk.chat.demo.robot.handlers.FrequencyCounter
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.activities.BaseActivity

class SearchResultPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = SparseArray<SearchResultFragment>()
    private val queryType = arrayOf("feed", "topic", "question", "favorite")

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return SearchResultFragment.newInstance(queryType[position]).also {
            fragments.put(position, it)
        }
    }

    // 获取指定位置的 Fragment
    fun getFragment(position: Int): SearchResultFragment? {
        return fragments[position]
    }
}


class SearchActivity : BaseActivity(), View.OnClickListener,OnDataListener {
    private lateinit var searchText: EditText
    private lateinit var searchHistory: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var searchHistoryAdapter: SearchTextAdapter
    private lateinit var searchResultAdapter: SearchResultPagerAdapter
    val threadHandler = ChatSDK.thread() as GWThreadHandler
    private val hints = listOf("会话", "时间轴", "信仰问答", "收藏")
    private var sharedPref: SharedPreferences? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        sharedPref = getSharedPreferences(
            "search_text", // 文件名（如不指定，则使用默认的 PreferenceManager.getDefaultSharedPreferences）
            MODE_PRIVATE // 仅当前应用可访问
        )
        findViewById<View>(R.id.back).setOnClickListener(this)
        findViewById<View>(R.id.search).setOnClickListener(this)
//        customPrompt.isChecked = threadHandler.isCustomPrompt
//        customPrompt.setOnCheckedChangeListener { _, isChecked ->
//            threadHandler.setCustomPrompt(isChecked)
//        }
//        recordPrompt = findViewById(R.id.recordPrompt)
//
//        val prompt = sharedPref.getString("record", getString(R.string.prompt_record))
//        recordPrompt.setText(prompt)
//        val fontSize = sharedPref.getInt("font_size", 14)
//        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
//        val brightness = sharedPref.getFloat("brightness", 1.0f)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager.isUserInputEnabled = false

        searchResultAdapter = SearchResultPagerAdapter(this)
        viewPager.adapter = searchResultAdapter
        // 将 TabLayout 与 ViewPager2 关联
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = hints[position]
        }.attach()
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                viewPager.post { performSearch(false) }
//            }
//        })


        initSearchHistoryView()

        searchText = findViewById(R.id.search_text)
        searchText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showHistory()
            }
        }
        // 监听搜索按键
        searchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(true)
                true
            } else false
        }
        searchText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performSearch(true)
                true
            } else false
        }

        showHistory()
    }

    override fun getLayout(): Int {
        return 0
    }

    fun initSearchHistoryView() {
        searchHistory = findViewById<RecyclerView>(R.id.history_list)

        // 初始化 FlexboxLayoutManager
        val flexboxLayoutManager = FlexboxLayoutManager(this@SearchActivity).apply {
            flexDirection = FlexDirection.ROW  // 主轴方向
            flexWrap = FlexWrap.WRAP           // 允许换行
            justifyContent = JustifyContent.FLEX_START    // 主轴对齐方式
            alignItems = AlignItems.CENTER      // 交叉轴对齐
        }

        searchHistory.layoutManager = flexboxLayoutManager

        searchHistoryAdapter = SearchTextAdapter() { clickedItem ->
//            Toast.makeText(this, "Clicked: ${clickedItem.text}", Toast.LENGTH_SHORT).show()
            searchText.setText(clickedItem.text)
            performSearch(true)
        }

        searchHistory.adapter = searchHistoryAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.back -> {
                finish()
            }

            R.id.search -> {
                performSearch(true)
            }

        }
    }

    private fun showHistory() {
        tabLayout.visibility = View.INVISIBLE
        viewPager.visibility = View.INVISIBLE
        searchHistory.visibility = View.VISIBLE

        val sortedItems = FrequencyCounter.getSortedItems(this@SearchActivity)
        var texts = sortedItems.take(20)
            .map { it.key }  // 确保key不为null
            .toTypedArray()
        val testData = mutableListOf<SearchText>().apply {
            // 添加Header
            add(SearchText.Header("热门搜索"))
            // 添加Tags
            addAll(texts.mapIndexed { index, text ->
                SearchText.Tag(
                    id = index.toString(),
                    text = text
                )
            })
        }

        searchHistoryAdapter.submitList(testData)
    }

    private fun showResults() {
        tabLayout.visibility = View.VISIBLE
        viewPager.visibility = View.VISIBLE
        searchHistory.visibility = View.INVISIBLE
        hideKeyboard()
        searchText.clearFocus()
    }


    private fun performSearch(force: Boolean) {
        val query = searchText.text.toString()
        if (query.isNotEmpty()) {
            var currentFragment: SearchResultFragment? =
                searchResultAdapter.getFragment(viewPager.currentItem)
            currentFragment?.performSearch(force)
            // 模拟搜索请求
//            val results = fetchSearchResults(query)
//            resultsAdapter.submitList(results)
            showResults()
        }
    }

    override fun getSearchKeyword(): String {
        return searchText.text.toString()
    }
}