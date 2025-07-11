package sdk.chat.demo.robot.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.guru.common.RX

class EditTextPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    // 定义三个页面的提示文本
    //    action_bible_pic = 1
//    action_daily_gw = 2
//    action_direct_msg = 3
//    action_daily_ai = 0
//    action_daily_pray = 4

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return EditTextFragment.newInstance(position, null)
    }
}

class EditTextFragment : Fragment() {

    companion object {
        private const val ARG_POSITION = "position"
        private const val ARG_SERVER_PROMPT = "serverPrompt"
        private var sharedPref: SharedPreferences? = null
        val actions = listOf("", "0", "4")

        fun newInstance(position: Int, prompt: String?): EditTextFragment {
            val args = Bundle().apply {
                putInt(ARG_POSITION, position)
                putString(ARG_SERVER_PROMPT, prompt) // 添加参数
            }
            return EditTextFragment().apply { arguments = args }
        }
    }

    private var position: Int = 0
    private var localPrompt: String? = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedPref = activity?.getSharedPreferences(
            "ai_prompt", // 文件名（如不指定，则使用默认的 PreferenceManager.getDefaultSharedPreferences）
            Context.MODE_PRIVATE // 仅当前应用可访问
        )
        // 加载布局
        val view = inflater.inflate(R.layout.fragment_edit_text, container, false)
        val editText = view.findViewById<EditText>(R.id.editText)
        val button = view.findViewById<Button>(R.id.actionButton)
        position = arguments?.getInt(ARG_POSITION) ?: 0

        localPrompt = sharedPref?.getString("ai_prompt${actions[position]}", null)

        if (localPrompt != null) {
            editText.setText(localPrompt)
        } else {
            editText.setText("")
        }

        // 按钮点击事件
        button.setOnClickListener {
            val text = editText.text.toString()
            sharedPref?.edit() { putString("ai_prompt${actions[position]}", text) }
        }

        return view
    }

}

class SettingPromptActivity : AppCompatActivity() {
    private lateinit var customPrompt: CheckBox
    val threadHandler = ChatSDK.thread() as GWThreadHandler
    private val hints = listOf("普通录入", "探索", "祷告默想")
    private var sharedPref: SharedPreferences? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_prompt)
        sharedPref = getSharedPreferences(
            "ai_prompt", // 文件名（如不指定，则使用默认的 PreferenceManager.getDefaultSharedPreferences）
            Context.MODE_PRIVATE // 仅当前应用可访问
        )
        customPrompt = findViewById(R.id.customPrompt)
        customPrompt.isChecked = threadHandler.isCustomPrompt
        customPrompt.setOnCheckedChangeListener { _, isChecked ->
            threadHandler.setCustomPrompt(isChecked)
        }
//        recordPrompt = findViewById(R.id.recordPrompt)
//
//        val prompt = sharedPref.getString("record", getString(R.string.prompt_record))
//        recordPrompt.setText(prompt)
//        val fontSize = sharedPref.getInt("font_size", 14)
//        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
//        val brightness = sharedPref.getFloat("brightness", 1.0f)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        viewPager.isUserInputEnabled = false

        var loadServer = findViewById<View>(R.id.loadServer)

        loadServer.setOnClickListener {
            threadHandler.serverPrompt
                .subscribeOn(RX.io())
                .observeOn(RX.main())
                .subscribe(
                    { data ->
                        if (data != null) {
                            sharedPref?.edit() { putString("ai_prompt", data.record) }
                            sharedPref?.edit() { putString("ai_prompt0", data.talk) }
                            sharedPref?.edit() { putString("ai_prompt4", data.pray) }
                            viewPager.adapter = EditTextPagerAdapter(this)
                        } else {
                            Toast.makeText(this, "获取系统提示词失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error ->
                        // 统一错误处理
                        Toast.makeText(this, "获取系统提示词失败:${error}", Toast.LENGTH_SHORT).show()
                    }
                )
        }


        viewPager.adapter = EditTextPagerAdapter(this)
        // 将 TabLayout 与 ViewPager2 关联
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = hints[position]
        }.attach()

    }


    override fun onDestroy() {
        super.onDestroy()
    }

}