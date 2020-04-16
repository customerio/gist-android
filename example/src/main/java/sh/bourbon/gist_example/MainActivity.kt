package sh.bourbon.gist_example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sh.bourbon.gist.presentation.GistListener
import sh.bourbon.gist.presentation.GistSdk
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GistSdk.addListener(object : GistListener {
            override fun onMessageShown(messageId: String) {
                Log.d("Main Activity", "Message Shown")
            }

            override fun onMessageDismissed(messageId: String) {
                Log.d("Main Activity", "Message Dismissed")
            }

            override fun onAction(action: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(messageId: String) {
                Log.d("Main Activity", "Message Error")
            }
        })

        launchButton.setOnClickListener {
            GistSdk.showMessage("expired")
        }
    }
}
