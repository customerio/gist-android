package sh.bourbon.gist_example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sh.bourbon.gist.presentation.GistListener
import sh.bourbon.gist.presentation.GistSdk
import sh.bourbon.gist.data.model.Message
import sh.bourbon.gist_example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tag by lazy { this::class.java.simpleName }
    private val gistSdk by lazy { GistSdk.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent?.action == Intent.ACTION_VIEW) {
            Log.d(tag, "View Intent: ${intent.dataString}")
        }

        gistSdk.addListener(object : GistListener {
            override fun onMessageShown(message: Message) {
                Log.d(tag, "Message Shown")
            }

            override fun onMessageDismissed(message: Message) {
                Log.d(tag, "Message Dismissed")
            }

            override fun onAction(currentRoute: String, action: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action from route $currentRoute",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(message: Message) {
                Log.d(tag, "Message Error")
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
