package sdk.chat.demo.robot.api.model

import sdk.chat.core.session.ChatSDK

data class LogEntry(
    val timestamp: Long,
    val kv: List<KeyValuePair>
)

data class KeyValuePair(
    val key: String,
    val value: String
)

data class LogRequest(
    val topic: String,
    val logs: List<LogEntry>
)

fun createBatchLogsRequest(topic:String,description: String,uid:String,logs:String): LogRequest {
    return LogRequest(
        topic = topic,
        logs = mutableListOf(
            LogEntry(
                timestamp = System.currentTimeMillis()/1000,
                kv = mutableListOf(
                    KeyValuePair("des", description),
                    KeyValuePair("uid", uid),
                    KeyValuePair("logData", logs)
                )
            )
        )
    )
}