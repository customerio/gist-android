package build.gist_example

import android.app.Application
import build.gist.GistEnvironment
import build.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val gistSdk = GistSdk.getInstance()

        // Initialize Gist SDK with site ID
        gistSdk.init(this, "38180e5d34fcae872aa7", "us", GistEnvironment.DEV)

        // Set current user ID
        gistSdk.setUserToken("ABC123")
    }
}