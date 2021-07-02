package build.gist.data.events

import build.gist.BuildConfig
import build.gist.data.NetworkUtilities
import build.gist.data.model.LogEvent
import build.gist.data.model.Message
import build.gist.data.repository.GistAnalyticsService
import build.gist.presentation.GistSdk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Analytics {
    companion object {
        private const val ANALYTICS_EVENT_LOADED = "gist_loaded"
        private const val ANALYTICS_EVENT_ACTION = "gist_action"
        private const val ANALYTICS_EVENT_SYSTEM_ACTION = "gist_system_action"
        private const val ANALYTICS_EVENT_DISMISSED = "gist_dismissed"
    }

    private val gistAnalyticsService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader(NetworkUtilities.ORGANIZATION_ID_HEADER, GistSdk.organizationId)
                    .build()

                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.GIST_ANALYTICS_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(GistAnalyticsService::class.java)
    }

    fun messageLoaded(message: Message, route: String) {
        GlobalScope.launch {
            gistAnalyticsService.logOrganizationEvent(
                LogEvent(
                    ANALYTICS_EVENT_LOADED,
                    route,
                    message.instanceId,
                    message.queueId
                )
            )
        }
    }

    fun messageDismissed(message: Message, route: String) {
        GlobalScope.launch {
            gistAnalyticsService.logOrganizationEvent(
                LogEvent(
                    ANALYTICS_EVENT_DISMISSED,
                    route,
                    message.instanceId,
                    message.queueId
                )
            )
        }
    }

    fun actionPerformed(message: Message, route: String, system: Boolean) {
        GlobalScope.launch {
            if (system) {
                gistAnalyticsService.logOrganizationEvent(
                    LogEvent(
                        ANALYTICS_EVENT_SYSTEM_ACTION,
                        route,
                        message.instanceId,
                        message.queueId
                    )
                )
            } else {
                gistAnalyticsService.logOrganizationEvent(
                    LogEvent(
                        ANALYTICS_EVENT_ACTION,
                        route,
                        message.instanceId,
                        message.queueId
                    )
                )
            }
        }
    }

}