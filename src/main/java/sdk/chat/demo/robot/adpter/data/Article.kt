package sdk.chat.demo.robot.adpter.data

data class Article(
    val id: String,
    val localId:Long,
    val day: String,
    val time: String,      // 时间轴显示的文字（如 "08:30"）
    var title: String,     // 5字左右的标题
    val content: String,    // 正文文本
    val colorTag: Int,    // 背景颜色
    val showDay: Boolean,    // 是否显示时间
    val isFirstDay: Boolean //是否第一天
)