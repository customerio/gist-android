package build.gist.presentation.engine

import android.util.Log
import android.webkit.JavascriptInterface
import build.gist.presentation.GIST_TAG
import com.google.gson.Gson

internal data class EngineWebMessage(
    val gist: EngineWebEvent
)

internal data class EngineWebEvent(
    val instanceId: String,
    val method: String,
    val parameters: Map<String, Any>? = null
)

class EngineWebViewInterface constructor(
    listener: EngineWebViewListener
) {
    private var listener: EngineWebViewListener = listener

    @JavascriptInterface
    fun postMessage(message: String) {
        Log.i(GIST_TAG, message)
        var event = Gson().fromJson(message, EngineWebMessage::class.java)
        event.gist.parameters?.let { eventParameters ->
            when (event.gist.method) {
                "bootstrapped" -> listener.bootstrapped()
                "routeLoaded" -> {
                    (eventParameters["route"] as String).let { route -> listener.routeLoaded(route) }
                }
                "routeChanged" -> {
                    (eventParameters["route"] as String).let { route -> listener.routeChanged(route) }
                }
                "routeError" -> {
                    (eventParameters["route"] as String).let { route -> listener.routeError(route) }
                }
                "sizeChanged" -> {
                    (eventParameters["width"] as Double).let { width ->
                        (eventParameters["height"] as Double).let { height ->
                            listener.sizeChanged(width = width, height = height)
                        }
                    }
                }
                "tap" -> {
                    (eventParameters["action"] as String).let { action ->
                        (eventParameters["system"] as Boolean).let { system ->
                            listener.tap(action = action, system = system)
                        }
                    }
                }
                "error" -> listener.error()
            }
        }
    }
}