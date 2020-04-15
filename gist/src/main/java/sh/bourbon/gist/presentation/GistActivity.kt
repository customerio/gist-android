package sh.bourbon.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlinx.android.synthetic.main.activity_gist.*
import sh.bourbon.gist.R


class GistActivity : AppCompatActivity(), GistListener {
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist)

        engineView.setEngineId(GistSdk.BOURBON_ENGINE_ID)

        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_in);
        animation.startDelay = 1000 // Delay animation to avoid TextureView jitter
        animation.setTarget(engineView)
        animation.start()
    }

    override fun onResume() {
        super.onResume()

        GistSdk.addListener(this)
    }

    override fun onPause() {
        GistSdk.removeListener(this)

        super.onPause()
    }

    override fun finish() {
        val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_out)
        animation.setTarget(engineView)
        animation.start()
        animation.doOnEnd {
            super.finish()
        }
    }

    override fun onMessageShown(messageId: String) {
    }

    override fun onMessageDismissed(messageId: String) {
        // Message was cancelled, close activity
        finish()
    }

    override fun onAction(action: String) {
    }

    override fun onError(messageId: String) {
        // Error displaying message, close activity
        finish()
    }
}
