package sh.bourbon.gist.presentation

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
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
import sh.bourbon.gist.BuildConfig
import sh.bourbon.gist.data.model.Configuration
import sh.bourbon.gist.data.model.MessageView
import sh.bourbon.gist.data.repository.GistService
import java.util.*


object GistSdk {

    private const val ORGANIZATION_ID_HEADER = "X-Bourbon-Organization-Id"
    private const val SHARED_PREFERENCES_NAME = "gist-sdk"
    private const val SHARED_PREFERENCES_USER_TOKEN_KEY = "userToken"
    private const val POLL_INTERVAL = 10_000L

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
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    private lateinit var organizationId: String
    private lateinit var context: Context

    private val listeners: MutableList<GistListener> = mutableListOf()

    private var observeUserMessagesJob: Job? = null
    private var timer: Timer? = null
    private var configuration: Configuration? = null
    private var isInitialized = false

    fun init(context: Context, organizationId: String) {
        this.context = context
        this.organizationId = organizationId
        this.isInitialized = true

        GlobalScope.launch {
            try {
                // Pre-fetch configuration
                gistService.fetchConfiguration()

                // Observe user messages if user token is set
                getUserToken()?.let { userToken -> observeMessagesForUser(userToken) }
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch configuration: ${e.message}", e)
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

    fun addListener(listener: GistListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GistListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    internal fun handleRouteLoaded(route: String) {
        listeners.forEach { it.onLoaded(route) }
    }

    internal fun handleRouteClosed(route: String) {
        listeners.forEach { it.onClosed(route) }
    }

    internal fun handleRouteError(route: String) {
        listeners.forEach { it.onError(route) }
    }

    internal fun handleAction(action: String) {
        listeners.forEach { it.onAction(action) }
    }

    internal fun logView(messageId: String) {
        ensureInitialized()

        GlobalScope.launch {
            try {
                val userToken = getUserToken() ?: throw Exception("User token not set")
                gistService.logView(MessageView(messageId, userToken))
            } catch (e: Exception) {
                Log.e(tag, "Failed to log message view: ${e.message}", e)
            }
        }
    }

    private fun showMessage(configuration: Configuration, messageId: String) {
        with(configuration) {
            val intent = GistActivity.newIntent(
                context,
                organizationId,
                projectId,
                engineEndpoint,
                identityEndpoint,
                messageId
            )

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            context.startActivity(intent)
        }
    }

    private fun canShowMessage() = !GistActivity.isShown

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
                    if (canShowMessage()) {
                        val latestMessages = gistService.fetchMessagesForUser(userToken)
                        showMessage(configuration, latestMessages.first().messageId)
                    }
                }
            } catch (e: CancellationException) {
                // Co-routine was cancelled, cancel internal timer
                timer?.cancel()
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch latest messages: ${e.message}", e)
            }
        }
    }

    private fun getUserToken(): String? {
        return sharedPreferences.getString(SHARED_PREFERENCES_USER_TOKEN_KEY, null)
    }

    private fun ensureInitialized() {
        if (!isInitialized) throw IllegalStateException("GistSdk must be initialized by calling GistSdk.init()")
    }
}

interface GistListener {

    fun onLoaded(route: String)

    fun onError(route: String)

    fun onClosed(route: String)

    fun onAction(action: String)
}