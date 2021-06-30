package build.gist.presentation

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleObserver
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import build.gist.R
import build.gist.databinding.ViewGistBinding

object GistJSInterface {
    @JavascriptInterface
    fun postMessage(message: String){
        Log.i("webview", message)
    }
}

class GistView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), LifecycleObserver {

    private lateinit var binding: ViewGistBinding

    init {
        View.inflate(context, R.layout.view_gist, this)
        binding = ViewGistBinding.inflate(LayoutInflater.from(context), this, true)
        binding.webView.visibility = INVISIBLE
    }

    fun setup(id: String) {
        binding.webView.visibility = VISIBLE
        binding.webView.loadUrl("https://code.gist.build/renderer/0.0.3/index.html?options=eyJvcmdhbml6YXRpb25JZCI6ImM2ZmY5MmI5LTU2MDctNDY1NS05MjY1LWYyNTg4ZjdlM2I1OCIsIm1lc3NhZ2VJZCI6InZlcnNpb24tMi0wIiwiZW5kcG9pbnQiOiJodHRwczovL2FwaS5naXN0LmJ1aWxkIiwibGl2ZVByZXZpZXciOmZhbHNlLCJwcm9wZXJ0aWVzIjpudWxsfQ==")
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.allowContentAccess = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
        binding.webView.addJavascriptInterface(GistJSInterface, "appInterface")

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                view.loadUrl("javascript:window.parent.postMessage = function(message) {window.appInterface.postMessage(JSON.stringify(message))}")
            }
        }
    }
}