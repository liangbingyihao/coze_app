package sdk.chat.demo.robot.holder

//import sdk.chat.ui.R
import android.graphics.drawable.Drawable
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.messages.MessagesListStyle
import io.noties.markwon.Markwon
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import sdk.chat.core.dao.Keys
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.manager.DownloadablePayload
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.StateStorage
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.chat.model.MessageHolder
import sdk.chat.ui.module.UIModule
import sdk.chat.ui.views.ProgressView
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX
import java.text.DateFormat

open class ChatTextViewHolder<T : MessageHolder>(itemView: View) :
    MessageHolders.BaseMessageViewHolder<T>(itemView, null),
    MessageHolders.DefaultMessageViewHolder,
    Consumer<Throwable> {
    open var root: View? = itemView.findViewById(sdk.chat.ui.R.id.root)
    open var bubble: ViewGroup? = itemView.findViewById(R.id.bubble)

    open var messageIcon: ImageView? = itemView.findViewById(R.id.messageIcon)

    open var imageOverlay: ImageView? = itemView.findViewById(R.id.imageOverlay)

    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(R.id.feedback)
    open var feedbackHint: TextView? = itemView.findViewById(R.id.feedbackHint)
    open var time: TextView? = itemView.findViewById(R.id.messageTime)

    open var readStatus: ImageView? = itemView.findViewById(R.id.readStatus)
//    open var replyView: View? = itemView.findViewById(R.id.replyView)
//    open var replyImageView: ImageView? = itemView.findViewById(R.id.replyImageView)
//    open var replyTextView: TextView? = itemView.findViewById(R.id.replyTextView)

    open var progressView: ProgressView? = itemView.findViewById(R.id.progressView)
    open var bubbleOverlay: View? = itemView.findViewById(R.id.bubbleOverlay)

    open var resendTextView: TextView? = itemView.findViewById(R.id.resendTextView)
    open var resendContainer: ConstraintLayout? = itemView.findViewById(R.id.resendContainer)
    open var sessionContainer: View? =
        itemView.findViewById(R.id.session_container)
    open var sessionName: TextView? = itemView.findViewById(R.id.session_name)

    //    open val btnFavorite: IconicsImageView? =
//        itemView.findViewById(R.id.btn_favorite)
//    open val btnDelete: IconicsImageView? = itemView.findViewById(R.id.btn_delete)
    open val btnPlay: ImageView? =
        itemView.findViewById(R.id.btn_play)
    open val feedbackMenu: View? = itemView.findViewById(R.id.feedback_menu)
    open val contentMenu: View? = itemView.findViewById(R.id.user_text_menu)

    open var format: DateFormat? = null

    open val dm = DisposableMap()
    open var image: ImageView? = itemView.findViewById(R.id.image)
    open var imageContainer: View? = itemView.findViewById(R.id.image_container)
    open var imageMenu: View? = itemView.findViewById(R.id.image_menu)
    open var bible: TextView? = itemView.findViewById(R.id.bible)

//    val exploreView: Map<String, TextView> = mapOf(
//        "explore0" to itemView.findViewById<TextView>(R.id.explore1),
//        "explore1" to itemView.findViewById<TextView>(R.id.explore2),
//        "explore2" to itemView.findViewById<TextView>(R.id.explore3)
//    )
    open var imageLikeAi: ImageView? = itemView.findViewById(R.id.btn_like_ai)
    open var imageLikeContent: ImageView? = itemView.findViewById(R.id.btn_like_user_text)

//    open var userClickListener: MessagesListAdapter.UserClickListener? = null

    init {
        itemView.let {
            format = UIModule.shared().messageBinder.messageTimeComparisonDateFormat(it.context)
        }
    }

    override fun onBind(holder: T) {
        bindListeners(holder)
//        bindStyle(holder)
        bind(holder)
    }

    open fun bind(t: T) {
        progressView?.actionButton?.setOnClickListener(View.OnClickListener {
            actionButtonPressed(t)
        })
        progressView?.bringToFront()

        bubble?.let {
            it.isSelected = isSelected
        }

        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
        if (t.text != null && !t.text.isEmpty()) {
            setText(t.message.id.toString() + "," + t.text, t.enableLinkify())
            var topic = threadHandler.getSessionName(t.message.threadId)
            if (topic != null) {
                sessionContainer?.visibility = View.VISIBLE
                sessionName?.let {
                    it.text = topic
                }
            } else {
                sessionContainer?.visibility = View.GONE
            }

        } else {
            sessionContainer?.visibility = View.GONE
            setText(t.message.id.toString() + ",原消息已删除", t.enableLinkify())
        }

        time?.let {
            UIModule.shared().timeBinder.bind(it, t)
        }

        messageIcon?.let {
            UIModule.shared().iconBinder.bind(it, t)
        }

        if (StateStorage.getStateB(t.message.status)) {
            imageLikeAi?.setImageResource(R.mipmap.ic_dislike_black)
        } else {
            imageLikeAi?.setImageResource(R.mipmap.ic_like_black)
        }
        if (StateStorage.getStateA(t.message.status)) {
            imageLikeContent?.setImageResource(R.mipmap.ic_dislike_black)
        } else {
            imageLikeContent?.setImageResource(R.mipmap.ic_like_black)
        }

        if (t.message.equals(threadHandler.playingMsg)) {
            btnPlay?.setImageResource(R.mipmap.ic_pause_black);
        } else {
            btnPlay?.setImageResource(R.mipmap.ic_play_black);
        }
        var aiFeedback: MessageDetail? = (t as? TextHolder)?.getAiFeedback();
//        var i = 0
//        var aiExplore = threadHandler.aiExplore
//        while (i < 3) {
//            var v: TextView = exploreView.getValue("explore$i")
//            if (aiExplore != null && t.message.id.equals(aiExplore.message.id) && i < aiExplore.itemList.size) {
//                var data = aiExplore.itemList[i]
//                v.visibility = View.VISIBLE
//                v.text = data.text
//                if (data.action == GWThreadHandler.action_bible_pic) {
//                    var bible = aiFeedback?.feedback?.bible ?: ""
//                    if (bible.isEmpty()) {
//                        v.visibility = View.GONE
//                        continue
//                    }
////                    bible = aiFeedback?.feedbackText ?:""
//                    v.setOnClickListener { view ->
//                        // 可以使用view参数
//                        if (aiFeedback != null && !bible.isEmpty()) {
//                            view as TextView
//                            threadHandler.sendExploreMessage(
//                                view.text.toString().trim(),
//                                t.message,
//                                data.action,
//                                "${aiFeedback.feedback.tag}|${bible}"
//                            ).subscribe();
//                        } else {
//                            Toast.makeText(v.context, "没有经文...", Toast.LENGTH_SHORT)
//                                .show()
//                        }
//                    }
//                } else {
//                    v.setOnClickListener { view ->
//                        view as TextView // 安全转换
//                        threadHandler.sendExploreMessage(
//                            view.text.toString().trim(),
//                            t.message,
//                            data.action,
//                            data.params
//                        ).subscribe();
//                    }
//                }
//            } else {
//                v.visibility = View.GONE
//            }
//            ++i
//        }

//        t.message.metaValuesAsMap
        var feedbackText = aiFeedback?.feedbackText ?: t.message.stringForKey("feedback")
        feedback?.let {
//            it.text = t.message.stringForKey("feedback")+t.message.id+t.message.type;
//            val markdown = "**Hello** _Markdown_"
            Markwon.create(it.context)
                .setMarkdown(it, feedbackText)
        }


        if (t.message.entityID.equals("welcome")) {
            feedbackMenu?.visibility = View.GONE
            contentMenu?.visibility = View.GONE
        } else {
            if (aiFeedback?.status != 2 || feedbackText.isEmpty()) {
                feedbackMenu?.visibility = View.GONE
            } else {
                feedbackMenu?.visibility = View.VISIBLE
            }
            //FIXME
            if (aiFeedback?.status == 2) {
                feedbackHint?.visibility = View.GONE
            } else {
                feedbackHint?.visibility = View.VISIBLE
                if(t.message.id==threadHandler.pendingMsgId()){
                    feedbackHint?.setText(R.string.loading);
                }else{
                    feedbackHint?.setText(R.string.msg_failed);
                }
//                val hints = ImageApi.getGwConfigs()?.configs?.generatingHint
//                feedbackHint?.text = if (!hints.isNullOrEmpty()) {
//                    hints.random()
//                } else {
//                    "种子正在发芽，新信息马上生长..." // 默认值
//                }
            }
            if (t.message.text.isEmpty()) {
                contentMenu?.visibility = View.GONE
            } else {
                contentMenu?.visibility = View.VISIBLE
            }
        }


        var imageUrl = t.message.stringForKey(Keys.ImageUrl)
        var bibleText = aiFeedback?.feedback?.bible
        if (imageUrl.isEmpty() || bibleText == null || bibleText.isEmpty()) {
            imageContainer?.visibility = View.GONE
            imageMenu?.visibility = View.GONE
        } else {
            imageContainer?.visibility = View.VISIBLE
            imageMenu?.visibility = View.VISIBLE
            bible?.text = bibleText
            Glide.with(image!!)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.icn_200_image_message_placeholder) // 占位图
                .error(R.drawable.icn_200_image_message_error) // 错误图
                .addListener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(image!!)
        }


//        var saved = StateStorage.getStateA(t.message.status)
//        text?.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
//            public override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
//                if (menu == null) {
//                    return true
//                }
//
//                val systemItems = (0 until menu.size()).mapNotNull { menu.getItem(it) }
//
//                menu.clear()
//
////                menu.add(0, R.id.like, 0, R.string.like)
////                    .setIcon(R.mipmap.ic_like_black)
////                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
////                menu.add(0, R.id.remove, 0, R.string.remove)
////                    .setIcon(R.mipmap.ic_del_black)
////                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//                systemItems.forEachIndexed { index, item ->
//                    if (index == 0) {
//                        if (saved) {
//                            menu.add(0, R.id.like, 0, R.string.unsave)
//                                .setIcon(R.mipmap.ic_dislike_black);
//                        } else {
//                            menu.add(0, R.id.like, 0, R.string.save)
//                                .setIcon(R.mipmap.ic_like_black);
//                        }
//                    } else if (index == 1) {
//                        menu.add(0, R.id.remove, 0, R.string.remove)
//                            .setIcon(R.mipmap.ic_del_black)
//                    }
//                    menu.add(item.groupId, item.itemId, index, item.title)
//                }
//
//                return true
//            }
//
//            public override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
////                var saved = StateStorage.getStateA(t.message.status)
////                menu?.findItem(R.id.like)?.apply {
////                    if (saved) {
////                        setTitle(R.string.unsave)
////                        setIcon(R.mipmap.ic_dislike_black)
////                    } else {
////                        setTitle(R.string.save)
////                        setIcon(R.mipmap.ic_like_black)
////                    }
////                }
//                return false
//            }
//
//            public override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
//                val start: Int = text!!.selectionStart
//                val end: Int = text!!.selectionEnd
//                val selectedText: String? = text!!.getText().toString().substring(start, end)
//
//                val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
//                when (item?.itemId) {
//                    R.id.remove -> {
//                        // Handle your custom action
////                        Toast.makeText(
////                            this@MainActivity,
////                            "Selected: " + selectedText,
////                            Toast.LENGTH_SHORT
////                        ).show()
//                        val disposable =
//                            threadHandler.clearUserText(t.message)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(
//                                    Consumer { result: Boolean ->
//                                        if (!result) {
//                                            ToastHelper.show(
//                                                text?.context,
//                                                text?.context?.getString(R.string.failed_and_retry)
//                                            );
//                                        }
//                                    },
//                                    Consumer { error: Throwable? ->
//                                        ToastHelper.show(text?.context, error?.message);
//                                    })
//                        dm.add(disposable)
//                        mode?.finish()
//                        return true
//                    }
//
//                    R.id.like -> {
//
//                        val disposable =
//                            threadHandler.toggleContentLikeState(t.message)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(
//                                    Consumer { newState: Int? ->
//                                        if (newState == 0) {
//                                            ToastHelper.show(
//                                                text?.context,
//                                                text?.context?.getString(R.string.unsaved)
//                                            );
//                                        } else if (newState == 1) {
//                                            ToastHelper.show(
//                                                text?.context,
//                                                text?.context?.getString(R.string.saved)
//                                            );
//                                        }
//                                    },
//                                    Consumer { error: Throwable? ->
//                                        ToastHelper.show(text?.context, error?.message);
//                                    })
//                        dm.add(disposable)
//                        mode?.finish()
//
////                        threadHandler.toggleContentLikeState(t.message)
////                        if(saved){
////                            ToastHelper.show(text?.context, text?.context?.getString(R.string.unsaved));
////                        }else{
////                            ToastHelper.show(text?.context, text?.context?.getString(R.string.saved));
////                        }
//                        return true
//                    }
//
//                    android.R.id.copy -> {
//                        Toast.makeText(text?.context, selectedText, Toast.LENGTH_SHORT)
//                            .show()
//                        return false
//                    }
//
//                    else -> return false
//                }
//            }
//
//            public override fun onDestroyActionMode(mode: ActionMode?) {
//                // Action mode finished
//            }
//        })
    }

    open fun setText(value: String, linkify: Boolean) {
        if (!value.isEmpty()) {
            bubble?.visibility = View.VISIBLE
            text?.let {
                if (linkify) {
                    it.autoLinkMask = Linkify.ALL
                } else {
                    it.autoLinkMask = 0
                }
                it.text = value
            }

        } else {
            bubble?.visibility = View.GONE
        }
    }

    open fun bindReadStatus(t: T) {
        readStatus?.let {
            UIModule.shared().readStatusViewBinder.onBind(it, t)
        }
    }

    open fun bindSendStatus(holder: T): Boolean {
        val showOverlay =
            progressView?.bindSendStatus(holder.sendStatus, holder.payload) ?: false
        bubbleOverlay?.visibility = if (showOverlay) View.VISIBLE else View.INVISIBLE

        // If we are showing overlay, hide icon
        messageIcon?.let {
            if (showOverlay) {
                it.visibility = View.INVISIBLE
            } else {
                UIModule.shared().iconBinder.bind(it, holder)
            }
        }

        bindResend(holder)

        return showOverlay
    }

    open fun bindResend(holder: T) {
        resendContainer?.let {
            if (holder.canResend()) {
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
        }
    }

    open fun bindProgress(t: T) {
        progressView?.bindProgress(t)
        bindResend(t)
    }

    open fun bindListeners(t: T) {
        dm.dispose()
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(
                    NetworkEvent.filterType(
                        EventType.MessageSendStatusUpdated,
                        EventType.MessageReadReceiptUpdated
                    )
                )
                .filter(NetworkEvent.filterMessageEntityID(t.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        bindReadStatus(t)
                        bindSendStatus(t)
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
                .filter(NetworkEvent.filterMessageEntityID(t.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        bindProgress(t)
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageUpdated))
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        bind(t)
                    }
                })


    }

