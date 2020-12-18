package sh.bourbon.gist.data.repository

import retrofit2.http.GET
import sh.bourbon.gist.data.model.Configuration

interface GistService {
    @GET("/api/v1/configuration")
    suspend fun fetchConfiguration(): Configuration
}