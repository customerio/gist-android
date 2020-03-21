package sh.bourbon.gist_example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import sh.bourbon.gist.presentation.GistListener
import sh.bourbon.gist.presentation.GistSdk

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GistSdk.addListener(object : GistListener {
            override fun onLoaded(route: String) {
            }

            override fun onError(route: String) {
            }

            override fun onAction(action: String) {
                Toast.makeText(
                    this@MainActivity,
                    "Action received: $action",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
