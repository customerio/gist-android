package sh.bourbon.gist.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sh.bourbon.gist.BuildConfig
import sh.bourbon.gist.data.model.Configuration
import sh.bourbon.gist.data.repository.GistService


object GistSdk {

    private const val ORGANIZATION_ID_HEADER = "X-Bourbon-Organization-Id"

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

    private lateinit var organizationId: String
    private lateinit var context: Context

    private var configuration: Configuration? = null

    private var isInitialized = false

    fun init(context: Context, organizationId: String) {
        this.context = context
        this.organizationId = organizationId
        this.isInitialized = true

        GlobalScope.launch {
            try {
                gistService.fetchConfiguration()
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch configuration: ${e.message}", e)
            }
        }
    }

    fun setUserId(userId: String) {
        ensureInitialized()

        GlobalScope.launch {
            try {
                val configuration = getConfiguration()

                // TODO: This should be changed to poll every 10s
                val latestMessages = gistService.fetchMessagesForUser(userId)
                showMessage(configuration, latestMessages.first().messageId)
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch latest messages: ${e.message}", e)
            }
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

    private fun ensureInitialized() {
        if (!isInitialized) throw IllegalStateException("GistSdk must be initialized by calling GistSdk.init()")
    }
}
