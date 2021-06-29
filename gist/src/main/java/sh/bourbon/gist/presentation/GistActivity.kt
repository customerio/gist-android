package sh.bourbon.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import sh.bourbon.gist.R
import sh.bourbon.gist.data.model.Message
import sh.bourbon.gist.databinding.ActivityGistBinding

class GistActivity : AppCompatActivity(), GistListener {

    private lateinit var binding: ActivityGistBinding

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //engineView.setup(GistSdk.BOURBON_ENGINE_ID, true)
        //gistWeb.loadUrl("https://app.dev.gist.build/live-preview/?options=eyJvcmdhbml6YXRpb25JZCI6IjVhNmYwMWE3LTY4NDctNDU1NC04ZTcxLTM2MTBhMjdlZDIwMSIsIm1lc3NhZ2VJZCI6InZvdWNoZXItZHJhZnQiLCJlbmRwb2ludCI6Imh0dHBzOi8vYXBpLmRldi5naXN0LmJ1aWxkIiwibGl2ZVByZXZpZXciOnRydWUsInByb3BlcnRpZXMiOm51bGx9#/")
        binding.gistWeb.loadUrl("https://code.gist.build/renderer/0.0.3/index.html?options=eyJvcmdhbml6YXRpb25JZCI6ImM2ZmY5MmI5LTU2MDctNDY1NS05MjY1LWYyNTg4ZjdlM2I1OCIsIm1lc3NhZ2VJZCI6InZlcnNpb24tMi0wLWRyYWZ0IiwiZW5kcG9pbnQiOiJodHRwczovL2FwaS5naXN0LmJ1aWxkIiwibGl2ZVByZXZpZXciOnRydWUsInByb3BlcnRpZXMiOm51bGx9")
        binding.gistWeb.settings.javaScriptEnabled = true
        binding.gistWeb.settings.allowFileAccess = true
        binding.gistWeb.settings.allowContentAccess = true
        binding.gistWeb.settings.domStorageEnabled = true
        binding.gistWeb.setBackgroundColor(Color.TRANSPARENT)
        binding.gistWeb.evaluateJavascript("window.parent.postMessage = function(message) {webkit.messageHandlers.gist.postMessage(message)}", null);

        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_in);
        animation.startDelay = 1000 // Delay animation to avoid TextureView jitter
        animation.setTarget(binding.gistView)
        animation.start()
    }

    override fun onResume() {
        super.onResume()

        GistSdk.addListener(this)
    }

    override fun onPause() {
        GistSdk.removeListener(this)

        super.onPause()
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_out)
        animation.setTarget(binding.gistView)
        animation.start()
        animation.doOnEnd {
            super.finish()
        }
    }

    override fun onMessageShown(message: Message) {
    }

    override fun onMessageDismissed(message: Message) {
        // Message was cancelled, close activity
        finish()
    }

    override fun onError(message: Message) {
        // Error displaying message, close activity
        finish()
    }

    override fun onAction(currentRoute: String, action: String) {
    }
}
