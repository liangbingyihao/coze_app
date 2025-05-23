package sdk.chat.demo.robot.adpter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

abstract class GenericMenuAdapter<T, VH : GenericMenuAdapter.ViewHolder<T>>(
    private val context: Context,
    private var items: List<T>,
    private var selectedPosition: Int
) : BaseAdapter() {

    abstract class ViewHolder<T> {  // 为 ViewHolder 单独声明泛型
        abstract fun bind(item: T, position: Int, isSelected: Boolean)
    }

    // 获取布局资源ID
    abstract fun getLayoutRes(): Int

    // 创建ViewHolder
    abstract fun createViewHolder(view: View): VH

    fun updateData(newItems: List<T>, newSelectedPos: Int) {
        this.items = newItems
        this.selectedPosition = newSelectedPos
        notifyDataSetChanged()
    }

    fun updateSelectedPosition(position: Int) {
        if(selectedPosition!=position){
            selectedPosition = position
            notifyDataSetChanged() // 通知更新视图
        }
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): T = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(getLayoutRes(), parent, false).apply {
                tag = createViewHolder(this)
            }

        val holder = view.tag as VH
        holder.bind(getItem(position), position, position == selectedPosition)
        return view
    }

}