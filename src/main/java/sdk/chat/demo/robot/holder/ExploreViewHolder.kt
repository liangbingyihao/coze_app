package sdk.chat.demo.robot.holder

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.functions.Predicate
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX

open class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var contentLoadingProgressBar: ContentLoadingProgressBar =
        itemView.findViewById<ContentLoadingProgressBar?>(
            R.id.pb_progress
        )
    val exploreView: Map<String, TextView> = mapOf(
        "explore0" to itemView.findViewById<TextView>(R.id.explore1),
        "explore1" to itemView.findViewById<TextView>(R.id.explore2),
        "explore2" to itemView.findViewById<TextView>(R.id.explore3)
    )
    open val dm = DisposableMap()
    var loading: Boolean = false

    fun bind(loading: Boolean,aiExplore: ExploreHolder) {
        bindListeners(aiExplore)
        // 根据header类型处理
        this.loading = loading
        contentLoadingProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        bindExplore(aiExplore)
    }

    open fun bindExplore(aiExplore: ExploreHolder) {
        bindListeners(aiExplore)
        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler

        Log.e("AIExplore", "bindExplore:" + aiExplore.message?.id)
        var i = 0
        var aiExplore: AIExplore? = aiExplore.aiExplore
        val aiFeedback = GWMsgHandler.getAiFeedback(aiExplore?.message)
//        Log.d("sending","threadHandler.isSendingMsg:${threadHandler.pendingMsgId()},aiExplore:${aiExplore?.message?.id},${aiExplore?.itemList?.size}");
        while (i < 3) {
            var v: TextView = exploreView.getValue("explore$i")
            if (aiExplore != null && i < aiExplore.itemList.size) {
//                Log.d("sending", "bindExplore:visible $i");
                var data = aiExplore.itemList[i]
                v.visibility = View.VISIBLE
                v.text = data.text
                if (data.action == AIExplore.ExploreItem.action_bible_pic) {
                    var bible = aiFeedback?.feedback?.bible ?: ""
                    if (bible.isEmpty()) {
                        v.visibility = View.GONE
                        continue
                    }
//                    bible = aiFeedback?.feedbackText ?:""
                    v.setOnClickListener { view ->
                        // 可以使用view参数
                        if (aiFeedback != null && !bible.isEmpty()) {
                            view as TextView
                            threadHandler.sendExploreMessage(
                                view.text.toString().trim(),
                                aiExplore.message,
                                data.action,
                                "${aiFeedback.feedback.tag}|${bible}"
                            ).subscribe();
                        } else {
                            Toast.makeText(v.context, "没有经文...", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else if (data.action == AIExplore.ExploreItem.action_input_prompt) {
                    var event = NetworkEvent.messageInputPrompt(data.text,data.getParamsStr())
                    v.setOnClickListener { view ->
                        ChatSDK.events().source().accept(event)
                    }
                } else {
                    v.setOnClickListener { view ->
                        view as TextView // 安全转换
                        threadHandler.sendExploreMessage(
                            view.text.toString().trim(),
                            aiExplore.message,
                            data.action,
                            data.getParamsStr()
                        ).subscribe();
                    }
                }
            } else {
//                Log.d("sending", "bindExplore:gone $i");
                v.visibility = View.GONE
            }
            ++i
        }
    }

    fun filterById(id: Long?): Predicate<NetworkEvent?> {
        Log.e("AIExplore", "ExploreViewHolder.MessageUpdated:" + id)
        return Predicate { networkEvent: NetworkEvent? -> networkEvent?.message?.id == id }
    }

    open fun bindListeners(t: ExploreHolder) {
        dm.dispose()
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageUpdated))
                .filter(filterById(t.message.id))
                .subscribe {
                    Log.e("AIExplore", "ExploreViewHolder.MessageUpdated:" + t.message.id)
                    RX.main().scheduleDirect {
                        t.aiExplore = null
                        bind(this.loading,t)
                    }
                })
    }
}