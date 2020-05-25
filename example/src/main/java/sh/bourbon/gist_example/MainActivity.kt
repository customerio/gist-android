package sh.bourbon.gist_example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sh.bourbon.gist.presentation.GistListener
import sh.bourbon.gist.presentation.GistSdk
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tag by lazy { this::class.java.simpleName }
    private val gistSdk by lazy { GistSdk.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gistSdk.addListener(object : GistListener {
            override fun onMessageShown(messageId: String) {
                Log.d(tag, "Message Shown")
            }

            override fun onMessageDismissed(messageId: String) {
                Log.d(tag, "Message Dismissed")
            }

            override fun onAction(currentRoute: String, action: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action from route $currentRoute",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(messageId: String) {
                Log.d(tag, "Message Error")
            }
        })

        launchButton.setOnClickListener { gistSdk.showMessage("demo") }
    }
}
