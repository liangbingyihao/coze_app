package sdk.chat.demo.robot.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.core.dao.Message
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.findTopmostVisibleViewByResId
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.activities.BaseActivity
import java.lang.ref.WeakReference

var hasShownGuideOverlay = false
fun hasShownGuideOverlay(context: Context): Boolean {
    if (!hasShownGuideOverlay) {
        hasShownGuideOverlay = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("has_shown_guide_all", false)
    }
    return hasShownGuideOverlay
}

class HighlightOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var weakContext: WeakReference<BaseActivity>? = null
    private var weakMessage: WeakReference<Message>? = null
    private val maskBackground: View
    private val highlightIndicator: View
    private val highlightTarget: TextView
    private val highlightDesc: TextView
    private var mode: String? = null

    private val guideDrawer = "guide_drawer"
    private val guidePic = "guide_pic"
    private val guidePray = "guide_pray"
    private val allModes = arrayOf(guideDrawer, guidePic, guidePray)

    private var onClickListener: OnClickListener = View.OnClickListener { view ->
        // 处理点击事件
        when (view.id) {
            R.id.mask_background -> {
                true
            }

            R.id.btn_next -> {
                handleNext()
                true
            }
        }
    }

    init {
        // 加载布局
        inflate(context, R.layout.overlay_guide, this)
        maskBackground = findViewById(R.id.mask_background)
        highlightIndicator = findViewById(R.id.guide_view)
        highlightTarget = findViewById(R.id.highlight_target)
        highlightDesc = findViewById(R.id.highlight_desc)
        maskBackground.setOnClickListener(onClickListener)
        findViewById<View>(R.id.btn_next).setOnClickListener(onClickListener)
    }


    private fun setHighlightMode(mode: String): Boolean {
        var context = weakContext?.get()

        if (context == null) {
            return false
        }

        var m: ImageView? = null
        if (mode == guideDrawer) {
            val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
            var topic = threadHandler.getSessionName(weakMessage?.get()?.threadId)
            if (topic != null) {
                m = context.findViewById<ImageView>(R.id.menu_home)
                highlightTarget.setText(R.string.guide_drawer)
                var desc = context.getString(R.string.guide_drawer_desc, topic)
                highlightDesc.text = desc
            }
        } else if (mode == guidePic) {
            var r: RecyclerView =
                context.findViewById<View>(R.id.chatView)
                    .findViewById<RecyclerView>(R.id.recyclerview)
            m = findTopmostVisibleViewByResId(r, R.id.btn_pic) as ImageView?
            highlightTarget.setText(R.string.guide_pic)
            highlightDesc.setText(R.string.guide_pic_desc)
        } else if (mode == guidePray) {
            var r: RecyclerView =
                context.findViewById<View>(R.id.chatView)
                    .findViewById<RecyclerView>(R.id.recyclerview)
            m = findTopmostVisibleViewByResId(r, R.id.btn_pray) as ImageView?
            highlightTarget.setText(R.string.guide_pray)
            highlightDesc.setText(R.string.guide_pray_desc)
        } else {
            return false
        }
        if (m != null) {
            m.post {
                setHighlightView(m)
            }
            context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putBoolean("has_shown_guide_$mode", true)
                }
            return true
        }
        return false
    }

    /**
     * 将指示 View 移动到目标 View 的位置
     * @param targetView 需要高亮的 View（如按钮）
     */
    private fun setHighlightView(targetView: ImageView) {
        // 1. 获取目标 View 在屏幕中的位置
        val targetLocation = IntArray(2)
        targetView.getLocationOnScreen(targetLocation)

        // 2. 获取 HighlightOverlayView 在屏幕中的位置
        val overlayLocation = IntArray(2)
        this.getLocationOnScreen(overlayLocation)

        // 3. 计算相对坐标（将目标 View 的坐标转换为相对于 HighlightOverlayView 的坐标）
        val relativeLeft = targetLocation[0] - overlayLocation[0]
        val relativeTop = targetLocation[1] - overlayLocation[1]

        Log.e(
            "setHighlightView",
            "relativeX:${relativeLeft},relativeY:${relativeTop},overlayRect:${overlayLocation},targetLocation[0]:${targetLocation}"
        )


//        // 1. 获取目标 View 在屏幕中的全局边界
//        val targetRect = Rect()
//        targetView.getGlobalVisibleRect(targetRect)
//
//        // 2. 获取 HighlightOverlayView 在屏幕中的全局边界
//        val overlayRect = Rect()
//        this.getGlobalVisibleRect(overlayRect)
//
//        // 3. 计算相对位置（考虑滚动视图等情况）
//        val relativeLeft = targetRect.left - overlayRect.left
//        val relativeTop = targetRect.top - overlayRect.top
//
//
//
        val imageDrawable: Drawable? = targetView.drawable

        // 4. 调整指示 View 的位置和大小
        highlightIndicator.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = relativeLeft.toInt()
            topMargin = relativeTop.toInt()
        }

        highlightTarget.setCompoundDrawablesWithIntrinsicBounds(
            imageDrawable, // left
            null,          // top
            null,          // right
            null           // bottom
        )

        // 5. 显示指示 View
        visibility = VISIBLE


    }


    fun handleFirst(context: BaseActivity, message: Message?) {

        Log.e("MainApp", "highlight.handleFirst")
        if (hasShownGuideOverlay(context)) {
            return
        }
        if (this.mode != null) {
            return
        }

        var match = false
        val action = message!!.integerForKey("action")
        if (action == 0) {
            val contextId = message.stringForKey("context_id")
            if (contextId == null || contextId.isEmpty()) {
                val aiFeedback = GWMsgHandler.getAiFeedback(message)
                if (aiFeedback != null && aiFeedback.status == MessageDetail.STATUS_SUCCESS) {
                    match = true
                }
            }
        }
        if (!match) {
            return
        }

        weakContext = WeakReference<BaseActivity>(context)
        weakMessage = WeakReference<Message>(message)

        handleNext()
    }

    fun handleNext() {

        Log.e("MainApp", "highlight.handleNext")
        var done = 0
        for (m in allModes) {
            if (!context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getBoolean("has_shown_guide_$m", false)
            ) {
                this.mode = m
                Log.e("MainApp", "highlight.handleNext.${m}")
                if (setHighlightMode(m)) {
                    Log.e("MainApp", "highlight.handleNext.${m} done")
                    return
                }
            } else {
                ++done
            }
        }
        if(done>=allModes.size){
            Log.e("MainApp", "highlight.handleNext.all_done")
            context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putBoolean("has_shown_guide_all", true)
                }
            hasShownGuideOverlay = true
        }
        this.mode = null
        visibility = GONE

    }
}