package build.gist.presentation

import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import build.gist.BuildConfig
import build.gist.data.NetworkUtilities
import build.gist.data.events.Analytics
import build.gist.data.model.Message
import build.gist.data.model.UserMessages
import build.gist.data.repository.GistQueueService
import java.util.*

const val GIST_TAG: String = "Gist"

object GistSdk : Application.ActivityLifecycleCallbacks {
    private const val SHARED_PREFERENCES_NAME = "gist-sdk"
    private const val SHARED_PREFERENCES_USER_TOKEN_KEY = "userToken"
    private const val POLL_INTERVAL = 10_000L

    private const val ACTION_CLOSE = "gist://close"

    private val gistQueueService by lazy {
        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                getUserToken()?.let { userToken ->
                    val request: Request = chain.request().newBuilder()
                        .addHeader(NetworkUtilities.ORGANIZATION_ID_HEADER, organizationId)
                        .addHeader(NetworkUtilities.USER_TOKEN_HEADER, userToken)
                        .build()

                    chain.proceed(request)
                } ?: run {
                    val request: Request = chain.request().newBuilder()
                        .addHeader(NetworkUtilities.ORGANIZATION_ID_HEADER, organizationId)
                        .build()

                    chain.proceed(request)
                }
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.GIST_QUEUE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(GistQueueService::class.java)
    }

    private val sharedPreferences by lazy {
        application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    lateinit var organizationId: String
    internal lateinit var application: Application

    private val listeners: MutableList<GistListener> = mutableListOf()

    private var resumedActivities = mutableSetOf<String>()

    private var observeUserMessagesJob: Job? = null
    private var timer: Timer? = null
    private var isInitialized = false
    private var topics: List<String> = emptyList()

    private var gistModalManager: GistModalManager = GistModalManager()
    internal var gistAnalytics: Analytics = Analytics()

    @JvmStatic
    fun getInstance() = this

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
    }

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

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}

    fun init(application: Application, organizationId: String) {
        this.application = application
        this.organizationId = organizationId
        this.isInitialized = true

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

    fun subscribeToTopic(topic: String) {
        var topicIndex = topics.indexOf(topic)
        if (topicIndex == -1) {
            topics = topics.plus(topic)
        }
    }

    fun unsubscribeFromTopic(topic: String) {
        var topicIndex = topics.indexOf(topic)
        if (topicIndex > -1) {
            topics = topics.drop(topicIndex)
        }
    }

    fun getTopics(): List<String>{
        return topics
    }

    fun clearTopics() {
        topics = emptyList()
    }

    fun clearUserToken() {
        ensureInitialized()
        // Remove user token from preferences & cancel job / timer.
        sharedPreferences.edit().remove(SHARED_PREFERENCES_USER_TOKEN_KEY).apply()
        observeUserMessagesJob?.cancel()
        timer = null
    }

    fun setUserToken(userToken: String) {
        ensureInitialized()

        if (!getUserToken().equals(userToken)) {
            Log.d(GIST_TAG, "Setting user token to: $userToken")
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

    fun showMessage(message: Message): String {
        ensureInitialized()

        GlobalScope.launch {
            try {
                gistModalManager.showModalMessage(message)
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to show message: ${e.message}", e)
            }
        }
        return message.instanceId
    }

    fun dismissMessage(instanceId: String) {
        //handleGistClosed(message = )
        //currentMessage?.let { currentMessage -> handleEngineRouteClosed(currentMessage) }
    }

    fun addListener(listener: GistListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GistListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    private fun logView(message: Message) {
        ensureInitialized()

        GlobalScope.launch {
            try {
                if (message.queueId != null) {
                    Log.d(GIST_TAG, "Logging view for user message: ${message.messageId}, with queue id: ${message.queueId}")
                    gistQueueService.logUserMessageView(message.queueId)
                } else {
                    Log.d(GIST_TAG, "Logging view for message: ${message.messageId}")
                    gistQueueService.logMessageView(message.messageId)
                }
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to log message view: ${e.message}", e)
            }
        }
    }

    private fun observeMessagesForUser() {
        // Clean up any previous observers
        observeUserMessagesJob?.cancel()
        timer = null

        Log.d(GIST_TAG, "Messages timer started")
        observeUserMessagesJob = GlobalScope.launch {
            try {
                // Poll for user messages
                val ticker = ticker(POLL_INTERVAL, context = this.coroutineContext)
                for (tick in ticker) {
                    Log.d(GIST_TAG, "Fetching user messages")
                    val latestMessagesResponse = gistQueueService.fetchMessagesForUser(UserMessages(getTopics()))
                    if (latestMessagesResponse.code() == 204) {
                        // No content, don't do anything
                        Log.d(GIST_TAG, "No messages found for user")
                        continue
                    } else if (latestMessagesResponse.isSuccessful) {
                        Log.d(GIST_TAG, "Found ${latestMessagesResponse.body()?.count()} messages for user")
                        latestMessagesResponse.body()?.last()?.let { message ->
                            showMessage(message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Co-routine was cancelled, cancel internal timer
                Log.d(GIST_TAG, "Messages timer cancelled")
                timer?.cancel()
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

    internal fun handleGistAction(currentRoute: String, action: String) {
        listeners.forEach { it.onAction(currentRoute, action) }
    }

    private fun getUserToken(): String? {
        return sharedPreferences.getString(SHARED_PREFERENCES_USER_TOKEN_KEY, null)
    }

    private fun ensureInitialized() {
        if (!isInitialized) throw IllegalStateException("GistSdk must be initialized by calling GistSdk.init()")
    }

    private fun isAppResumed() = resumedActivities.isNotEmpty()

    /*
    private fun canShowMessage(): Boolean {
        return isAppResumed() && !isGistActivityResumed()
    }

    private fun isGistActivityResumed() = resumedActivities.contains(GistModalActivity::class.java.name)
     */
}

interface GistListener {
    fun onMessageShown(message: Message)
    fun onMessageDismissed(message: Message)
    fun onError(message: Message)
    fun onAction(currentRoute: String, action: String)
}