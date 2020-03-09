package sh.bourbon.gist_example

import android.app.Application
import sh.bourbon.gist.presentation.GistSdk

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Gist SDK with organization ID
        GistSdk.init(this, BuildConfig.ORGANIZATION_ID)

        // Set current user ID
        GistSdk.setUserId(BuildConfig.USER_ID)
    }
}