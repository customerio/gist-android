package build.gist.data.model
import java.util.*

enum class MessagePosition(val position: String) {
    TOP("top"),
    CENTER("center"),
    BOTTOM("bottom")
}

data class GistProperties(
    val routeRule: String?,
    val elementId: String?,
    val position: MessagePosition
)

data class Message(
    val messageId: String = "",
    val instanceId: String = UUID.randomUUID().toString(),
    val queueId: String? = null,
    val properties: Map<String, Any?>? = null
)

class GistMessageProperties {
    companion object {
        fun getGistProperties(message: Message): GistProperties {
            var routeRule: String = ""
            var elementId: String = ""

            message.properties?.let { properties ->
                (properties["routeRule"] as String).let { rule ->
                    routeRule = rule
                }
                (properties["elementId"] as String).let { id ->
                    elementId = id
                }
            }
            return GistProperties(routeRule = routeRule, elementId = elementId, MessagePosition.CENTER)
        }
    }
}
