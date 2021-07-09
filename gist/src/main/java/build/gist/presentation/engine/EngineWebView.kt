package build.gist.presentation.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.http.SslError
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.webkit.*
import android.widget.FrameLayout
import build.gist.BuildConfig
import build.gist.data.model.engine.EngineWebConfiguration
import build.gist.presentation.GIST_TAG
import com.google.gson.Gson
import java.io.UnsupportedEncodingException

internal class EngineWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), EngineWebViewListener {

    var listener: EngineWebViewListener? = null
    private var webView: WebView = WebView(context)

    init {
        this.addView(webView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(configuration: EngineWebConfiguration) {
        val jsonString = Gson().toJson(configuration)
        encodeToBase64(jsonString)?.let { options ->
            val messageUrl = "${BuildConfig.GIST_RENDERER}/index.html?options=${options}"
            Log.i(GIST_TAG, "Rendering message with URL: $messageUrl")
            webView.loadUrl(messageUrl)
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.settings.domStorageEnabled = true
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.addJavascriptInterface(EngineWebViewInterface(this), "appInterface")

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.loadUrl("javascript:window.parent.postMessage = function(message) {window.appInterface.postMessage(JSON.stringify(message))}")
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return !url.startsWith("https://code.gist.build")
                }

                override fun onReceivedError(view: WebView?, errorCod: Int, description: String, failingUrl: String?) {
                    listener?.error()
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    listener?.error()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    listener?.error()
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    listener?.error()
                }
            }
        }?: run {
            listener?.error()
        }
    }

    private fun encodeToBase64(text: String): String? {
        var data: ByteArray?
        try {
            data = text.toByteArray(charset("UTF-8"))
        } catch (ex: UnsupportedEncodingException) {
            Log.e(GIST_TAG, "Unsupported encoding exception")
            return null
        }
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    override fun bootstrapped() {
        listener?.bootstrapped()
    }

    override fun tap(action: String, system: Boolean) {
        listener?.tap(action, system)
    }

    override fun routeChanged(newRoute: String) {
        listener?.routeChanged(newRoute)
    }

    override fun routeError(route: String) {
        listener?.routeError(route)
    }

    override fun routeLoaded(route: String) {
        listener?.routeLoaded(route)
    }

    override fun sizeChanged(width: Double, height: Double) {
        listener?.sizeChanged(width, height)
    }

    override fun error() {
        listener?.error()
    }
}