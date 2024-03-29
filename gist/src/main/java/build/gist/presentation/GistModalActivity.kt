package build.gist.presentation

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import build.gist.R
import build.gist.data.model.GistMessageProperties
import build.gist.data.model.Message
import build.gist.data.model.MessagePosition
import build.gist.databinding.ActivityGistBinding
import build.gist.utilities.ElapsedTimer
import com.google.gson.Gson

const val GIST_MESSAGE_INTENT: String = "GIST_MESSAGE"
const val GIST_MODAL_POSITION_INTENT: String = "GIST_MODAL_POSITION"

class GistModalActivity : AppCompatActivity(), GistListener, GistViewListener {
    private lateinit var binding: ActivityGistBinding
    private var currentMessage: Message? = null
    private var messagePosition: MessagePosition = MessagePosition.CENTER
    private var elapsedTimer: ElapsedTimer = ElapsedTimer()

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GistModalActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GistSdk.addListener(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        binding = ActivityGistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val messageStr = this.intent.getStringExtra(GIST_MESSAGE_INTENT)
        val modalPositionStr = this.intent.getStringExtra(GIST_MODAL_POSITION_INTENT)
        Gson().fromJson(messageStr, Message::class.java)?.let { messageObj ->
            currentMessage = messageObj
            currentMessage?.let { message ->
                elapsedTimer.start("Displaying modal for message: ${message.messageId}")
                binding.gistView.listener = this
                binding.gistView.setup(message)
                messagePosition = if (modalPositionStr == null) {
                    GistMessageProperties.getGistProperties(message).position
                } else {
                    MessagePosition.valueOf(modalPositionStr.uppercase())
                }
                when (messagePosition) {
                    MessagePosition.CENTER -> binding.modalGistViewLayout.setVerticalGravity(Gravity.CENTER_VERTICAL)
                    MessagePosition.BOTTOM -> binding.modalGistViewLayout.setVerticalGravity(Gravity.BOTTOM)
                    MessagePosition.TOP -> binding.modalGistViewLayout.setVerticalGravity(Gravity.TOP)
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
        runOnUiThread {
            val animation = if (messagePosition == MessagePosition.TOP) {
                AnimatorInflater.loadAnimator(this, R.animator.animate_out_to_top)
            } else {
                AnimatorInflater.loadAnimator(this, R.animator.animate_out_to_bottom)
            }
            animation.setTarget(binding.modalGistViewLayout)
            animation.start()
            animation.doOnEnd {
                super.finish()
            }
        }
    }

    override fun onDestroy() {
        GistSdk.removeListener(this)
        GistSdk.dismissMessage()
        super.onDestroy()
    }

    override fun onMessageShown(message: Message) {
        runOnUiThread {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            binding.modalGistViewLayout.visibility = View.VISIBLE
            val animation = if (messagePosition == MessagePosition.TOP) {
                AnimatorInflater.loadAnimator(this, R.animator.animate_in_from_top)
            } else {
                AnimatorInflater.loadAnimator(this, R.animator.animate_in_from_bottom)
            }
            animation.setTarget(binding.modalGistViewLayout)
            animation.start()
            animation.doOnEnd {
                elapsedTimer.end()
            }
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
        runOnUiThread {
            binding.modalGistViewLayout.updateViewLayout(binding.gistView, params)
        }
    }

    override fun onError(message: Message) {
        finish()
    }

    override fun embedMessage(message: Message, elementId: String) {}

    override fun onAction(message: Message, currentRoute: String, action: String, name: String) {}
}
