package build.gist.presentation

import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import build.gist.GistEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import build.gist.data.listeners.Queue
import build.gist.data.model.Message
import build.gist.data.model.MessagePosition

const val GIST_TAG: String = "Gist"

object GistSdk : Application.ActivityLifecycleCallbacks {
    private const val SHARED_PREFERENCES_NAME = "gist-sdk"
    private const val SHARED_PREFERENCES_USER_TOKEN_KEY = "userToken"
    private const val POLL_INTERVAL = 10_000L

    private val sharedPreferences by lazy {
        application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    lateinit var siteId: String
    lateinit var dataCenter: String
    internal lateinit var gistEnvironment: GistEnvironment
    internal lateinit var application: Application

    private val listeners: MutableList<GistListener> = mutableListOf()

    private var resumedActivities = mutableSetOf<String>()

    private var observeUserMessagesJob: Job? = null
    private var isInitialized = false
    private var gistQueue: Queue = Queue()

    private var gistModalManager: GistModalManager = GistModalManager()
    internal var currentRoute: String = ""

    @JvmStatic
    fun getInstance() = this

    override fun onActivityResumed(activity: Activity) {
        resumedActivities.add(activity.javaClass.name)

        // Start polling if app is resumed and user messages are not being observed
        val isNotObservingMessages =
            observeUserMessagesJob == null || observeUserMessagesJob?.isCancelled == true

        if (isAppResumed() && getUserToken() != null && isNotObservingMessages) {
            observeMessagesForUser()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        resumedActivities.remove(activity.javaClass.name)

        // Stop polling if app is in background
        if (!isAppResumed()) {
            observeUserMessagesJob?.cancel()
            observeUserMessagesJob = null
        }
    }

    fun init(application: Application, siteId: String, dataCenter: String, environment: GistEnvironment = GistEnvironment.PROD) {
        this.application = application
        this.siteId = siteId
        this.dataCenter = dataCenter
        this.isInitialized = true
        this.gistEnvironment = environment

        application.registerActivityLifecycleCallbacks(this)

        GlobalScope.launch {
            try {
                // Observe user messages if user token is set
                if (getUserToken() != null) {
                    observeMessagesForUser()
                }
            } catch (e: Exception) {
                Log.e(GIST_TAG, e.message, e)
            }
        }
    }

    fun setCurrentRoute(route: String) {
        currentRoute = route
        gistQueue.checkLocalQueue()
        Log.i(GIST_TAG, "Current gist route set to: $currentRoute")
    }

    // User Token

    fun clearUserToken() {
        ensureInitialized()
        // Remove user token from preferences & cancel job / timer.
        sharedPreferences.edit().remove(SHARED_PREFERENCES_USER_TOKEN_KEY).apply()
        observeUserMessagesJob?.cancel()
    }

    fun setUserToken(userToken: String) {
        ensureInitialized()

        if (!getUserToken().equals(userToken)) {
            Log.i(GIST_TAG, "Setting user token to: $userToken")
            // Save user token in preferences to be fetched on the next launch
            sharedPreferences.edit().putString(SHARED_PREFERENCES_USER_TOKEN_KEY, userToken).apply()

            // Try to observe messages for the freshly set user token
            try {
                observeMessagesForUser()
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to observe messages for user: ${e.message}", e)
            }
        }
    }

    // Messages

    fun showMessage(message: Message, position: MessagePosition? = null): String? {
        ensureInitialized()
        var messageShown = false

        GlobalScope.launch {
            try {
                messageShown = gistModalManager.showModalMessage(message, position)
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to show message: ${e.message}", e)
            }
        }
        return if (messageShown) message.instanceId else null
    }

    fun dismissMessage() {
        gistModalManager.dismissActiveMessage()
    }

    // Listeners

    fun addListener(listener: GistListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GistListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.forEach {
            val listenerPackageName = it.javaClass.`package`.name
            if (!listenerPackageName.toString().startsWith("build.gist.")) {
                Log.d(GIST_TAG, "Removing listener $listenerPackageName")
                listeners.remove(it)
            }
        }
    }

    // Gist Message Observer

    private fun observeMessagesForUser() {
        // Clean up any previous observers
        observeUserMessagesJob?.cancel()

        Log.i(GIST_TAG, "Messages timer started")
        gistQueue.fetchUserMessages()
        observeUserMessagesJob = GlobalScope.launch {
            try {
                // Poll for user messages
                val ticker = ticker(POLL_INTERVAL, context = this.coroutineContext)
                for (tick in ticker) {
                    gistQueue.fetchUserMessages()
                }
            } catch (e: CancellationException) {
                // Co-routine was cancelled, cancel internal timer
                Log.i(GIST_TAG, "Messages timer cancelled")
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to get user messages: ${e.message}", e)
            }
        }
    }

    internal fun handleGistLoaded(message: Message) {
        listeners.forEach { it.onMessageShown(message) }
    }

    internal fun handleGistClosed(message: Message) {
        listeners.forEach { it.onMessageDismissed(message) }
    }

    internal fun handleGistError(message: Message) {
        listeners.forEach { it.onError(message) }
    }

    internal fun handleEmbedMessage(message: Message, elementId: String) {
        listeners.forEach { it.embedMessage(message, elementId) }
    }

    internal fun handleGistAction(message: Message, currentRoute: String, action: String, name: String) {
        listeners.forEach { it.onAction(message, currentRoute, action, name) }
    }

    internal fun getUserToken(): String? {
        return sharedPreferences.getString(SHARED_PREFERENCES_USER_TOKEN_KEY, null)
    }

    private fun ensureInitialized() {
        if (!isInitialized) throw IllegalStateException("GistSdk must be initialized by calling GistSdk.init()")
    }

    private fun isAppResumed() = resumedActivities.isNotEmpty()

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}
}

interface GistListener {
    fun embedMessage(message: Message, elementId: String)
    fun onMessageShown(message: Message)
    fun onMessageDismissed(message: Message)
    fun onError(message: Message)
    fun onAction(message: Message, currentRoute: String, action: String, name: String)
}