package sh.bourbon.gist.data.model

data class Message(
    val messageId: String,
    val queueId: String? = null,
    val properties: Map<String, Any?>? = null
)