package build.gist_example

import android.app.Application
import build.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val gistSdk = GistSdk.getInstance()

        // Initialize Gist SDK with organization ID
        gistSdk.init(this, BuildConfig.ORGANIZATION_ID)

        // Set current user ID
        gistSdk.setUserToken(BuildConfig.USER_ID)

        gistSdk.subscribeToTopic("announcements")
    }
}