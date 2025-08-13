package sdk.chat.demo.robot.holder

import android.graphics.drawable.Drawable
import android.text.util.Linkify
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mikepenz.iconics.view.IconicsImageView
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.messages.MessagesListStyle
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.manager.DownloadablePayload
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.robot.IconUtils
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.ui.R
import sdk.chat.ui.chat.model.ImageMessageHolder
import sdk.chat.ui.module.UIModule
import sdk.chat.ui.utils.DrawableUtil
import sdk.chat.ui.utils.ToastHelper
import sdk.chat.ui.view_holders.v2.MessageDirection
import sdk.chat.ui.views.ProgressView
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX
import java.text.DateFormat
import kotlin.math.abs

open class ChatImageViewHolder<T : ImageMessageHolder>(
    itemView: View
) :
    MessageHolders.BaseMessageViewHolder<T>(itemView, null),
    MessageHolders.DefaultMessageViewHolder,
    Consumer<Throwable> {

    companion object {
    }

    val keyIsGood: String = "is-good"
    open var root: View? = itemView.findViewById(R.id.root)
    open var bubble: ViewGroup? = itemView.findViewById(R.id.bubble)
    open var image: ImageView? = itemView.findViewById(R.id.image)

    open var messageIcon: ImageView? = itemView.findViewById(R.id.messageIcon)

    open var imageOverlay: ImageView? = itemView.findViewById(R.id.imageOverlay)

    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.feedback)
    open var time: TextView? = itemView.findViewById(R.id.messageTime)

    open var readStatus: ImageView? = itemView.findViewById(R.id.readStatus)
//    open var replyView: View? = itemView.findViewById(R.id.replyView)
//    open var replyImageView: ImageView? = itemView.findViewById(R.id.replyImageView)
//    open var replyTextView: TextView? = itemView.findViewById(R.id.replyTextView)

    open var progressView: ProgressView? = itemView.findViewById(R.id.progressView)
    open var bubbleOverlay: View? = itemView.findViewById(R.id.bubbleOverlay)

    open var resendContainer: ConstraintLayout? = itemView.findViewById(R.id.resendContainer)
    open var sessionContainer: View? =
        itemView.findViewById(sdk.chat.demo.pre.R.id.session_container)
    open var sessionName: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.session_name)

    open var bible: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.bible)
    open var reference: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.reference)
    open var month: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.month)
    open var day: TextView? = itemView.findViewById(sdk.chat.demo.pre.R.id.day)

    open var format: DateFormat? = null

    open val btnCopy: ImageView? =
        itemView.findViewById(sdk.chat.demo.pre.R.id.btn_pray)

    open val dm = DisposableMap()


    //    open var explore1: View? = itemView.findViewById(sdk.chat.demo.pre.R.id.explore1)
//    open var explore2: View? = itemView.findViewById(sdk.chat.demo.pre.R.id.explore2)
//    open var explore3: View? = itemView.findViewById(sdk.chat.demo.pre.R.id.explore3)
    val exploreView: Map<String, TextView> = mapOf(
        "explore0" to itemView.findViewById<TextView>(sdk.chat.demo.pre.R.id.explore1),
        "explore1" to itemView.findViewById<TextView>(sdk.chat.demo.pre.R.id.explore2),
        "explore2" to itemView.findViewById<TextView>(sdk.chat.demo.pre.R.id.explore3)
    )

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
        Log.e("bindImage", t.message.id.toString())
        loadImage(t)
        progressView?.actionButton?.setOnClickListener(View.OnClickListener {
            actionButtonPressed(t)
        })
        progressView?.bringToFront()

        bubble?.let {
            it.isSelected = isSelected
        }

        setText(t.text, t.enableLinkify())

        time?.let {
            UIModule.shared().timeBinder.bind(it, t)
        }

        messageIcon?.let {
            UIModule.shared().iconBinder.bind(it, t)
        }

