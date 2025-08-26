package sdk.chat.demo.robot.holder

//import sdk.chat.ui.R
import android.graphics.drawable.Drawable
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.audio.TTSHelper
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


    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(R.id.feedback)
    open var sendErrorHint: TextView? = itemView.findViewById(R.id.send_error_hint)
    open var replyErrorHint: TextView? = itemView.findViewById(R.id.reply_error_hint)
    open var processContainer: View? = itemView.findViewById(R.id.process_container)
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
//        progressView?.actionButton?.setOnClickListener(View.OnClickListener {
//            actionButtonPressed(t)
//        })
//        progressView?.bringToFront()
//
//        bubble?.let {
//            it.isSelected = isSelected
//        }

        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
        var action = (t as? TextHolder)?.action
        if (action != GWThreadHandler.action_daily_pray && t.text != null && !t.text.isEmpty()) {
            setText(
                t.message.id.toString() + "," + t.text,
//                t.message.id.toString() + "," + t.message.messageStatus.name + ","+ (t as? TextHolder)?.aiFeedback?.status +"," + t.text,
                t.enableLinkify()
            )
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
            if (action != GWThreadHandler.action_daily_pray && !t.message.entityID.equals("welcome")) {
                setText("原消息已删除", t.enableLinkify())
            } else {
                setText("", false);
            }
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

        if (t.message.equals(TTSHelper.getPlayingMsg()) && !TTSHelper.isPlayerPaused()) {
            btnPlay?.setImageResource(R.mipmap.ic_pause_black);
        } else {
            btnPlay?.setImageResource(R.mipmap.ic_play_black);
        }

        bindSendStatus(t)

        var aiFeedback: MessageDetail? = (t as? TextHolder)?.getAiFeedback();

//        t.message.metaValuesAsMap
        var feedbackText = aiFeedback?.feedbackText ?: ""
//        feedbackText = aiFeedback?.feedbackText ?: t.message.stringForKey("feedback")
        feedback?.let {
            it.visibility = View.VISIBLE
            Markwon.create(it.context)
                .setMarkdown(it, feedbackText)
        }


        if (t.message.entityID.equals("welcome")) {
            contentMenu?.visibility = View.GONE
            bubble?.visibility = View.GONE
            feedbackMenu?.visibility = View.VISIBLE
            sessionContainer?.visibility = View.GONE
            processContainer?.visibility = View.GONE
            sendErrorHint?.visibility = View.GONE
        } else {
            if (feedbackText.isEmpty()) {
                feedbackMenu?.visibility = View.GONE
            }
//            if (aiFeedback?.status == MessageDetail.STATUS_SUCCESS) {
//                processContainer?.visibility = View.GONE
//            } else {
//                processContainer?.visibility = View.VISIBLE
//                if (t.message.id == threadHandler.pendingMsgId()) {
//                    feedbackHint?.setText(R.string.loading);
//                    feedbackHint?.setTextColor("#919191".toColorInt())
//                    imageFeedbackHint?.setImageResource(R.drawable.loading_animation)
//                } else {
//                    feedbackHint?.setText(R.string.ai_failed);
//                    feedbackHint?.setTextColor("#FFCF4B40".toColorInt())
//                    imageFeedbackHint?.setImageResource(R.mipmap.ic_redo_red)
//                }
//            }
            if (t.message.text.isEmpty() || action == GWThreadHandler.action_daily_pray) {
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

        var aiFeedback: MessageDetail? = (holder as? TextHolder)?.getAiFeedback();
        var status = holder.message.messageStatus
//        Log.d("sending", "bindSendStatus:" + status.name)
        if (status.ordinal < MessageSendStatus.Replying.ordinal) {
            feedbackMenu?.visibility = View.GONE
            feedback?.visibility = View.GONE
            contentMenu?.visibility = View.GONE
            if (aiFeedback == null && status == MessageSendStatus.UploadFailed) {
                sendErrorHint?.visibility = View.VISIBLE
            } else {
                sendErrorHint?.visibility = View.GONE
            }

            if (status == MessageSendStatus.Uploading) {
                processContainer?.visibility = View.VISIBLE
            } else {
                processContainer?.visibility = View.GONE
            }
            replyErrorHint?.visibility = View.GONE
        } else {
            feedback?.visibility = View.VISIBLE
            processContainer?.visibility = View.GONE
            feedbackMenu?.visibility = View.GONE
            if (status == MessageSendStatus.Sent) {
                var feedbackText = aiFeedback?.feedbackText ?: ""
                if (!feedbackText.isEmpty()) {
                    feedbackMenu?.visibility = View.VISIBLE
                }
            } else if (status == MessageSendStatus.Replying) {
                processContainer?.visibility = View.VISIBLE
            }

            if (status == MessageSendStatus.Failed) {
                replyErrorHint?.visibility = View.VISIBLE
            } else {
                replyErrorHint?.visibility = View.GONE
            }
            sendErrorHint?.visibility = View.GONE
        }
        return true


//        if (aiFeedback == null && status == MessageSendStatus.UploadFailed) {
//            //消息也没发出去
//            sendErrorHint?.visibility = View.VISIBLE
//            replyErrorHint?.visibility = View.GONE
//            contentMenu?.visibility = View.GONE
//            processContainer?.visibility = View.GONE
//            imageContainer?.visibility = View.GONE
//            imageMenu?.visibility = View.GONE
//            return true
//        }
//        sendErrorHint?.visibility = View.GONE
//        if (status == MessageSendStatus.Uploading || status == MessageSendStatus.Replying) {
//            feedbackHint?.setText(R.string.uploading);
//            feedbackHint?.setTextColor("#919191".toColorInt())
//            imageFeedbackHint?.setImageResource(R.drawable.loading_animation)
//            return true
//        } else if (holder.message.messageStatus == MessageSendStatus.Failed) {
//            feedbackHint?.setText(R.string.ai_failed);
//            feedbackHint?.setTextColor("#FFCF4B40".toColorInt())
//            imageFeedbackHint?.setImageResource(R.mipmap.ic_redo_red)
//        } else if (holder.message.messageStatus == MessageSendStatus.Replying) {
//            val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
//            if (holder.message.id == threadHandler.pendingMsgId()) {
//                processContainer?.visibility = View.VISIBLE
//                feedbackHint?.setText(R.string.loading);
//                feedbackHint?.setTextColor("#919191".toColorInt())
//                imageFeedbackHint?.setImageResource(R.drawable.loading_animation)
//            }
//        }
//        return false

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
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe { networkEvent ->
                    RX.main().scheduleDirect {
                        bindReadStatus(t)
                        bindSendStatus(t)
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        bindProgress(t)
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter { networkEvent: NetworkEvent? ->
                    networkEvent!!.type == EventType.ThreadsUpdated
                            && networkEvent.threadId == t.message.threadId
                }
//                .filter { networkEvent: NetworkEvent? -> networkEvent!!.type == EventType.ThreadsUpdated }
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                        var topic = threadHandler.getSessionName(t.message.threadId)
                        if (topic != null) {
                            sessionName?.let {
                                it.text = topic
                            }
                        }
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