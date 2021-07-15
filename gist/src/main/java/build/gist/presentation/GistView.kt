package build.gist.presentation

import android.R.attr
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.util.AttributeSet
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.startActivity
import build.gist.BuildConfig
import build.gist.data.model.Message
import build.gist.data.model.engine.EngineWebConfiguration
import build.gist.presentation.engine.EngineWebView
import build.gist.presentation.engine.EngineWebViewListener
import com.google.gson.Gson
import java.net.URI
import java.nio.charset.StandardCharsets


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
        engineWebView.alpha = 0.0f
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
        var shouldLogAction = true
        currentMessage?.let { message ->
            currentRoute?.let { route ->
                GistSdk.handleGistAction(message = message, currentRoute = route, action = action)
                when {
                    action.startsWith("gist://") -> {
                        val gistAction = URI(action)
                        val urlQuery = UrlQuerySanitizer(action)
                        when (gistAction.host) {
                            "close" -> {
                                shouldLogAction = false
                                Log.i(GIST_TAG, "Dismissing from action: $action")
                                GistSdk.handleGistClosed(message)
                            }
                            "loadPage" -> {
                                val url = urlQuery.getValue("url")
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(url)
                                startActivity(context, intent, null)
                            }
                            "showMessage" -> {
                                GistSdk.handleGistClosed(message)
                                val messageId = urlQuery.getValue("messageId")
                                val propertiesBase64 = urlQuery.getValue("properties")
                                val parameterBinary = Base64.decode(propertiesBase64, Base64.DEFAULT)
                                val parameterString = String(parameterBinary, StandardCharsets.UTF_8)
                                val map: Map<String, Any> = HashMap()
                                val properties = Gson().fromJson(parameterString, map.javaClass)
                                GistSdk.getInstance().showMessage(
                                    Message(messageId = messageId, properties = properties)
                                )
                            }
                            else -> {
                                shouldLogAction = false
                                Log.i(GIST_TAG, "Gist action unhandled")
                            }
                        }
                    }
                    system -> {
                        try {
                            shouldLogAction = false
                            GistSdk.gistAnalytics.actionPerformed(message = message, route = route, system = system)
                            Log.i(GIST_TAG, "Dismissing from system action: $action")
                            GistSdk.handleGistClosed(message)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(action)
                            startActivity(context, intent, null)
                        } catch (e: ActivityNotFoundException) {
                            Log.i(GIST_TAG, "System action not handled")
                        }
                    }
                }
                if (shouldLogAction) {
                    Log.i(GIST_TAG, "Action selected: $action")
                    GistSdk.gistAnalytics.actionPerformed(message = message, route = route, false)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        currentMessage?.let { currentMessage ->
            currentRoute?.let { currentRoute ->
                GistSdk.gistAnalytics.messageDismissed(message = currentMessage, route = currentRoute)
            }
        }
        super.onDetachedFromWindow()
    }

    override fun routeError(route: String) {
        currentMessage?.let { message ->
            GistSdk.handleGistError(message)
        }
    }

    override fun routeLoaded(route: String) {
        currentRoute = route
        if (firstLoad) {
            engineWebView.alpha = 1.0f
            currentMessage?.let { message ->
                GistSdk.gistAnalytics.messageLoaded(message = message, route = route)
                GistSdk.handleGistLoaded(message)
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