////                val now = System.currentTimeMillis()
////                if (now - lastActionModeTime < ACTION_MODE_DEBOUNCE) {
////                    return false
////                }
////
////                if (isActionModeActive) {
////                    return false
////                }
//
////                lastActionModeTime = now
//                mode?.setCustomView(View(text!!.context))
//
//                // 创建垂直布局的PopupMenu
//                verticalPopupMenu = PopupMenu(text!!.context, text)
//
//
//                // 将系统菜单项复制到PopupMenu
//                for (i in 0..<menu!!.size) {
//                    val systemItem = menu[i]
//                    verticalPopupMenu?.getMenu()?.add(systemItem.getTitle())
//                        ?.setIcon(systemItem.getIcon())
//                        ?.setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
////                            mode.finish(); // 选择菜单项后结束ActionMode
//                            onActionItemClicked(
//                                mode,
//                                systemItem
//                            )
//                        })
//                }
//                // 监听PopupMenu关闭
//                verticalPopupMenu?.setOnDismissListener {
//                    mode?.finish() // 只在菜单关闭时结束ActionMode
//                }
////
////                // 取消系统默认的水平菜单
////                mode.finish()
//
//
//                // 显示垂直菜单
//                verticalPopupMenu?.show()
//                return true


    // Inflate your custom menu
