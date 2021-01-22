package sh.bourbon.gist_example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sh.bourbon.gist.presentation.GistListener
import sh.bourbon.gist.presentation.GistSdk
import kotlinx.android.synthetic.main.activity_main.*
import sh.bourbon.gist.data.model.Message

class MainActivity : AppCompatActivity() {

    private val tag by lazy { this::class.java.simpleName }
    private val gistSdk by lazy { GistSdk.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_VIEW) {
            Log.d(tag, "View Intent: ${intent.dataString}")
        }

        setContentView(R.layout.activity_main)

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

        launchButton.setOnClickListener {
            val message = Message(messageId = "artists", properties = mainRouteProperties)
            gistSdk.showMessage(message)
        }
    }
}
