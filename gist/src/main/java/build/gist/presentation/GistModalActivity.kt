package build.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import build.gist.R
import build.gist.data.model.GistMessageProperties
import build.gist.data.model.Message
import build.gist.data.model.MessagePosition
import build.gist.databinding.ActivityGistBinding
import com.google.gson.Gson

const val GIST_MESSAGE_INTENT: String = "GIST_MESSAGE"

class GistModalActivity : AppCompatActivity(), GistListener, GistViewListener {
    private lateinit var binding: ActivityGistBinding
    private var currentMessage: Message? = null

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistModalActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GistSdk.addListener(this)
        binding = ActivityGistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val messageStr = this.intent.getStringExtra(GIST_MESSAGE_INTENT)
        Gson().fromJson(messageStr, Message::class.java)?.let { messageObj ->
            currentMessage = messageObj
            currentMessage?.let { message ->
                binding.gistView.listener = this
                binding.gistView.setup(message)
                when (GistMessageProperties.getGistProperties(message).position) {
                    MessagePosition.CENTER -> binding.modalGistViewLayout.setVerticalGravity(Gravity.CENTER_VERTICAL)
                    MessagePosition.BOTTOM -> binding.modalGistViewLayout.setVerticalGravity(Gravity.BOTTOM)
                    else -> binding.modalGistViewLayout.setVerticalGravity(Gravity.TOP)
                }
            }
        } ?: run {
            finish()
        }
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
        animation.setTarget(binding.modalGistViewLayout)
        animation.start()
        animation.doOnEnd {
            super.finish()
            currentMessage?.let { message ->
                GistSdk.removeListener(this)
                GistSdk.handleGistClosed(message)
            }
        }
    }

    override fun onMessageShown(message: Message) {
        runOnUiThread {
            val animation = AnimatorInflater.loadAnimator(super.getBaseContext(), R.animator.animate_in)
            animation.setTarget(binding.modalGistViewLayout)
            animation.start()
        }
    }

    override fun onMessageDismissed(message: Message) {
        currentMessage?.let { currentMessage ->
            if (currentMessage.instanceId == message.instanceId) {
                finish()
            }
        }
    }

    override fun onGistViewSizeChanged(width: Int, height: Int) {
        Log.i(GIST_TAG, "Message Size Changed")
        val params = binding.gistView.layoutParams
        params.height = height
        params.width = width
        runOnUiThread {
            binding.modalGistViewLayout.updateViewLayout(binding.gistView, params)
        }
    }

    override fun onError(message: Message) {
        finish()
    }

    override fun embedMessage(message: Message, elementId: String) {}

    override fun onAction(message: Message, currentRoute: String, action: String) {}
}
