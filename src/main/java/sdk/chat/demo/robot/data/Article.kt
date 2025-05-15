package sdk.chat.demo.robot.data

data class Article(
    val id: String,
    val time: String,      // 时间轴显示的文字（如 "08:30"）
    val title: String,     // 5字左右的标题
    val content: String    // 正文文本
)