package build.gist_example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import build.gist.presentation.GistListener
import build.gist.presentation.GistSdk
import build.gist.data.model.Message
import build.gist.presentation.GIST_TAG
import build.gist_example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val gistSdk by lazy { GistSdk.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            override fun onAction(currentRoute: String, action: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action from route $currentRoute",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(message: Message) {
                Log.d(GIST_TAG, "Message Error")
            }
        })

        val mainRouteProperties = mutableMapOf<String, Any?>()
        mainRouteProperties["title"] = "Top Artists"
        mainRouteProperties["list"] = ArtistsMock.data()

        binding.launchButton.setOnClickListener {
            val message = Message(messageId = "artists", properties = mainRouteProperties)
            gistSdk.showMessage(message)
        }
    }
}
