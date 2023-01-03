package build.gist_example

import android.app.Application
import build.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val gistSdk = GistSdk.getInstance()

        // Initialize Gist SDK with site ID
        gistSdk.init(this, "c6ff92b9", "eu")

        // Set current user ID
        gistSdk.setUserToken("ABC123")
    }
}