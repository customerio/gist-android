package sh.bourbon.gist.data.repository

import retrofit2.http.Body
import retrofit2.http.POST
import sh.bourbon.gist.data.model.LogEvent

interface GistAnalyticsService {
    @POST("/api/v1/organization/events")
    suspend fun logOrganizationEvent(@Body logEvent: LogEvent)
}