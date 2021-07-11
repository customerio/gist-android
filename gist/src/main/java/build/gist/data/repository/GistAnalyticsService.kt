package build.gist.data.repository

import retrofit2.http.Body
import retrofit2.http.POST
import build.gist.data.model.LogEvent

interface GistAnalyticsService {
    @POST("/api/v1/organization/events")
    suspend fun logOrganizationEvent(@Body logEvent: LogEvent)
}