package sh.bourbon.gist.data.repository

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import sh.bourbon.gist.data.model.Message
import sh.bourbon.gist.data.model.MessageView

interface GistQueueService {
    @POST("/api/v1/users")
    suspend fun fetchMessagesForUser(@Body topics: List<String>): Response<List<Message>>

    @POST("/api/v1/logs/message/{messageId}")
    suspend fun logMessageView(@Path("messageId") messageId: String)

    @POST("/api/v1/logs/queue/{queueId}")
    suspend fun logUserMessageView(@Path("queueId") queueId: String)
}