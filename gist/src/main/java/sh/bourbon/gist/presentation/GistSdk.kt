package sh.bourbon.gist.presentation

import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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
import sh.bourbon.engine.BourbonEngine
import sh.bourbon.engine.BourbonEngineListener
import sh.bourbon.engine.EngineConfiguration
import sh.bourbon.gist.BuildConfig
import sh.bourbon.gist.data.model.Configuration
import sh.bourbon.gist.data.model.MessageView
import sh.bourbon.gist.data.repository.GistService
import java.util.*


object GistSdk : Application.ActivityLifecycleCallbacks {

    internal const val BOURBON_ENGINE_ID = "gistSdk"

    private const val ORGANIZATION_ID_HEADER = "X-Bourbon-Organization-Id"
    private const val SHARED_PREFERENCES_NAME = "gist-sdk"
    private const val SHARED_PREFERENCES_USER_TOKEN_KEY = "userToken"
    private const val POLL_INTERVAL = 10_000L

    private const val ACTION_CLOSE = "gist://close"

    private val tag by lazy { this::class.java.simpleName }

    private val gistService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader(ORGANIZATION_ID_HEADER, organizationId)
                    .build()

                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(GistService::class.java)
    }

    private val sharedPreferences by lazy {
        application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    private lateinit var organizationId: String
    private lateinit var application: Application

    private val listeners: MutableList<GistListener> = mutableListOf()

    private var resumedActivities = mutableSetOf<String>()

    private var observeUserMessagesJob: Job? = null
    private var timer: Timer? = null
    private var configuration: Configuration? = null
    private var isInitialized = false
    private var bourbonEngine: BourbonEngine? = null
    private var currentMessage: String? = null
    private var pendingMessageId: String? = null

    @JvmStatic
    fun getInstance() = this

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivities.add(activity.javaClass.name)

        // Start polling if app is resumed and user messages are not being observed
        val isNotObservingMessages =
            observeUserMessagesJob == null || observeUserMessagesJob?.isCancelled == true

        if (isAppResumed() && isNotObservingMessages) {
            getUserToken()?.let { userToken -> observeMessagesForUser(userToken) }
        }

        // Show any pending messages
        pendingMessageId?.let { messageId ->
            pendingMessageId = null
            handleEngineRouteLoaded(messageId)
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

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is GistActivity) {
            currentMessage?.let { currentMessage -> handleEngineRouteClosed(currentMessage) }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
    }

    fun init(application: Application, organizationId: String) {
        this.application = application
        this.organizationId = organizationId
        this.isInitialized = true

        application.registerActivityLifecycleCallbacks(this)

        GlobalScope.launch {
            try {
                // Pre-fetch configuration
                getConfiguration()

                // Observe user messages if user token is set
                getUserToken()?.let { userToken -> observeMessagesForUser(userToken) }
            } catch (e: Exception) {
                Log.e(tag, e.message, e)
            }
        }
    }

    fun setUserToken(userToken: String) {
        ensureInitialized()

        // Save user token in preferences to be fetched on the next launch
        sharedPreferences.edit().putString(SHARED_PREFERENCES_USER_TOKEN_KEY, userToken).apply()

        // Try to observe messages for the freshly set user token
        try {
            observeMessagesForUser(userToken)
        } catch (e: Exception) {
            Log.e(tag, "Failed to observe messages for user: ${e.message}", e)
        }
    }

    fun showMessage(messageId: String) {
        ensureInitialized()

        GlobalScope.launch {
            try {
                val configuration = getConfiguration()
                showMessage(configuration, messageId)
            } catch (e: Exception) {
                Log.e(tag, "Failed to show message: ${e.message}", e)
            }
        }
    }

    fun dismissMessage() {
        currentMessage?.let { currentMessage -> handleEngineRouteClosed(currentMessage) }
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

    private fun logView(messageId: String) {
        ensureInitialized()

        GlobalScope.launch {
            try {
                gistService.logView(MessageView(messageId, getUserToken()))
            } catch (e: Exception) {
                Log.e(tag, "Failed to log message view: ${e.message}", e)
            }
        }
    }

    private fun showMessage(configuration: Configuration, messageRoute: String) {
        with(configuration) {
            if (currentMessage == null) {
                currentMessage = messageRoute
                val uiHandler = Handler(application.mainLooper)
                val runnable = Runnable {
                    bourbonEngine = BourbonEngine(application, BOURBON_ENGINE_ID).apply {
                        setup(
                            EngineConfiguration(
                                organizationId = organizationId,
                                projectId = projectId,
                                engineEndpoint = engineEndpoint,
                                authenticationEndpoint = identityEndpoint,
                                engineVersion = 1.0,
                                configurationVersion = 1.0,
                                mainRoute = messageRoute
                            )
                        )

                        setListener(object : BourbonEngineListener {
                            var isInitialLoad = true
                            override fun onBootstrapped() {
                            }

                            override fun onRouteChanged(newRoute: String) {
                            }

                            override fun onRouteError(route: String) {
                                handleEngineRouteError(route)
                            }

                            override fun onError() {
                                handleEngineRouteError(messageRoute)
                            }

                            override fun onRouteLoaded(route: String) {
                                if (isInitialLoad) {
                                    isInitialLoad = false

                                    val isAppStillRunning = resumedActivities.isNotEmpty()
                                    if (isAppStillRunning) {
                                        handleEngineRouteLoaded(messageRoute)
                                    } else {
                                        // App was paused between the request and the time the engine was loaded.
                                        // Since the activity cannot be shown in this state, set the message id as
                                        // pending and show it when the app is resumed.
                                        pendingMessageId = route
                                    }
                                }
                            }

                            override fun onTap(action: String) {
                                when (action) {
                                    ACTION_CLOSE -> handleEngineRouteClosed(messageRoute)
                                    else -> handleEngineAction(action)
                                }
                            }
                        })
                    }
                }

                uiHandler.post(runnable)
            }
        }
    }

    private fun showMessageActivity() {
        val intent = GistActivity.newIntent(application)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        application.startActivity(intent)
    }

    private suspend fun getConfiguration(): Configuration {
        val existingConfiguration = configuration
        if (existingConfiguration != null) return existingConfiguration

        // Configuration does not exist, fetch it from service and persist globally
        try {
            val configuration = gistService.fetchConfiguration()
            this.configuration = configuration
            return configuration
        } catch (e: Exception) {
            throw Exception("Failed to fetch configuration: ${e.message}", e)
        }
    }

    private fun observeMessagesForUser(userToken: String) {
        // Clean up any previous observers
        observeUserMessagesJob?.cancel()
        timer = null

        observeUserMessagesJob = GlobalScope.launch {
            try {
                val configuration = getConfiguration()

                // Poll for user messages
                val ticker = ticker(POLL_INTERVAL, context = this.coroutineContext)
                for (tick in ticker) {
                    val latestMessagesResponse = gistService.fetchMessagesForUser(userToken)
                    if (latestMessagesResponse.code() == 204) {
                        // No content, don't do anything
                        continue
                    } else if (latestMessagesResponse.isSuccessful) {
                        latestMessagesResponse.body()?.last()?.let {
                            if (canShowMessage()) showMessage(configuration, it.route)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Co-routine was cancelled, cancel internal timer
                timer?.cancel()
            } catch (e: Exception) {
                Log.e(tag, "Failed to get user messages: ${e.message}", e)
            }
        }
    }

    private fun handleEngineRouteLoaded(route: String) {
        showMessageActivity()
        logView(route)
        listeners.forEach { it.onMessageShown(route) }
    }

    private fun handleEngineRouteClosed(route: String) {
        currentMessage = null
        bourbonEngine = null
        listeners.forEach { it.onMessageDismissed(route) }
    }

    private fun handleEngineRouteError(route: String) {
        listeners.forEach { it.onError(route) }
        currentMessage = null
        bourbonEngine = null
    }

    private fun handleEngineAction(action: String) {
        listeners.forEach { it.onAction(action) }
    }

    private fun getUserToken(): String? {
        return sharedPreferences.getString(SHARED_PREFERENCES_USER_TOKEN_KEY, null)
    }

    private fun ensureInitialized() {
        if (!isInitialized) throw IllegalStateException("GistSdk must be initialized by calling GistSdk.init()")
    }

    private fun canShowMessage(): Boolean {
        return isAppResumed() && !isGistActivityResumed()
    }

    private fun isAppResumed() = resumedActivities.isNotEmpty()

    private fun isGistActivityResumed() = resumedActivities.contains(GistActivity::class.java.name)
}

interface GistListener {
    fun onMessageShown(messageRoute: String)

    fun onMessageDismissed(messageRoute: String)

    fun onAction(action: String)

    fun onError(messageRoute: String)
}