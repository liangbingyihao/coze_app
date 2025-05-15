package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.data.Article
import sdk.chat.ui.activities.BaseActivity


class ArticleListActivity : BaseActivity() {
    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, topicId: Long? = null) {
            val intent = Intent(context, ArticleListActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, topicId)
            }
            context.startActivity(intent)
        }
    }
    private val topics = listOf("全部", "科技", "健康", "体育") // 下拉菜单选项

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_list)
        // 1. 设置 ActionBar 返回键
        setTitle("hahahah");
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 显示返回箭头

        val topicId = intent.getLongExtra(EXTRA_INITIAL_DATA,0)

        // 模拟数据
        val articles = listOf(
            Article("1", "08:30", "晨间新闻", "今日天气晴，气温25℃，适合外出..."),
            Article("2", "09:15", "科技动态", "苹果发布iOS 16新功能预览..."),
            Article("3", "10:00", "健康贴士", "每天喝够8杯水有助于新陈代谢...")
        )

        // 设置RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = ArticleAdapter { article -> /* 点击处理 */ }
        recyclerView.adapter = adapter
        adapter.submitList(articles)

        // 添加分割线（可选）
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun getLayout(): Int {
        return R.layout.activity_article_list;
    }



    // 3. 创建下拉菜单 Spinner
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_article_list, menu)

        // 获取 Spinner
        val spinnerItem = menu.findItem(R.id.spinner_topic)
        val spinner = spinnerItem.actionView as Spinner

        // 配置 Spinner 适配器
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            topics
        )

        // 监听选项切换
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selectedTopic = topics[pos]
                Toast.makeText(this@ArticleListActivity, "切换主题: $selectedTopic", Toast.LENGTH_SHORT).show()
                // 这里加载对应主题的数据
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return true
    }

    // 4. 处理返回键点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { // 返回键 ID
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}