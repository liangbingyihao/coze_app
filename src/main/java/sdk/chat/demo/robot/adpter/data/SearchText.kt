package sdk.chat.demo.robot.adpter.data

sealed class SearchText {
    data class Header(val title: String) : SearchText()
    data class Tag(val id: String, val text: String) : SearchText()
}

//data class SearchText(
//    val id: String,          // 唯一标识
//    val text: String,        // 显示文本
////    val color: Int,          // 背景色
////    var isSelected: Boolean = false // 选中状态
//)
