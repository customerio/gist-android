package build.gist.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.startActivity
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
    private var currentMessage: Message? = null
    private var currentRoute: String? = null
    private var firstLoad: Boolean = true
    var listener: GistViewListener? = null

    init {
        engineWebView.listener = this
        this.addView(engineWebView)
    }

    fun setup(message: Message) {
        currentMessage = message
        currentMessage?.let { message ->
            val engineWebConfiguration = EngineWebConfiguration(
                organizationId = GistSdk.getInstance().organizationId,
                messageId = message.messageId,
                instanceId = message.instanceId,
                endpoint = BuildConfig.GIST_API_URL,
                properties = message.properties
            )
            engineWebView.setup(engineWebConfiguration)
        }
    }

    override fun tap(action: String, system: Boolean) {
        currentMessage?.let { message ->
            currentRoute?.let { route ->
                when {
                    action == "gist://close" -> {
                        Log.i(GIST_TAG, "Dismissing from action: $action")
                        dismissMessage(message, route)
                    }
                    system -> {
                        try {
                            Log.i(GIST_TAG, "Dismissing from system action: $action")
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(action)
                            startActivity(context, intent, null)
                            dismissMessage(message, route)
                        } catch (e: ActivityNotFoundException) {
                            Log.i(GIST_TAG, "System action not handled")
                        }
                    }
                    else -> {
                        Log.i(GIST_TAG, "Action selected: $action")
                    }
                }
                GistSdk.gistAnalytics.actionPerformed(message = message, route = route, system = system)
                GistSdk.handleGistAction(message = message, currentRoute = route, action = action)
            }
        }
    }

    private fun dismissMessage(message: Message, route: String) {
        GistSdk.handleGistClosed(message)
        GistSdk.gistAnalytics.messageDismissed(message = message, route = route)
    }

    override fun routeError(route: String) {
        currentMessage?.let { message ->
            GistSdk.handleGistError(message)
        }
    }

    override fun routeLoaded(route: String) {
        currentRoute = route
        if (firstLoad) {
            currentMessage?.let { message ->
                GistSdk.handleGistLoaded(message)
                GistSdk.gistAnalytics.messageLoaded(message = message, route = route)
            }
        }
    }

    override fun error() {
        currentMessage?.let { message ->
            GistSdk.handleGistError(message)
        }
    }

    override fun bootstrapped() {}
    override fun routeChanged(newRoute: String) {}
    override fun sizeChanged(width: Double, height: Double) {
        listener?.onGistViewSizeChanged(getSizeBasedOnDPI(width.toInt()), getSizeBasedOnDPI(height.toInt()))
    }

    private fun getSizeBasedOnDPI(size: Int): Int {
        return size * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
    }
}

interface GistViewListener {
    fun onGistViewSizeChanged(width: Int, height: Int) {}
}