//        bindReadStatus(t)
//        bindSendStatus(t)
//        bindProgress(t)

        btnCopy?.setOnClickListener {
            ToastHelper.show(it.context, "copy....");
        }


        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
        feedback?.let {
            it.text = t.message.stringForKey("feedback");
        }
        var topic = threadHandler.getSessionName(t.message.threadId)
        if (topic != null) {
            sessionContainer?.visibility = View.VISIBLE
            sessionName?.let {
                it.text = topic
            }
        } else {
            sessionContainer?.visibility = View.GONE
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
            text?.setTextIsSelectable(true);
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
//        this.style = style
//        if (direction == MessageDirection.Incoming) {
////            applyIncomingStyle(style)
//        } else {
////            applyOutgoingStyle(style)
//        }
    }

    open fun applyIncomingStyle(style: MessagesListStyle) {

        progressView?.let {
            it.setTintColor(style.incomingTextColor, style.incomingDefaultBubbleColor)
        }

        bubble?.let {
            it.setPadding(
                style.incomingDefaultBubblePaddingLeft,
                style.incomingDefaultBubblePaddingTop,
                style.incomingDefaultBubblePaddingRight,
                style.incomingDefaultBubblePaddingBottom
            )
            ViewCompat.setBackground(it, style.getIncomingBubbleDrawable())

            it.background = DrawableUtil.getMessageSelector(
                it.context,
                R.attr.incomingDefaultBubbleColor,
                R.attr.incomingDefaultBubbleSelectedColor,
                R.attr.incomingDefaultBubblePressedColor,
                R.attr.incomingBubbleDrawable
            )
        }

        text?.let {
            it.setTextColor(style.incomingTextColor)
            it.setTextSize(0, style.incomingTextSize.toFloat())
            it.setTypeface(it.typeface, style.incomingTextStyle)
            it.autoLinkMask = style.textAutoLinkMask
            it.setLinkTextColor(style.incomingTextLinkColor)
            configureLinksBehavior(it)
        }

        time?.let {
            it.setTextColor(style.incomingTimeTextColor)
            it.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                style.incomingTimeTextSize.toFloat()
            )
            it.setTypeface(it.typeface, style.incomingTimeTextStyle)
        }

        imageOverlay?.let {
            ViewCompat.setBackground(it, style.getIncomingImageOverlayDrawable())
        }
    }

    open fun applyOutgoingStyle(style: MessagesListStyle) {

        progressView?.let {
            it.setTintColor(style.outcomingTextColor, style.outcomingDefaultBubbleColor)
        }

//        bubble?.let {
//            it.setPadding(
//                style.outcomingDefaultBubblePaddingLeft,
//                style.outcomingDefaultBubblePaddingTop,
//                style.outcomingDefaultBubblePaddingRight,
//                style.outcomingDefaultBubblePaddingBottom
//            )
//            ViewCompat.setBackground(it, style.getOutcomingBubbleDrawable())
//
//            it.background = DrawableUtil.getMessageSelector(
//                it.context,
//                R.attr.outcomingDefaultBubbleColor,
//                R.attr.outcomingDefaultBubbleSelectedColor,
//                R.attr.outcomingDefaultBubblePressedColor,
//                R.attr.outcomingBubbleDrawable
//            )
//        }

        text?.let {
//            it.setTextColor(style.outcomingTextColor)
//            it.setTextSize(0, style.outcomingTextSize.toFloat())
            it.setTypeface(it.typeface, style.outcomingTextStyle)
            it.autoLinkMask = style.textAutoLinkMask
            it.setLinkTextColor(style.outcomingTextLinkColor)
            configureLinksBehavior(it)
        }

        time?.let {
            it.setTextColor(style.outcomingTimeTextColor)
            it.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                style.outcomingTimeTextSize.toFloat()
            )
            it.setTypeface(it.typeface, style.outcomingTimeTextStyle)
        }

        imageOverlay?.let {
            ViewCompat.setBackground(it, style.getOutcomingImageOverlayDrawable())
        }
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

    open fun loadImage(holder: T) {
        val imageHolder: ImageHolder? = holder as? ImageHolder;
        if (imageHolder == null) {
            return
        }
        var action = imageHolder.action;
        var imageDaily: ImageDaily? = imageHolder.getImageDaily()
        if (imageDaily == null) {
            return
        }
        if (action == GWThreadHandler.action_bible_pic) {
            bible?.text = imageDaily?.scripture
        } else if (action == GWThreadHandler.action_daily_gw || action == GWThreadHandler.action_daily_gw_pray) {
            day?.text = if (imageDaily.date != null) imageDaily.date.substring(8) else ""
            month?.text = if (imageDaily.date != null) imageDaily.date.substring(0, 7) else ""
            bible?.text = imageDaily.scripture
            reference?.text = imageDaily.reference
        }
        Glide.with(image!!)
            .load(imageDaily.backgroundUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(sdk.chat.demo.pre.R.drawable.icn_200_image_message_placeholder) // 占位图
            .error(sdk.chat.demo.pre.R.drawable.icn_200_image_message_error) // 错误图
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