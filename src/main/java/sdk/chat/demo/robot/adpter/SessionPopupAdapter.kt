package sdk.chat.demo.robot.adpter

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.data.ArticleSession

class SessionPopupAdapter(
    context: Context,
    items: List<ArticleSession>,
    selectedPosition: Int
) : GenericMenuAdapter<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>(
    context,
    items,
    selectedPosition
) {
    override fun getLayoutRes(): Int {
        return R.layout.item_session_spinner_dropdown
    }

    override fun createViewHolder(view: View): SessionItemViewHolder {
        return SessionItemViewHolder(view)
    }

    inner class SessionItemViewHolder(view: View) :
        GenericMenuAdapter.ViewHolder<ArticleSession>() {
        private val titleView: TextView = view.findViewById(R.id.spinner_dropdown_item_text)

        override fun bind(
            item: ArticleSession,
            position: Int,
            isSelected: Boolean
        ) {
            val session: ArticleSession? = getItem(position)
            if (session != null) {
                titleView.setText(session.title)
                // 设置选中状态
                if (isSelected) {
                    titleView.setTextColor(
                        ContextCompat.getColor(
                            titleView.context,
                            R.color.item_text_selected
                        )
                    )
                } else {
                    titleView.setTextColor(
                        ContextCompat.getColor(
                            titleView.context,
                            R.color.item_text_normal
                        )
                    )
                }
            }
        }
    }
}