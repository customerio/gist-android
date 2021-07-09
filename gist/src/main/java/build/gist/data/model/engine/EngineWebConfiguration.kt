package build.gist.data.model.engine

internal data class EngineWebConfiguration(
    val organizationId: String,
    val messageId: String,
    val instanceId: String,
    val endpoint: String,
    val livePreview: Boolean = false,
    val properties: Map<String, Any?>? = null
)