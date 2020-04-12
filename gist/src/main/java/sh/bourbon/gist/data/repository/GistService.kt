package sh.bourbon.gist.data.repository

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import sh.bourbon.gist.data.model.Configuration
import sh.bourbon.gist.data.model.Message
import sh.bourbon.gist.data.model.MessageView


interface GistService {

    @GET("/api/v1/configuration")
    suspend fun fetchConfiguration(): Configuration

    @GET("/api/v1/queue/user/{userToken}")
    suspend fun fetchMessagesForUser(@Path("userToken") userToken: String): Response<List<Message>>

    @POST("/api/v1/log")
    suspend fun logView(@Body messageView: MessageView)
}