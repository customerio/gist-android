package build.gist.presentation

import android.content.Intent
import android.util.Log
import build.gist.data.model.Message
import com.google.gson.Gson

internal class GistModalManager: GistListener {
    private var currentMessage: Message? = null

    init {
        GistSdk.addListener(this)
    }

    internal fun showModalMessage(message: Message): Boolean {
        if (currentMessage != null) {
            Log.i(GIST_TAG, "Message ${message.messageId} not shown, activity is already showing.")
            return false
        }

        Log.i(GIST_TAG, "Showing message: ${message.messageId}")
        currentMessage = message

        val intent = GistModalActivity.newIntent(GistSdk.application)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(GIST_MESSAGE_INTENT, Gson().toJson(message));
        GistSdk.application.startActivity(intent)
        return true
    }

    internal fun dismissActiveMessage() {
        currentMessage?.let { message ->
            GistSdk.handleGistClosed(message = message)
        } ?: run {
            Log.i(GIST_TAG, "No modal messages to dismiss.")
        }
    }

    override fun onMessageDismissed(message: Message) {
        if (message.instanceId == currentMessage?.instanceId) {
            currentMessage = null
        }
    }

    override fun onError(message: Message) {
        if (message.instanceId == currentMessage?.instanceId) {
            currentMessage = null
        }
    }

    override fun onMessageShown(message: Message) {}

    override fun onAction(message: Message, currentRoute: String, action: String) {}
}