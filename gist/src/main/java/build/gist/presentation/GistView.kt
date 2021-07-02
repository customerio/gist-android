package build.gist.presentation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import build.gist.BuildConfig
import build.gist.data.model.Message
import build.gist.data.model.engine.EngineWebConfiguration
import build.gist.presentation.engine.EngineWebView
import build.gist.presentation.engine.EngineWebViewListener

class GistView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), EngineWebViewListener {

    private var engineWebView: EngineWebView = EngineWebView(context)

    init {
        engineWebView.listener = this
        this.addView(engineWebView)
    }

    fun setup(message: Message) {
        var engineWebConfiguration = EngineWebConfiguration(
            organizationId = GistSdk.getInstance().organizationId,
            messageId = message.messageId,
            instanceId = message.instanceId,
            endpoint = BuildConfig.GIST_API_URL,
            properties = message.properties
        )
        engineWebView.setup(engineWebConfiguration)
    }

    override fun bootstrapped() {
    }

    override fun tap(action: String, system: Boolean) {
    }

    override fun routeChanged(newRoute: String) {
    }

    override fun routeError(route: String) {
    }

    override fun routeLoaded(route: String) {
    }

    override fun sizeChanged(width: Double, height: Double) {
    }

    override fun error() {
    }

}