package build.gist_example

import android.app.Application
import build.gist.GistEnvironment
import build.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val gistSdk = GistSdk.getInstance()

        // Initialize Gist SDK with site ID
        gistSdk.init(this, "a5ec106751ef4b34a0b9", "eu", GistEnvironment.PROD)

        // Set current user ID
        gistSdk.setUserToken("ABC123")
    }
}