//                mode.menuInflater.inflate(R.menu.custom_text_selection_menu, menu)

    //                menu?.add(0, R.id.like, 2, "自定义操作1")
//                    ?.setIcon(R.mipmap.ic_like_black)
//                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //    public Predicate<NetworkEvent> filter() {
    //        return new Predicate<NetworkEvent>() {
    //            @Override
    //            public boolean test(NetworkEvent networkEvent) throws Exception {
    //                return networkEvent.type == type;
    //            }
    //        };
    //    }
    fun filterById(id: Long?): Predicate<NetworkEvent?> {
        return Predicate { networkEvent: NetworkEvent? -> networkEvent?.message?.id == id }
    }

    override fun applyStyle(style: MessagesListStyle) {
////        this.style = style
//        if (direction == MessageDirection.Incoming) {
//            applyIncomingStyle(style)
//        } else {
////            applyOutgoingStyle(style)
//        }
    }


    open fun actionButtonPressed(holder: T) {
        val payload = holder.payload
        if (payload is DownloadablePayload) {
            progressView?.let { view ->
                dm.add(payload.startDownload().observeOn(RX.main()).subscribe({
                    view.actionButton?.visibility = View.INVISIBLE
                }, {
                    it.printStackTrace()
                    view.actionButton?.visibility = View.VISIBLE
                }))
            }
        }
    }

    override fun setAvatarClickListener(l: MessagesListAdapter.UserClickListener?) {
//        userClickListener = l
    }

    override fun accept(t: Throwable?) {
        t?.printStackTrace()
    }

}