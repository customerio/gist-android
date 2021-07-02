package build.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import build.gist.R
import build.gist.data.model.GistMessageProperties
import build.gist.data.model.Message
import build.gist.databinding.ActivityGistBinding
import build.gist.presentation.engine.EngineWebMessage
import com.google.gson.Gson

const val GIST_MESSAGE_INTENT: String = "GIST_MESSAGE"

class GistModalActivity : AppCompatActivity(), GistListener {
    private lateinit var binding: ActivityGistBinding
    private var currentMessage: Message? = null

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistModalActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val messageStr = this.intent.getStringExtra(GIST_MESSAGE_INTENT)
        Gson().fromJson(messageStr, Message::class.java)?.let { messageObj ->
            currentMessage = messageObj
            currentMessage?.let { message ->
                binding.gistView.setup(message)
                val animation = AnimatorInflater.loadAnimator(this, R.animator.animate_in)
                animation.setTarget(binding.modalGistView)
                animation.start()
            }
        } ?: run {
            finish()
        }
    }

    /*
        var params = this.layoutParams
        params.height = height.toInt()
        params.width = width.toInt()
        this.layoutParams = params
     */

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
        animation.setTarget(binding.modalGistView)
        animation.start()
        animation.doOnEnd {
            super.finish()
            currentMessage?.let { message ->
                GistSdk.handleGistClosed(message);
            }
        }
    }

    override fun onMessageShown(message: Message) {
    }

    override fun onMessageDismissed(message: Message) {
        GistSdk.dismissMessage()
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
