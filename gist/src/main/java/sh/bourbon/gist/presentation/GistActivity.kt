package sh.bourbon.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlinx.android.synthetic.main.activity_gist.*
import sh.bourbon.gist.R
import sh.bourbon.gist.data.model.Message

class GistActivity : AppCompatActivity(), GistListener {
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist)

        //engineView.setup(GistSdk.BOURBON_ENGINE_ID, true)
        //gistWeb.loadUrl("https://app.dev.gist.build/live-preview/?options=eyJvcmdhbml6YXRpb25JZCI6IjVhNmYwMWE3LTY4NDctNDU1NC04ZTcxLTM2MTBhMjdlZDIwMSIsIm1lc3NhZ2VJZCI6InZvdWNoZXItZHJhZnQiLCJlbmRwb2ludCI6Imh0dHBzOi8vYXBpLmRldi5naXN0LmJ1aWxkIiwibGl2ZVByZXZpZXciOnRydWUsInByb3BlcnRpZXMiOm51bGx9#/")
        gistWeb.loadUrl("http://app.local.gist.build:8000/live-preview/?options=eyJvcmdhbml6YXRpb25JZCI6IjViNDk4NDMwLTY0ZTMtNGUxYS04MjVjLWVlYTFlMTk3MzUwOCIsIm1lc3NhZ2VJZCI6ImV1cm9wZWFuLWRlc3RpbmF0aW9ucyIsImluc3RhbmNlSWQiOiIwZWZjNjM4Yi1jZDkwLTQzN2ItYTY3Mi05OTg4NTEzNzZlNTQiLCJlbmRwb2ludCI6Imh0dHA6Ly9hcGkubG9jYWwuZ2lzdC5idWlsZDo4MyIsImxpdmVQcmV2aWV3IjpmYWxzZX0=")
        gistWeb.settings.javaScriptEnabled = true
        gistWeb.settings.allowFileAccess = true
        gistWeb.settings.allowContentAccess = true
        gistWeb.settings.domStorageEnabled = true
        gistWeb.setBackgroundColor(Color.TRANSPARENT)

        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_in);
        animation.startDelay = 1000 // Delay animation to avoid TextureView jitter
        animation.setTarget(gistView)
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
        animation.setTarget(gistView)
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
