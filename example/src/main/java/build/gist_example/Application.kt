package build.gist_example

import android.app.Application
import build.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val gistSdk = GistSdk.getInstance()

        // Initialize Gist SDK with organization ID
        gistSdk.init(this, "c6ff92b9-5607-4655-9265-f2588f7e3b58")

        // Set current user ID
        gistSdk.setUserToken("ABC123")
    }
}