package build.gist.data.listeners

import android.util.Log
import build.gist.BuildConfig
import build.gist.data.NetworkUtilities
import build.gist.data.model.GistMessageProperties
import build.gist.data.model.Message
import build.gist.data.model.UserMessages
import build.gist.data.repository.GistQueueService
import build.gist.presentation.GIST_TAG
import build.gist.presentation.GistListener
import build.gist.presentation.GistSdk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.regex.PatternSyntaxException

class Queue: GistListener {

    init {
        GistSdk.addListener(this)
    }

    private val gistQueueService by lazy {
        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                GistSdk.getUserToken()?.let { userToken ->
                    val request: Request = chain.request().newBuilder()
                        .addHeader(NetworkUtilities.ORGANIZATION_ID_HEADER, GistSdk.organizationId)
                        .addHeader(NetworkUtilities.USER_TOKEN_HEADER, userToken)
                        .build()

                    chain.proceed(request)
                } ?: run {
                    val request: Request = chain.request().newBuilder()
                        .addHeader(NetworkUtilities.ORGANIZATION_ID_HEADER, GistSdk.organizationId)
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

    internal fun fetchUserMessages() {
        GlobalScope.launch {
            try {
                Log.i(GIST_TAG, "Fetching user messages")
                val latestMessagesResponse = gistQueueService.fetchMessagesForUser(
                    UserMessages(
                        GistSdk.getTopics()
                    )
                )
                if (latestMessagesResponse.code() == 204) {
                    // No content, don't do anything
                    Log.i(GIST_TAG, "No messages found for user")
                } else if (latestMessagesResponse.isSuccessful) {
                    Log.i(GIST_TAG, "Found ${latestMessagesResponse.body()?.count()} messages for user")
                    run loop@{
                        latestMessagesResponse.body()?.forEach foreach@{ message ->
                            val gistProperties = GistMessageProperties.getGistProperties(message)
                            gistProperties.routeRule?.let { routeRule ->
                                try {
                                    if (!routeRule.toRegex().matches(GistSdk.currentRoute)) {
                                        Log.i(
                                            GIST_TAG,
                                            "Message route: $routeRule does not match current route: ${GistSdk.currentRoute}"
                                        )
                                        return@foreach
                                    }
                                } catch (e: PatternSyntaxException) {
                                    Log.i(GIST_TAG, "Invalid route rule regex: $routeRule")
                                    return@foreach
                                }
                            }
                            gistProperties.elementId?.let { elementId ->
                                Log.i(
                                    GIST_TAG,
                                    "Embedding message from queue with queue id: ${message.queueId}"
                                )
                                GistSdk.handleEmbedMessage(message, elementId)
                            } ?: run {
                                Log.i(
                                    GIST_TAG,
                                    "Showing message from queue with queue id: ${message.queueId}"
                                )
                                GistSdk.showMessage(message)
                                return@loop
                            }
                        }
                    }
                }
            }
            catch (e: Exception) {
                Log.e(
                    GIST_TAG,
                    "Error fetching messages: ${e.message}"
                )
            }
        }
    }

    private fun logView(message: Message) {
        GlobalScope.launch {
            try {
                if (message.queueId != null) {
                    Log.i(GIST_TAG, "Logging view for user message: ${message.messageId}, with queue id: ${message.queueId}")
                    gistQueueService.logUserMessageView(message.queueId)
                } else {
                    Log.i(GIST_TAG, "Logging view for message: ${message.messageId}")
                    gistQueueService.logMessageView(message.messageId)
                }
            } catch (e: Exception) {
                Log.e(GIST_TAG, "Failed to log message view: ${e.message}", e)
            }
        }
    }

    override fun onMessageShown(message: Message) {
        logView(message)
    }

    override fun embedMessage(message: Message, elementId: String) {}

    override fun onMessageDismissed(message: Message) {}

    override fun onError(message: Message) {}

    override fun onAction(message: Message, currentRoute: String, action: String, name: String) {}
}