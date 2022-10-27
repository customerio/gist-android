package build.gist_example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import build.gist.data.model.Message
import build.gist.data.model.MessagePosition
import build.gist.presentation.GIST_TAG
import build.gist.presentation.GistListener
import build.gist.presentation.GistSdk
import build.gist.presentation.GistViewListener
import build.gist_example.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), GistViewListener {

    private lateinit var binding: ActivityMainBinding

    private val gistSdk by lazy { GistSdk.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gistSdk.setCurrentRoute("home")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WebView.setWebContentsDebuggingEnabled(true)

        if (intent?.action == Intent.ACTION_VIEW) {
            Log.d(GIST_TAG, "View Intent: ${intent.dataString}")
        }

        gistSdk.addListener(object : GistListener {
            override fun onMessageShown(message: Message) {
                Log.d(GIST_TAG, "Message Shown")
            }

            override fun onMessageDismissed(message: Message) {
                Log.d(GIST_TAG, "Message Dismissed")
            }

            override fun onAction(message: Message, currentRoute: String, action: String, name: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action with name $name, from route $currentRoute on message ${message.instanceId}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(message: Message) {
                Log.d(GIST_TAG, "Message Error")
            }

            override fun embedMessage(message: Message, elementId: String) {
                runOnUiThread {
                    binding.gistView.setup(message)
                }
            }
        })

        val mainRouteProperties = mutableMapOf<String, Any?>()
        mainRouteProperties["title"] = "Top Artists"
        mainRouteProperties["list"] = ArtistsMock.data()

        binding.launchButton.setOnClickListener {
            val message = Message(messageId = "artists", properties = mainRouteProperties)
            gistSdk.showMessage(message, MessagePosition.CENTER)
        }

        binding.gistView.setup(message = Message("version-2-0"))
        binding.gistView.listener = this
    }

    override fun onGistViewSizeChanged(width: Int, height: Int) {
        val params = binding.gistView.layoutParams
        params.height = height
        runOnUiThread {
            binding.mainLayout.updateViewLayout(binding.gistView, params)
        }
    }
}
